package io.dnbg.minecraft.legendaries.legendary;

import java.util.Arrays;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;

/**
 * The plinth's silhouette, and the candidates being compared against it.
 *
 * <p>A tier is a block, a width, a height and the height of its centre; a display renders its block
 * full-size and centred on the entity, so scale and translation fall out of those four numbers.
 *
 * <p><strong>Scale a block unevenly and its texture stretches.</strong> A full block squashed to a
 * tenth of its height renders its 16x16 faces at 16x2, and the result reads as smeared rather than
 * as stone. {@link #uniform} is the way out: it takes a scale and derives the height from what the
 * block naturally is, so a slab stays a slab and every pixel stays square. Thin tiers come from
 * naturally thin blocks rather than from crushing tall ones.
 */
public final class PlinthShape {
	/**
	 * One tier: a block, the scale applied to each axis, and the height its underside sits at.
	 *
	 * <p>These are <em>scale factors</em>, not world sizes, and the distinction is the whole reason
	 * the record is shaped this way. A slab is already half a block tall, so a slab at scale 1 is
	 * 0.5 high — feeding a wanted height of 0.5 in as the scale would render it 0.25 high and
	 * squashed. Ask for a scale; read {@link #worldHeight()} for what you get.
	 */
	public record Tier(Block block, float scaleX, float scaleY, float scaleZ, float bottomY) {
		/** How tall this tier actually stands, once its block's own proportions are applied. */
		public float worldHeight() {
			return scaleY * naturalHeight(block);
		}

		public float topY() {
			return bottomY + worldHeight();
		}

		public float centreY() {
			return bottomY + worldHeight() / 2.0f;
		}
	}

	private static final Block DARK = Blocks.POLISHED_DEEPSLATE;
	private static final Block DARK_SLAB = Blocks.POLISHED_DEEPSLATE_SLAB;

	/**
	 * The case the legendary sits inside.
	 *
	 * <p>Looked up by id rather than named as a constant, for the same reason the entity types are:
	 * {@code Blocks.PURPLE_STAINED_GLASS} exists in 26.1 and 26.1.2 and was replaced by the
	 * {@code Blocks.STAINED_GLASS} colour collection in 26.2, so either spelling compiles against
	 * half the supported range and fails on the other half.
	 */
	private static final Block CASE = BuiltInRegistries.BLOCK.getValue(
			Identifier.withDefaultNamespace("purple_stained_glass"));

	/** How tall this block is in its own right, before any scaling. */
	private static float naturalHeight(Block block) {
		return block instanceof SlabBlock ? 0.5f : 1.0f;
	}

	/**
	 * A tier scaled evenly, sitting on top of whatever is at {@code bottomY}.
	 *
	 * <p>Even scaling is the whole point: width and height move together, so the block's own
	 * proportions survive and its texture is not stretched.
	 */
	private static Tier uniform(Block block, float scale, float bottomY) {
		return new Tier(block, scale, scale, scale, bottomY);
	}

	/** Stacks tiers flush, each sitting on the one below, and returns the finished profile. */
	private static Tier[] stack(Object... blockThenScale) {
		Tier[] tiers = new Tier[blockThenScale.length / 2];
		float y = 0.0f;
		for (int i = 0; i < tiers.length; i++) {
			Block block = (Block) blockThenScale[i * 2];
			float scale = ((Number) blockThenScale[i * 2 + 1]).floatValue();
			tiers[i] = uniform(block, scale, y);
			y = tiers[i].topY();
		}
		return tiers;
	}

	/** How wide the case is, as a fraction of a block. Narrower than the cap it sits on. */
	private static final float CASE_SCALE = 0.7f;

	/**
	 * Appends the case to a profile, so no caller has to remember to and no profile can omit it.
	 *
	 * <p>The case being last is relied on nowhere: {@link #caseCentre} finds it by block. Every
	 * profile gets one, which is what makes {@link #VARIANTS} a like-for-like comparison.
	 */
	private static Tier[] cased(Object... blockThenScale) {
		Object[] all = Arrays.copyOf(blockThenScale, blockThenScale.length + 2);
		all[blockThenScale.length] = CASE;
		all[blockThenScale.length + 1] = CASE_SCALE;
		return stack(all);
	}

	/**
	 * The pedestal as it actually stands, glass case included.
	 *
	 * <p>Two things carry the look, and both are texture rather than geometry. The shaft is
	 * <strong>chiseled stone bricks</strong>, whose face is a bordered frame around a sunken inner
	 * square — the recessed panel a display entity cannot carve, drawn by the block itself. And the
	 * foot is <strong>two slabs</strong> rather than one, which is the stepped base of a plinth for
	 * the price of one more display.
	 *
	 * <p><strong>There are deliberately no corner brackets.</strong> The obvious next ornament is a
	 * stair at each top corner, and it does not work at any size: whichever corner faces the viewer
	 * lands in the middle of the visible face, on top of the panel it was meant to set off. Smaller
	 * makes it a wart rather than a corbel, and there is no offset that clears the panel without
	 * overhanging the cap. The panel is doing the work; leave it visible.
	 *
	 * <p>The case is narrower than the cap it sits on, so it reads as set down on the plinth rather
	 * than as another tier of it.
	 */
	public static final Tier[] LIVE = cased(
			DARK_SLAB, 1.06f,
			DARK_SLAB, 0.9f,
			Blocks.CHISELED_STONE_BRICKS, 0.8f,
			DARK_SLAB, 0.95f);

	/** Where the item floats: the middle of the glass case, so it is held inside it. */
	public static final float CASE_CENTRE_Y = caseCentre();

	/**
	 * Applied on top of the item's own GROUND transform, which already renders it at dropped size.
	 * Above 1 to lift it off that baseline, because a dropped item alone is small inside the case.
	 */
	public static final float ITEM_SCALE = 1.6f;

	private static float caseCentre() {
		for (Tier tier : LIVE) {
			if (tier.block() == CASE) {
				return tier.centreY();
			}
		}
		throw new IllegalStateException("the live plinth has no case for the legendary to sit in");
	}

	public record Variant(String label, Tier[] tiers) {
	}

	/**
	 * The candidates {@code /legendaries debug plinths} stands in a row, so a shape can be judged
	 * beside the alternatives rather than on its own.
	 *
	 * <p>Every one is {@link #cased}, so the row compares plinths rather than plinths against
	 * bare stacks. {@code stretched} is the shape from before even scaling, kept so the reason for
	 * that rule stays visible rather than merely asserted.
	 */
	public static final Variant[] VARIANTS = {
		new Variant("stretched (old)", new Tier[] {
			// Deliberately uneven, kept so the comparison is visible rather than asserted. These
			// are the numbers from before uniform scaling; every one of them squashes its block.
			new Tier(DARK, 1.0f, 0.12f, 1.0f, 0.0f),
			new Tier(DARK, 0.9f, 0.10f, 0.9f, 0.12f),
			new Tier(Blocks.LODESTONE, 0.82f, 0.72f, 0.82f, 0.22f),
			new Tier(DARK, 0.86f, 0.06f, 0.86f, 0.94f),
			new Tier(DARK, 0.9f, 0.10f, 0.9f, 1.0f),
			new Tier(DARK, 1.0f, 0.14f, 1.0f, 1.1f),
		}),
		new Variant("live", LIVE),
		new Variant("lodestone shaft", cased(
				DARK_SLAB, 1.06f, DARK_SLAB, 0.9f, Blocks.LODESTONE, 0.8f, DARK_SLAB, 0.95f)),
		new Variant("plain foot", cased(
				DARK_SLAB, 1.0f, Blocks.CHISELED_STONE_BRICKS, 0.8f, DARK_SLAB, 0.95f)),
		new Variant("squat", cased(
				DARK_SLAB, 1.06f, DARK_SLAB, 0.9f, Blocks.CHISELED_STONE_BRICKS, 0.62f, DARK_SLAB, 0.95f)),
		new Variant("tall", cased(
				DARK_SLAB, 1.06f, DARK_SLAB, 0.9f, Blocks.CHISELED_STONE_BRICKS, 1.0f, DARK_SLAB, 0.95f)),
		new Variant("deepslate tiles", cased(
				Blocks.DEEPSLATE_TILE_SLAB, 1.06f, Blocks.DEEPSLATE_TILE_SLAB, 0.9f,
				Blocks.CHISELED_DEEPSLATE, 0.8f, Blocks.DEEPSLATE_TILE_SLAB, 0.95f)),
	};

	private PlinthShape() {
	}
}
