package io.dnbg.minecraft.legendaries.legendary;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Mace's ability: a sphere of the world around the player is annihilated, and the shell left
 * behind is turned to molten rock.
 *
 * <p>Two passes over the same region, and the order is what makes the shell a shell. The first
 * erases everything within {@link #BLAST_RADIUS}; the second converts blocks just outside it that
 * survived the first and touch the void it left. Doing both in one pass would let a block be
 * converted and then erased, leaving the crater edge ragged.
 */
public final class MoltenBlast {
	public static final int COOLDOWN_TICKS = 60 * 20;

	private static final int BLAST_RADIUS = 4;
	/** One block of reach past the crater, which is where the shell can be. */
	private static final int SHELL_RADIUS = BLAST_RADIUS + 1;
	/** setBlock flag: update neighbours and notify clients, the ordinary "a block changed" set. */
	private static final int BLOCK_UPDATE = Block.UPDATE_ALL;

	/**
	 * The molten palette and its weights, in step.
	 *
	 * <p>13:13:13:1 puts lava at exactly one shell block in forty. That is a quarter of the rate it
	 * started at — the first cut was 3:3:3:1, or one in ten — and the odd-looking 13 is what makes
	 * the quarter exact rather than approximate.
	 *
	 * <p>Explicit weights rather than a flat array with one entry per weight: at these numbers that
	 * array would be forty entries long, and a reader would have to count them to learn the odds.
	 */
	private static final Block[] MOLTEN = {
		Blocks.MAGMA_BLOCK, Blocks.NETHERRACK, Blocks.COAL_BLOCK, Blocks.LAVA,
	};
	private static final int[] MOLTEN_WEIGHTS = {13, 13, 13, 1};

	private MoltenBlast() {
	}

	/** Fires the blast, centred on the player. */
	public static void fire(ServerLevel level, Player player) {
		BlockPos centre = player.blockPosition();
		RandomSource random = level.getRandom();

		// Pass one: erase the crater. No drops — even a radius-4 sphere is a couple of hundred
		// blocks, and dropping them would bury the player in item entities and cost the server more
		// than the blast itself.
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-BLAST_RADIUS, -BLAST_RADIUS, -BLAST_RADIUS),
				centre.offset(BLAST_RADIUS, BLAST_RADIUS, BLAST_RADIUS))) {
			if (!within(centre, pos, BLAST_RADIUS) || !destructible(level, pos)) {
				continue;
			}
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE);
		}

		// Pass two: melt the shell. Only blocks that were already there, are outside the crater, and
		// touch it — so the crater gets a molten lining rather than the whole neighbourhood changing.
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-SHELL_RADIUS, -SHELL_RADIUS, -SHELL_RADIUS),
				centre.offset(SHELL_RADIUS, SHELL_RADIUS, SHELL_RADIUS))) {
			if (within(centre, pos, BLAST_RADIUS) || !destructible(level, pos) || !bordersCrater(centre, pos)) {
				continue;
			}
			level.setBlock(pos, molten(random).defaultBlockState(), BLOCK_UPDATE);
		}
	}

	/** A weighted draw from the palette. */
	private static Block molten(RandomSource random) {
		int total = 0;
		for (int weight : MOLTEN_WEIGHTS) {
			total += weight;
		}
		int roll = random.nextInt(total);
		for (int i = 0; i < MOLTEN.length; i++) {
			roll -= MOLTEN_WEIGHTS[i];
			if (roll < 0) {
				return MOLTEN[i];
			}
		}
		// Unreachable: roll is bounded by the same total the loop subtracts.
		return MOLTEN[0];
	}

	private static boolean within(BlockPos centre, BlockPos pos, int radius) {
		return centre.distSqr(pos) <= (double) radius * radius;
	}

	/** Whether this position touches the crater on one of its six faces. */
	private static boolean bordersCrater(BlockPos centre, BlockPos pos) {
		for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
			if (within(centre, pos.relative(direction), BLAST_RADIUS)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether this block may be touched at all.
	 *
	 * <p>Air is skipped because there is nothing to do, and negative hardness is skipped because that
	 * is how vanilla marks the blocks no tool may break — bedrock, barriers, end portal frames. Left
	 * in, a blast at the right altitude would hole the world floor or the Nether roof permanently.
	 */
	private static boolean destructible(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return !state.isAir() && state.getDestroySpeed(level, pos) >= 0;
	}
}
