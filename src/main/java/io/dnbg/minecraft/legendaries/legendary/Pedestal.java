package io.dnbg.minecraft.legendaries.legendary;

import io.dnbg.minecraft.legendaries.Legendaries;
import io.dnbg.minecraft.legendaries.mixin.BlockDisplayAccessor;
import io.dnbg.minecraft.legendaries.mixin.DisplayTransformAccessor;
import io.dnbg.minecraft.legendaries.mixin.InteractionAccessor;
import io.dnbg.minecraft.legendaries.mixin.ItemDisplayAccessor;
import io.dnbg.minecraft.legendaries.mixin.TextDisplayAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Where the legendaries wait when nobody is carrying them.
 *
 * <p>Entities, no blocks: a {@link Display.BlockDisplay} for the plinth, an
 * {@link Display.ItemDisplay} per legendary standing on it, and one {@link Interaction} that turns a
 * right-click into a claim. Nothing is placed in the world, so there is nothing to mine, nothing to
 * grief, and nothing to collide with — a display entity has no collision at all.
 *
 * <p><strong>The pedestal is a fixture, not a container that appears when full.</strong> It stands
 * at world spawn from the first tick of a world and stays there whether or not anything is on it;
 * only the item displays come and go. Building it on arrival and tearing it down on collection made
 * it invisible in the state that matters most — a legendary is out there somewhere, and the empty
 * plinth is the thing that tells you where it will come back to.
 *
 * <p><strong>An item display holds the authoritative stack.</strong> The legendary that returns here
 * is the same stack that left, kept on its display rather than copied into saved data — so a rename
 * or an added enchantment survives a trip to the pedestal, and there is no second definition of the
 * item to drift from its recipe.
 */
public final class Pedestal {
	/** Scoreboard tag identifying our entities, so a player's own displays are never touched. */
	public static final String TAG = "legendaries_pedestal";
	/** Marks which legendary an item display belongs to, so a claim can put the right one back. */
	private static final String SLOT_TAG_PREFIX = "legendaries_slot_";

	/**
	 * Marks which shape an entity was built to, so a pedestal already standing is rebuilt when the
	 * shape changes rather than kept as it was. See {@link PlinthShape#FINGERPRINT}.
	 */
	private static final String SHAPE_TAG = "legendaries_shape_" + PlinthShape.FINGERPRINT;

	private static final double SEARCH_RADIUS = 3.0;

	/** Ticks per quarter-turn of the legendaries on their pedestal; four make a revolution. */
	private static final int SPIN_STEP_TICKS = 15;
	private static final float QUARTER_TURN = (float) (Math.PI / 2.0);

	private Pedestal() {
	}

	/**
	 * Looks an entity type up by id rather than naming a constant.
	 *
	 * <p>The constants moved: {@code EntityType.BLOCK_DISPLAY} in 26.1 and 26.1.2 became
	 * {@code EntityTypes.BLOCK_DISPLAY} in 26.2, so either spelling breaks half the supported range.
	 * {@code BuiltInRegistries.ENTITY_TYPE} and {@code Registry.getValue} are unchanged across all of
	 * it, and the ids are datapack-visible names that cannot move without breaking every world.
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Entity> EntityType<T> type(String id) {
		return (EntityType<T>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id));
	}

	/**
	 * The pedestal's position, siting it at world spawn the first time it is needed.
	 *
	 * <p>World spawn is only the initial siting. Once stored, the position is the pedestal's own —
	 * moving world spawn afterwards does not move it, and neither does anything but the admin command.
	 */
	public static BlockPos position(MinecraftServer server, LegendaryState state) {
		BlockPos stored = state.pedestalPos();
		if (stored != null) {
			return stored;
		}
		ServerLevel level = LegendaryState.home(server);
		BlockPos spawn = level.getRespawnData().globalPos().pos();
		// Generate the column before asking how tall it is. A heightmap read against a chunk that
		// does not exist yet answers with the bottom of the world, which would bury the pedestal in
		// the bedrock — and a legendary is only ever lost at moments nobody chose, so the spawn chunk
		// being cold is the normal case rather than the odd one.
		level.getChunkAt(spawn);
		BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
		state.setPedestalPos(surface);
		return surface;
	}

	/**
	 * Builds the plinth and click target if they are not standing, and does nothing if they are.
	 *
	 * <p>Three callers, and only one of them waits. The session tick holds off until the site's
	 * <em>entity index</em> reports loaded — not merely its chunk, which arrives first and would make
	 * a standing pedestal read as absent. {@code place()} and {@code move()} call this whenever they
	 * need it, so both can still raise a duplicate inside that window.
	 *
	 * <p>That is tolerable rather than ignored: a duplicate is a wrong-sized set, and the next
	 * {@code ensure()} rebuilds it — carrying every held legendary across, which is what stops a
	 * duplicate from costing one. Do not make the rebuild cheaper by skipping that carry.
	 */
	public static void ensure(MinecraftServer server) {
		LegendaryState state = LegendaryState.get(server);
		BlockPos pos = position(server, state);
		ServerLevel level = LegendaryState.home(server);
		if (structureIntact(level, pos, state)) {
			return;
		}
		// Something was removed out from under us, or a duplicate was raised. Rebuild the whole
		// thing rather than guess which piece is wrong — but carry the legendaries across first.
		// The displays hold the authoritative stacks, so discarding them without reading them
		// destroys the items outright: the state would still say they are home, take() would refuse
		// to correct it, place() would refuse to re-home them, and `crafted` would still block a
		// replacement.
		List<ItemStack> held = heldStacks(level, pos);
		clearEntities(level, pos);
		buildFixtures(level, pos);
		for (ItemStack stack : held) {
			Legendary.of(stack).ifPresent(legendary -> showSlot(level, pos, legendary, stack));
		}
		if (!held.isEmpty()) {
			Legendaries.LOGGER.warn("Rebuilt the pedestal at {} and put {} legendaries back on it", pos, held.size());
		}
		Legendaries.LOGGER.info("Pedestal standing at {}", pos);
	}

	/**
	 * Whether the plinth, the click target and exactly the expected item displays are all present,
	 * and all built to the shape this version draws.
	 *
	 * <p><strong>The shape tag is what makes a change reach a world that already has a pedestal.</strong>
	 * Counting alone only notices a tier being added or removed; a different block, a different
	 * scale or a resized click target leaves the count untouched, so the old pedestal stood
	 * unchanged and the change reached only worlds that had never seen one.
	 */
	private static boolean structureIntact(ServerLevel level, BlockPos pos, LegendaryState state) {
		int plinths = 0;
		int targets = 0;
		int displays = 0;
		for (Entity entity : ours(level, pos)) {
			if (!entity.entityTags().contains(SHAPE_TAG)) {
				return false;
			}
			if (entity instanceof Display.BlockDisplay) {
				plinths++;
			} else if (entity instanceof Interaction) {
				targets++;
			} else if (entity instanceof Display.ItemDisplay) {
				displays++;
			}
		}
		return plinths == PlinthShape.LIVE.length && targets == 1 && displays == state.onPedestal().size();
	}

	/** Puts a legendary on the pedestal, building it first if it somehow is not there. */
	public static void place(MinecraftServer server, ItemStack stack) {
		Legendary legendary = Legendary.of(stack).orElse(null);
		if (legendary == null) {
			return;
		}
		LegendaryState state = LegendaryState.get(server);
		BlockPos pos = position(server, state);
		ServerLevel level = LegendaryState.home(server);
		if (state.onPedestal(legendary)) {
			// A second copy of something that is supposed to be unique — which only the operator's
			// `item give` can produce. There is one slot per legendary, so this one cannot be
			// displayed; drop it at the pedestal rather than deleting it. Refusing quietly here
			// would make the command a way to destroy the copies it just handed out.
			Legendaries.LOGGER.warn("{} is already on the pedestal; dropping the returning copy beside it",
					legendary.displayName());
			level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
					stack.copy()));
			return;
		}
		// Order matters twice here. ensure() runs first, while the state still describes the world as
		// it is — marking the legendary home beforehand makes structureIntact count a display that
		// has not spawned yet, so it never matches and every single return rebuilds the pedestal.
		// But the mark must still land before the display spawns: spawning into a loaded chunk fires
		// the entity-load event synchronously.
		ensure(server);
		state.setOnPedestal(legendary, true);
		showSlot(level, pos, legendary, stack.copy());
		Legendaries.LOGGER.info("{} returned to its pedestal at {}", legendary.displayName(), pos);
	}

	/**
	 * Takes one legendary off the pedestal, chosen at random from those standing on it.
	 *
	 * <p>The stack comes off its item display rather than being rebuilt, so whatever it carried when
	 * it arrived is what leaves. The pedestal itself stays standing — only what it holds changes.
	 */
	public static ItemStack takeOne(MinecraftServer server) {
		LegendaryState state = LegendaryState.get(server);
		BlockPos pos = position(server, state);
		ServerLevel level = LegendaryState.home(server);
		List<ItemStack> held = heldStacks(level, pos);
		if (held.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack chosen = held.get(level.getRandom().nextInt(held.size()));
		Legendary legendary = Legendary.of(chosen).orElse(null);
		if (legendary == null) {
			return ItemStack.EMPTY;
		}
		clearSlot(level, pos, legendary);
		state.setOnPedestal(legendary, false);
		return chosen;
	}

	/**
	 * Turns the legendaries on their pedestal, a quarter of a turn at a time.
	 *
	 * <p>The server sets a target and the client walks to it, so this costs one packet per display
	 * per {@link #SPIN_STEP_TICKS} rather than one per tick. Two things make that work, and both are
	 * easy to get wrong:
	 *
	 * <p><strong>A quarter-turn, never more.</strong> The client slerps to the new rotation, and
	 * slerp takes the short way round — so a half-turn is ambiguous and anything past it doubles
	 * back. Four quarters is the largest step that always advances, and stepping through
	 * {@code quarter % 4} keeps the angle exact however long the server has been up.
	 *
	 * <p><strong>The delay is written forced.</strong> {@code Display.onSyncedDataUpdated} restarts
	 * the interpolation clock for that key alone — a changed rotation only marks the render state
	 * dirty — and synched data drops a write that does not change the value. Writing the same zero
	 * unforced would start the first leg of the spin and no other, leaving the legendaries parked at
	 * ninety degrees.
	 */
	public static void spin(MinecraftServer server) {
		if (server.getTickCount() % SPIN_STEP_TICKS != 0) {
			return;
		}
		BlockPos pos = LegendaryState.get(server).pedestalPos();
		if (pos == null) {
			return;
		}
		ServerLevel level = LegendaryState.home(server);
		// Asked before ours(), which loads the chunk to search it. Turning something nobody can see
		// is not worth keeping the spawn chunk warm for.
		if (!level.areEntitiesLoaded(ChunkPos.pack(pos))) {
			return;
		}
		int quarter = (server.getTickCount() / SPIN_STEP_TICKS) % 4;
		Quaternionf facing = new Quaternionf().rotateY(quarter * QUARTER_TURN);
		for (Entity entity : ours(level, pos)) {
			if (!(entity instanceof Display.ItemDisplay display)) {
				continue;
			}
			display.getEntityData().set(DisplayTransformAccessor.interpolationDurationId(), SPIN_STEP_TICKS);
			display.getEntityData().set(DisplayTransformAccessor.interpolationDelayId(), 0, true);
			display.getEntityData().set(DisplayTransformAccessor.leftRotationId(), facing);
		}
	}

	/** Whether anything at all is standing on the pedestal, without disturbing it. */
	public static boolean isEmpty(MinecraftServer server) {
		LegendaryState state = LegendaryState.get(server);
		return state.onPedestal().isEmpty();
	}

	private static void showSlot(ServerLevel level, BlockPos pos, Legendary legendary, ItemStack stack) {
		clearSlot(level, pos, legendary);
		Display.ItemDisplay shown = Pedestal.<Display.ItemDisplay>type("item_display")
				.create(level, EntitySpawnReason.COMMAND);
		if (shown == null) {
			return;
		}
		// Slots are spread along X by enum order so two legendaries do not occupy the same point.
		// The width of a slot comes from the case, so however many legendaries there are they are
		// laid out inside the glass rather than spilling out of it.
		int slots = Legendary.values().length;
		double offset = (legendary.ordinal() - (slots - 1) / 2.0) * PlinthShape.slotWidth(slots);
		shown.snapTo(pos.getX() + 0.5 + offset, pos.getY() + PlinthShape.CASE_CENTRE_Y, pos.getZ() + 0.5,
				0.0f, 0.0f);
		// Rendered exactly as a dropped item is. GROUND is the model's own transform for lying on
		// the floor, so the legendary in the case is the shape players already know, at the size the
		// model itself specifies — rather than a held item scaled by a number somebody guessed.
		shown.getEntityData().set(ItemDisplayAccessor.itemDisplayId(), ItemDisplayContext.GROUND.getId());
		float scale = PlinthShape.itemScale(slots);
		shown.getEntityData().set(DisplayTransformAccessor.scaleId(), new Vector3f(scale, scale, scale));
		shown.getSlot(0).set(stack);
		shown.addTag(TAG);
		shown.addTag(SHAPE_TAG);
		shown.addTag(SLOT_TAG_PREFIX + legendary.name());
		level.addFreshEntity(shown);
	}

	private static void clearSlot(ServerLevel level, BlockPos pos, Legendary legendary) {
		for (Entity entity : ours(level, pos)) {
			if (entity.entityTags().contains(SLOT_TAG_PREFIX + legendary.name())) {
				entity.discard();
			}
		}
	}

	/** Every legendary stack the pedestal's displays are holding. */
	private static List<ItemStack> heldStacks(ServerLevel level, BlockPos pos) {
		List<ItemStack> held = new ArrayList<>();
		for (Entity entity : ours(level, pos)) {
			if (entity instanceof Display.ItemDisplay display) {
				ItemStack candidate = display.getSlot(0).get();
				if (Legendary.isAny(candidate)) {
					held.add(candidate.copy());
				}
			}
		}
		return held;
	}

	private static void buildFixtures(ServerLevel level, BlockPos pos) {
		buildTiers(level, pos, PlinthShape.LIVE, TAG, SHAPE_TAG);

		Interaction click = Pedestal.<Interaction>type("interaction").create(level, EntitySpawnReason.COMMAND);
		if (click != null) {
			// The case alone answers a right-click: it is the thing with a legendary visibly inside
			// it, and the plinth holding it up is scenery. An Interaction grows upward from its
			// position, so it starts at the case's underside and is exactly the glass.
			click.snapTo(pos.getX() + 0.5, pos.getY() + PlinthShape.CASE_BOTTOM_Y, pos.getZ() + 0.5,
					0.0f, 0.0f);
			click.getEntityData().set(InteractionAccessor.widthId(), PlinthShape.CASE_SIZE);
			click.getEntityData().set(InteractionAccessor.heightId(), PlinthShape.CASE_SIZE);
			click.addTag(TAG);
			click.addTag(SHAPE_TAG);
			level.addFreshEntity(click);
		}
	}

	/**
	 * Spawns one plinth's worth of block displays, carrying exactly the tags the caller asks for.
	 *
	 * <p>The caller supplies every tag, including {@link #TAG}. Adding that here would mean preview
	 * plinths wore the real pedestal's tag, and {@link #discardStaleOnLoad} would delete them the
	 * instant they spawned for standing somewhere the pedestal is not — which is that method doing
	 * its job, on the wrong entities.
	 */
	public static void buildTiers(ServerLevel level, BlockPos pos, PlinthShape.Tier[] tiers, String... tags) {
		for (PlinthShape.Tier tier : tiers) {
			Display.BlockDisplay part = Pedestal.<Display.BlockDisplay>type("block_display")
					.create(level, EntitySpawnReason.COMMAND);
			if (part == null) {
				continue;
			}
			// Every tier sits at the same entity position; scale and translation give it its size
			// and its height in the stack. Translation is applied in the display's own space, so
			// halving the scale halves what a unit of translation moves.
			part.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
			part.getEntityData().set(BlockDisplayAccessor.blockStateId(), tier.block().defaultBlockState());
			part.getEntityData().set(DisplayTransformAccessor.scaleId(),
					new Vector3f(tier.scaleX(), tier.scaleY(), tier.scaleZ()));
			// A block model grows from its own origin, so horizontal centring costs half its scaled
			// width — and the vertical offset is simply where its underside goes.
			part.getEntityData().set(DisplayTransformAccessor.translationId(),
					new Vector3f(-tier.scaleX() / 2.0f, tier.bottomY(), -tier.scaleZ() / 2.0f));
			for (String tag : tags) {
				part.addTag(tag);
			}
			level.addFreshEntity(part);
		}
	}

	/** Floats a line of text above a position, so a row of preview plinths can be told apart. */
	public static void label(ServerLevel level, BlockPos pos, String text, String... tags) {
		Display.TextDisplay label = Pedestal.<Display.TextDisplay>type("text_display")
				.create(level, EntitySpawnReason.COMMAND);
		if (label == null) {
			return;
		}
		label.snapTo(pos.getX() + 0.5, pos.getY() + 2.4, pos.getZ() + 0.5, 0.0f, 0.0f);
		label.getEntityData().set(TextDisplayAccessor.textId(), Component.literal(text));
		for (String tag : tags) {
			label.addTag(tag);
		}
		level.addFreshEntity(label);
	}

	/** Removes the pedestal's entities without touching the state's record of where it is. */
	public static void clearEntities(ServerLevel level, BlockPos pos) {
		for (Entity entity : ours(level, pos)) {
			entity.discard();
		}
	}

	/**
	 * Discards our entities wherever they turn out not to belong.
	 *
	 * <p>Clearing by search cannot be relied on: an entity in an unloaded chunk is not in the world's
	 * entity index, and touching the chunk does not put it there synchronously — entity sections load
	 * on their own schedule. So rather than trying to find stale entities at the moment we create new
	 * ones, we recognise them when they load and remove them then.
	 */
	public static void discardStaleOnLoad(Entity entity, ServerLevel level) {
		if (!entity.entityTags().contains(TAG)) {
			return;
		}
		if (level.dimension() != Level.OVERWORLD) {
			// A pedestal is only ever built in the overworld, so anything wearing this tag in another
			// dimension was not put there by us. Judging it would mean deleting somebody else's
			// entity on the strength of a name collision.
			return;
		}
		MinecraftServer server = level.getServer();
		if (server == null) {
			return;
		}
		BlockPos current = LegendaryState.get(server).pedestalPos();
		// Position alone decides. The pedestal stands whether or not it is holding anything, so
		// asking whether a legendary is home would sweep away the empty plinth that is the whole
		// point of it being permanent.
		boolean belongs = current != null && entity.blockPosition().closerThan(current, SEARCH_RADIUS);
		if (!belongs) {
			entity.discard();
		}
	}

	private static List<Entity> ours(ServerLevel level, BlockPos pos) {
		// An entity in an unloaded chunk is not in the world's entity index, so a search would come
		// back empty and the caller would conclude there is nothing there.
		level.getChunkAt(pos);
		AABB box = new AABB(pos).inflate(SEARCH_RADIUS);
		return level.getEntities((Entity) null, box, entity -> entity.entityTags().contains(TAG));
	}
}
