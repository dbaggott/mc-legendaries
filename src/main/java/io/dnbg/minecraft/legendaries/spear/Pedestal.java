package io.dnbg.minecraft.legendaries.spear;

import io.dnbg.minecraft.legendaries.Legendaries;
import io.dnbg.minecraft.legendaries.mixin.BlockDisplayAccessor;
import io.dnbg.minecraft.legendaries.mixin.InteractionAccessor;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Where the spear waits when nobody is carrying it.
 *
 * <p>Three entities, no blocks: a {@link Display.BlockDisplay} for the plinth, a
 * {@link Display.ItemDisplay} holding the spear, and an {@link Interaction} that turns a
 * right-click into a claim. Nothing is placed in the world, so there is nothing to mine, nothing
 * to grief, and nothing to collide with — a display entity has no collision at all.
 *
 * <p><strong>The item display holds the authoritative stack.</strong> The spear that returns here
 * is the same stack that left, kept on the display rather than copied into saved data — so a
 * rename or an added enchantment survives a trip to the pedestal, and there is no second
 * definition of "the spear" to drift from the recipe.
 */
public final class Pedestal {
	/** Scoreboard tag identifying our entities, so a player's own displays are never touched. */
	public static final String TAG = "legendaries_pedestal";

	private static final double PLINTH_SCALE_Y = 0.5;
	private static final float INTERACTION_SIZE = 1.5f;
	private static final double SEARCH_RADIUS = 3.0;

	private Pedestal() {
	}

	/**
	 * Looks an entity type up by id rather than naming a constant.
	 *
	 * <p>The constants moved: {@code EntityType.BLOCK_DISPLAY} in 26.1 and 26.1.2 became
	 * {@code EntityTypes.BLOCK_DISPLAY} in 26.2, so either spelling breaks half the supported range.
	 * {@code BuiltInRegistries.ENTITY_TYPE} and {@code Registry.getValue} are unchanged across all
	 * of it, and the ids are datapack-visible names that cannot move without breaking every world.
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Entity> EntityType<T> type(String id) {
		return (EntityType<T>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id));
	}

	/**
	 * The pedestal's position, placing it at world spawn the first time it is needed.
	 *
	 * <p>World spawn is only the initial siting. Once stored, the position is the pedestal's own —
	 * moving world spawn afterwards does not move the pedestal, and neither does anything but the
	 * admin command.
	 */
	public static BlockPos position(MinecraftServer server, SpearState state) {
		BlockPos stored = state.pedestalPos();
		if (stored != null) {
			return stored;
		}
		ServerLevel level = SpearState.home(server);
		BlockPos spawn = level.getRespawnData().globalPos().pos();
		// Generate the column before asking how tall it is. A heightmap read against a chunk that
		// does not exist yet answers with the bottom of the world, which would bury the pedestal in
		// the bedrock — and the spear is only ever lost at moments nobody chose, so the spawn chunk
		// being cold is the normal case rather than the odd one.
		level.getChunkAt(spawn);
		BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
		state.setPedestalPos(surface);
		return surface;
	}

	/** Puts the spear on the pedestal, replacing anything already standing there. */
	public static void place(MinecraftServer server, ItemStack spear) {
		SpearState state = SpearState.get(server);
		if (state.spearOnPedestal()) {
			// Already standing there. Placing again would build a second pedestal on top of the
			// first, and only one of them would be the one the state points at.
			Legendaries.LOGGER.warn("Ignoring a return to the pedestal while the spear is already on it");
			return;
		}
		BlockPos pos = position(server, state);
		ServerLevel level = SpearState.home(server);
		clearEntities(level, pos);

		// Mark the spear home BEFORE spawning anything. Spawning into a loaded chunk fires the
		// entity-load event synchronously, and discardStaleOnLoad reads this flag to decide what
		// belongs — so setting it afterwards makes the pedestal delete itself on creation, but only
		// where the chunk happened to be warm.
		state.setSpearOnPedestal(true);

		Display.BlockDisplay plinth = Pedestal.<Display.BlockDisplay>type("block_display").create(level, EntitySpawnReason.COMMAND);
		if (plinth != null) {
			plinth.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
			plinth.getEntityData().set(BlockDisplayAccessor.blockStateId(), Blocks.LODESTONE.defaultBlockState());
			plinth.addTag(TAG);
			level.addFreshEntity(plinth);
		}

		Display.ItemDisplay shown = Pedestal.<Display.ItemDisplay>type("item_display").create(level, EntitySpawnReason.COMMAND);
		if (shown != null) {
			shown.snapTo(pos.getX() + 0.5, pos.getY() + 1.0 + PLINTH_SCALE_Y, pos.getZ() + 0.5, 0.0f, 0.0f);
			shown.getSlot(0).set(spear.copy());
			shown.addTag(TAG);
			level.addFreshEntity(shown);
		}

		Interaction click = Pedestal.<Interaction>type("interaction").create(level, EntitySpawnReason.COMMAND);
		if (click != null) {
			click.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
			click.getEntityData().set(InteractionAccessor.widthId(), INTERACTION_SIZE);
			click.getEntityData().set(InteractionAccessor.heightId(), INTERACTION_SIZE);
			click.addTag(TAG);
			level.addFreshEntity(click);
		}

		Legendaries.LOGGER.info("Netherite Spear returned to its pedestal at {}", pos);
	}

	/**
	 * Takes the spear off the pedestal, or returns empty if it is not there.
	 *
	 * <p>The stack comes off the item display rather than being rebuilt, so whatever the spear
	 * carried when it arrived is what leaves.
	 */
	public static ItemStack take(MinecraftServer server) {
		SpearState state = SpearState.get(server);
		if (!state.spearOnPedestal()) {
			return ItemStack.EMPTY;
		}
		BlockPos pos = position(server, state);
		ServerLevel level = SpearState.home(server);
		ItemStack held = ItemStack.EMPTY;
		for (Entity entity : ours(level, pos)) {
			if (entity instanceof Display.ItemDisplay display) {
				ItemStack candidate = display.getSlot(0).get();
				if (NetheriteSpear.is(candidate)) {
					held = candidate.copy();
				}
			}
		}
		clearEntities(level, pos);
		state.setSpearOnPedestal(false);
		return held;
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
	 * <p>Clearing by search cannot be relied on: an entity in an unloaded chunk is not in the
	 * world's entity index, and touching the chunk does not put it there synchronously — entity
	 * sections load on their own schedule. So rather than trying to find stale entities at the
	 * moment we create new ones, we recognise them when they load and remove them then.
	 *
	 * <p>That covers every way one is left behind: a pedestal relocated while its old site was
	 * cold, a duplicate from a second spear, or leftovers from a claim that happened elsewhere.
	 */
	public static void discardStaleOnLoad(Entity entity, ServerLevel level) {
		if (!entity.entityTags().contains(TAG)) {
			return;
		}
		MinecraftServer server = level.getServer();
		if (server == null) {
			return;
		}
		SpearState state = SpearState.get(server);
		BlockPos current = state.pedestalPos();
		boolean belongs = state.spearOnPedestal()
				&& current != null
				&& entity.blockPosition().closerThan(current, SEARCH_RADIUS);
		if (!belongs) {
			entity.discard();
		}
	}

	private static List<Entity> ours(ServerLevel level, BlockPos pos) {
		// An entity in an unloaded chunk is not in the world's entity index, so a search would come
		// back empty and the caller would conclude there is nothing there. Loading the chunk first
		// is what stops a pedestal from being duplicated across a restart: without it, a relocate
		// while the old site is cold leaves its entities standing and orphaned for good.
		level.getChunkAt(pos);
		AABB box = new AABB(pos).inflate(SEARCH_RADIUS);
		return level.getEntities((Entity) null, box, entity -> entity.entityTags().contains(TAG));
	}
}
