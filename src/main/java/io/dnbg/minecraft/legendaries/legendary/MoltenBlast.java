package io.dnbg.minecraft.legendaries.legendary;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
	 * What a shell block becomes, and how often.
	 *
	 * <p>A {@code null} entry means <em>leave it as it is</em>. It is an outcome rather than a
	 * separate skip so the mix stays in one table: retuning how much original rock shows through is
	 * a weight change here, not a second concept somewhere else.
	 *
	 * <p>4:4:4:3 totals fifteen, putting leave-as-is at exactly three in fifteen — one shell block in
	 * five keeps whatever it was made of, which is what stops the lining reading as a uniform coat.
	 *
	 * <p>Lava was among these and is not any more: it flowed out of the shell and kept going, so a
	 * blast left a spreading mess rather than a crater.
	 */
	private static final Block[] SHELL = {
		Blocks.MAGMA_BLOCK, Blocks.NETHERRACK, Blocks.COAL_BLOCK, null,
	};
	private static final int[] SHELL_WEIGHTS = {4, 4, 4, 3};

	/** Explosion puffs across the crater, and flames clinging to each newly-molten shell block. */
	private static final int EXPLOSION_PUFFS = 40;
	private static final int FLAMES_PER_SHELL_BLOCK = 2;

	private MoltenBlast() {
	}

	/** Fires the blast, centred on the player. */
	public static void fire(ServerLevel level, Player player) {
		BlockPos centre = player.blockPosition();
		RandomSource random = level.getRandom();
		// Particles go out before the blocks change, so the burst reads as the cause of the crater
		// rather than an afterthought once the ground has already gone.
		announce(level, centre);

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
			Block molten = shellOutcome(random);
			if (molten == null) {
				continue;
			}
			level.setBlock(pos, molten.defaultBlockState(), BLOCK_UPDATE);
			// Flames on the face that looks into the crater, so the lining reads as freshly molten.
			level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
					FLAMES_PER_SHELL_BLOCK, 0.3, 0.3, 0.3, 0.01);
		}
	}

	/**
	 * The visible burst.
	 *
	 * <p>{@code sendParticles} is a server-to-client packet, so this reaches a vanilla client with
	 * nothing installed — which is the property the whole mod is built around. A blast that only
	 * modded clients could see would be worse than none.
	 *
	 * <p>{@code EXPLOSION_EMITTER} is the single large bloom TNT uses; the scattered
	 * {@code EXPLOSION} puffs are spread across the crater's width so the burst fills the volume the
	 * blast is about to clear rather than sitting at one point.
	 */
	private static void announce(ServerLevel level, BlockPos centre) {
		double x = centre.getX() + 0.5;
		double y = centre.getY() + 0.5;
		double z = centre.getZ() + 0.5;
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, EXPLOSION_PUFFS,
				BLAST_RADIUS * 0.5, BLAST_RADIUS * 0.5, BLAST_RADIUS * 0.5, 0.0);
		level.sendParticles(ParticleTypes.FLAME, x, y, z, EXPLOSION_PUFFS * 2,
				BLAST_RADIUS * 0.5, BLAST_RADIUS * 0.5, BLAST_RADIUS * 0.5, 0.05);
	}

	/** A weighted draw from {@link #SHELL}; null means leave the block as it is. */
	private static Block shellOutcome(RandomSource random) {
		int total = 0;
		for (int weight : SHELL_WEIGHTS) {
			total += weight;
		}
		int roll = random.nextInt(total);
		for (int i = 0; i < SHELL.length; i++) {
			roll -= SHELL_WEIGHTS[i];
			if (roll < 0) {
				return SHELL[i];
			}
		}
		// Unreachable: roll is bounded by the same total the loop subtracts.
		return null;
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
