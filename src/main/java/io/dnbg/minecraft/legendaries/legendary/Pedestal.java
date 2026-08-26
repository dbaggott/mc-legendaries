package io.dnbg.minecraft.legendaries.legendary;

import io.dnbg.minecraft.legendaries.Legendaries;
import io.dnbg.minecraft.legendaries.mixin.BlockDisplayAccessor;
import io.dnbg.minecraft.legendaries.mixin.InteractionAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

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

	private static final double HOVER = 1.25;
	private static final double SLOT_SPREAD = 0.4;
	private static final float INTERACTION_SIZE = 1.5f;
	private static final double SEARCH_RADIUS = 3.0;

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

	/** Whether the plinth, the click target and exactly the expected item displays are all present. */
	private static boolean structureIntact(ServerLevel level, BlockPos pos, LegendaryState state) {
		int plinths = 0;
		int targets = 0;
		int displays = 0;
		for (Entity entity : ours(level, pos)) {
			if (entity instanceof Display.BlockDisplay) {
				plinths++;
			} else if (entity instanceof Interaction) {
				targets++;
			} else if (entity instanceof Display.ItemDisplay) {
				displays++;
			}
		}
		return plinths == 1 && targets == 1 && displays == state.onPedestal().size();
	}

	/** Puts a legendary on the pedestal, building it first if it somehow is not there. */
	public static void place(MinecraftServer server, ItemStack stack) {
		Legendary legendary = Legendary.of(stack).orElse(null);
		if (legendary == null) {
			return;
		}
		LegendaryState state = LegendaryState.get(server);
		if (state.onPedestal(legendary)) {
			Legendaries.LOGGER.warn("Ignoring a return of {} while it is already on the pedestal", legendary);
			return;
		}
		BlockPos pos = position(server, state);
		ServerLevel level = LegendaryState.home(server);
		// Mark it home BEFORE spawning anything. Spawning into a loaded chunk fires the entity-load
		// event synchronously, and discardStaleOnLoad reads this state to decide what belongs — so
		// setting it afterwards makes the display delete itself on creation, but only where the
		// chunk happened to be warm.
		state.setOnPedestal(legendary, true);
		ensure(server);
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
		double offset = (legendary.ordinal() - (Legendary.values().length - 1) / 2.0) * SLOT_SPREAD;
		shown.snapTo(pos.getX() + 0.5 + offset, pos.getY() + HOVER, pos.getZ() + 0.5, 0.0f, 0.0f);
		shown.getSlot(0).set(stack);
		shown.addTag(TAG);
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
		Display.BlockDisplay plinth = Pedestal.<Display.BlockDisplay>type("block_display")
				.create(level, EntitySpawnReason.COMMAND);
		if (plinth != null) {
			plinth.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
			plinth.getEntityData().set(BlockDisplayAccessor.blockStateId(), Blocks.LODESTONE.defaultBlockState());
			plinth.addTag(TAG);
			level.addFreshEntity(plinth);
		}

		Interaction click = Pedestal.<Interaction>type("interaction").create(level, EntitySpawnReason.COMMAND);
		if (click != null) {
			click.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
			click.getEntityData().set(InteractionAccessor.widthId(), INTERACTION_SIZE);
			click.getEntityData().set(InteractionAccessor.heightId(), INTERACTION_SIZE);
			click.addTag(TAG);
			level.addFreshEntity(click);
		}
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
