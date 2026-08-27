package io.dnbg.minecraft.legendaries.legendary;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Mace's ability: a sphere of the world around the player is annihilated, and the shell left
 * behind is turned to molten rock.
 *
 * <p>Two passes over the same region, and the order is what makes the shell a shell. The first
 * erases everything within the configured radius; the second converts blocks just outside it that
 * survived the first and touch the void it left. Doing both in one pass would let a block be
 * converted and then erased, leaving the crater edge ragged.
 */
public final class MoltenBlast {
	private static final int TICKS_PER_SECOND = 20;
	/** setBlock flag: update neighbours and notify clients, the ordinary "a block changed" set. */
	private static final int BLOCK_UPDATE = Block.UPDATE_ALL;

	/**
	 * What a melted shell block becomes, drawn from evenly. How much of the shell melts at all is
	 * {@link LegendarySetting#UNMELTED}.
	 *
	 * <p>Nothing in this table may be a fluid. A fluid does not stay in the shell — it flows out of
	 * the crater and keeps going, which turns a blast into a spreading mess.
	 */
	private static final Block[] SHELL = {
		Blocks.MAGMA_BLOCK, Blocks.NETHERRACK, Blocks.COAL_BLOCK,
	};

	private static final int PERCENT = 100;

	/** Explosion puffs across the crater, and flames clinging to each newly-molten shell block. */
	private static final int EXPLOSION_PUFFS = 40;
	private static final int FLAMES_PER_SHELL_BLOCK = 2;

	/**
	 * What the blast does to anything caught in it: two and a half hearts, in health points.
	 *
	 * <p>Flat across the crater rather than falling off with distance. The blast erases the whole
	 * sphere without regard to where in it a block was, and the damage reads as the same event.
	 */
	private static final float BLAST_DAMAGE = 5.0f;

	/**
	 * How hard the blast throws what it catches, in sticks of TNT.
	 *
	 * <p>Two, and literally so: sticks detonating together each push in their own turn, so twice one
	 * stick's impulse is what two of them do.
	 */
	private static final double KNOCKBACK_TNT = 2.0;

	/**
	 * How far a stick of TNT still throws, in blocks: twice its explosion radius of four, which is
	 * the span vanilla's impulse falls off over.
	 *
	 * <p>TNT's reach rather than the crater's, because TNT is what the throw is matched to and the
	 * crater's radius is a knob. A blast configured wider than this has an outer band that is burned
	 * and not thrown.
	 */
	private static final double TNT_KNOCKBACK_REACH = 8.0;

	/**
	 * The damage this deals, defined in {@code data/legendaries/damage_type/molten_blast.json} and
	 * added to {@code #minecraft:bypasses_armor} and {@code #minecraft:no_knockback} by tags beside
	 * it. The second is what every vanilla explosion type carries: damage shoves its victim by
	 * default, away from wherever the source stood, and that shove would land on top of the impulse
	 * {@link #hurl} computes rather than instead of it.
	 *
	 * <p>A type of its own rather than {@code magic}, which is the vanilla type that bypasses armor,
	 * because a type is also what names the death message and "was killed by magic" is not what
	 * happened.
	 *
	 * <p><strong>Its {@code message_id} is deliberately a vanilla one.</strong> The id becomes the
	 * translation key, and this mod is built so a vanilla client needs nothing installed — so a key
	 * of our own would reach most players as the literal string
	 * {@code death.attack.molten_blast}. {@code explosion} is the closest true one every client
	 * already has. The borrowed id carries none of {@code explosion}'s behaviour: armor bypassing
	 * comes from the tag, and vanilla explosion damage does not bypass armor at all.
	 */
	private static final ResourceKey<DamageType> MOLTEN_BLAST = ResourceKey.create(
			Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("legendaries", "molten_blast"));

	private MoltenBlast() {
	}

	/** How long the mace waits between blasts, in ticks — configurable for testing. */
	public static int cooldownTicks(MinecraftServer server) {
		return LegendaryState.get(server).setting(Legendary.MACE, LegendarySetting.COOLDOWN) * TICKS_PER_SECOND;
	}

	/** Fires the blast, centred on the player. */
	public static void fire(ServerLevel level, Player player) {
		MinecraftServer server = level.getServer();
		LegendaryState state = LegendaryState.get(server);
		int blastRadius = state.setting(Legendary.MACE, LegendarySetting.RADIUS);
		int unmelted = state.setting(Legendary.MACE, LegendarySetting.UNMELTED);
		// One block of reach past the crater, which is where the shell can be.
		int shellRadius = blastRadius + 1;
		BlockPos centre = player.blockPosition();
		RandomSource random = level.getRandom();
		// Particles go out before the blocks change, so the burst reads as the cause of the crater
		// rather than an afterthought once the ground has already gone.
		announce(level, centre, blastRadius);
		// Before the ground goes, while everything caught in it is still standing where it was.
		engulf(level, player, centre, blastRadius);

		// Pass one: erase the crater. No drops — the sphere runs to hundreds of blocks and grows with
		// the cube of the radius, so dropping them would bury the player in item entities and cost
		// the server more than the blast itself.
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-blastRadius, -blastRadius, -blastRadius),
				centre.offset(blastRadius, blastRadius, blastRadius))) {
			if (!within(centre, pos, blastRadius) || !destructible(level, pos)) {
				continue;
			}
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE);
		}

		// Pass two: melt the shell. Only blocks that were already there, are outside the crater, and
		// touch it — so the crater gets a molten lining rather than the whole neighbourhood changing.
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-shellRadius, -shellRadius, -shellRadius),
				centre.offset(shellRadius, shellRadius, shellRadius))) {
			if (within(centre, pos, blastRadius) || !destructible(level, pos)
					|| !bordersCrater(centre, pos, blastRadius)) {
				continue;
			}
			if (random.nextInt(PERCENT) < unmelted) {
				continue;
			}
			Block molten = SHELL[random.nextInt(SHELL.length)];
			level.setBlock(pos, molten.defaultBlockState(), BLOCK_UPDATE);
			// Flames on the face that looks into the crater, so the lining reads as freshly molten.
			level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
					FLAMES_PER_SHELL_BLOCK, 0.3, 0.3, 0.3, 0.01);
		}
	}

	/**
	 * What the blast does to the living inside the crater, except the one who set it off: burns them,
	 * and throws them clear.
	 *
	 * <p>The wielder is spared because the blast is centred on them: they are always inside their
	 * own radius, so charging them for it would make every use cost two and a half hearts that no
	 * armor could soften, and fire them straight up out of their own crater. Everything else living
	 * in the sphere takes the full amount. A spectator is excluded as well: the damage already
	 * passes them by, and throwing one would be the blast reaching somebody who is not there.
	 *
	 * <p>Distance is measured from the entity's own position rather than the block it stands in,
	 * because an entity is a point and a block is not — rounding to the block would spare something
	 * standing just inside the edge and burn something just outside it.
	 */
	private static void engulf(ServerLevel level, Player player, BlockPos centre, int blastRadius) {
		DamageSource source = new DamageSource(
				level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(MOLTEN_BLAST), player);
		double reachSqr = (double) blastRadius * blastRadius;
		// The centre written out rather than asked for: BlockPos.getCenter() exists in 26.1 and was
		// gone by 26.2, and this is the same arithmetic announce() already does.
		Vec3 origin = new Vec3(centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5);
		AABB caught = new AABB(centre).inflate(blastRadius);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, caught,
				candidate -> candidate != player && !candidate.isSpectator()
						&& candidate.distanceToSqr(origin) <= reachSqr)) {
			victim.hurtServer(level, source, BLAST_DAMAGE);
			hurl(victim, origin);
		}
	}

	/**
	 * Throws one victim clear of the centre, as hard as two sticks of TNT would.
	 *
	 * <p>Vanilla's own explosion impulse, twice over: a linear falloff from the centre out to TNT's
	 * reach, along the line from the centre to the victim's eyes rather than their feet, so something
	 * standing on the blast goes up as well as out. What it drops is vanilla's line-of-sight term,
	 * because the sphere between the two is about to be air and there is nothing left to shelter
	 * behind.
	 *
	 * <p>A thrown player is handed the impulse directly, because a push alone only reaches the
	 * server's copy of their motion. What sends that copy back to the player it belongs to is the
	 * entity being marked hurt, which damage landing does as a side effect — and this damage does
	 * not always land: a victim inside its invulnerability window takes none, and a creative player
	 * never does. Without the packet those two are thrown on the server and stand still on screen.
	 *
	 * <p>Flying in creative is the exception, as it is to a real explosion. Nothing else syncs the
	 * push to them either, so withholding the packet is what leaves an admin watching a blast from
	 * above where they were.
	 */
	private static void hurl(LivingEntity victim, Vec3 centre) {
		double falloff = Math.max(1.0 - Math.sqrt(victim.distanceToSqr(centre)) / TNT_KNOCKBACK_REACH, 0.0);
		double resistance = victim.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
		Vec3 outward = victim.getEyePosition().subtract(centre).normalize();
		victim.push(outward.scale(KNOCKBACK_TNT * falloff * (1.0 - resistance)));
		if (victim instanceof ServerPlayer thrown && !(thrown.isCreative() && thrown.getAbilities().flying)) {
			thrown.connection.send(new ClientboundSetEntityMotionPacket(thrown));
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
	private static void announce(ServerLevel level, BlockPos centre, int blastRadius) {
		double x = centre.getX() + 0.5;
		double y = centre.getY() + 0.5;
		double z = centre.getZ() + 0.5;
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, EXPLOSION_PUFFS,
				blastRadius * 0.5, blastRadius * 0.5, blastRadius * 0.5, 0.0);
		level.sendParticles(ParticleTypes.FLAME, x, y, z, EXPLOSION_PUFFS * 2,
				blastRadius * 0.5, blastRadius * 0.5, blastRadius * 0.5, 0.05);
	}

	private static boolean within(BlockPos centre, BlockPos pos, int radius) {
		return centre.distSqr(pos) <= (double) radius * radius;
	}

	/** Whether this position touches the crater on one of its six faces. */
	private static boolean bordersCrater(BlockPos centre, BlockPos pos, int blastRadius) {
		for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
			if (within(centre, pos.relative(direction), blastRadius)) {
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
