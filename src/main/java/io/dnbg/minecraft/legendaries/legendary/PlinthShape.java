package io.dnbg.minecraft.legendaries.legendary;

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
	public record Tier(Block block, float width, float height, float centreY) {
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
		float height = scale * naturalHeight(block);
		return new Tier(block, scale, height, bottomY + height / 2.0f);
	}

	/** Stacks tiers flush, each sitting on the one below, and returns the finished profile. */
	private static Tier[] stack(Object... blockThenScale) {
		Tier[] tiers = new Tier[blockThenScale.length / 2];
		float y = 0.0f;
		for (int i = 0; i < tiers.length; i++) {
			Block block = (Block) blockThenScale[i * 2];
			float scale = ((Number) blockThenScale[i * 2 + 1]).floatValue();
			tiers[i] = uniform(block, scale, y);
			y += tiers[i].height();
		}
		return tiers;
	}

	/**
	 * The pedestal as it actually stands, glass case included.
	 *
	 * <p>The case is narrower than the cap it sits on, so it reads as set down on the plinth rather
	 * than as another tier of it.
	 */
	public static final Tier[] LIVE = stack(
			DARK_SLAB, 1.0f,
			Blocks.LODESTONE, 0.8f,
			DARK_SLAB, 0.95f,
			CASE, 0.7f);

	/** Where the item floats: the middle of the glass case, so it is held inside it. */
	public static final float CASE_CENTRE_Y = caseCentre();

	/** How large the item renders — near its dropped size, so it sits in the case with room around it. */
	public static final float ITEM_SCALE = 0.45f;

	private static float caseCentre() {
		Tier top = LIVE[LIVE.length - 1];
		return top.centreY();
	}

	public record Variant(String label, Tier[] tiers) {
	}

	/**
	 * The candidates, spread across the three things still in question: whether even scaling fixes
	 * the stretching, what the shaft is made of, and how squat the whole thing should be.
	 *
	 * <p>{@code stretched} is the shape before this change, kept so the comparison is visible rather
	 * than asserted.
	 */
	public static final Variant[] VARIANTS = {
		new Variant("stretched (old)", new Tier[] {
			new Tier(DARK, 1.0f, 0.12f, 0.06f),
			new Tier(DARK, 0.9f, 0.10f, 0.17f),
			new Tier(Blocks.LODESTONE, 0.82f, 0.72f, 0.58f),
			new Tier(DARK, 0.86f, 0.06f, 0.97f),
			new Tier(DARK, 0.9f, 0.10f, 1.05f),
			new Tier(DARK, 1.0f, 0.14f, 1.17f),
		}),
		new Variant("even (live)", LIVE),
		new Variant("even, squat", stack(DARK_SLAB, 1.0f, Blocks.LODESTONE, 0.6f, DARK_SLAB, 0.9f)),
		new Variant("even, tall", stack(DARK_SLAB, 0.95f, Blocks.LODESTONE, 1.0f, DARK_SLAB, 0.9f)),
		new Variant("stepped foot", stack(
				DARK_SLAB, 1.05f, DARK_SLAB, 0.9f, Blocks.LODESTONE, 0.78f, DARK_SLAB, 0.95f)),
		new Variant("chiseled shaft", stack(
				DARK_SLAB, 1.0f, Blocks.CHISELED_STONE_BRICKS, 0.8f, DARK_SLAB, 0.95f)),
		new Variant("deepslate-tile shaft", stack(
				Blocks.DEEPSLATE_TILE_SLAB, 1.0f, Blocks.DEEPSLATE_TILES, 0.8f, Blocks.DEEPSLATE_TILE_SLAB, 0.95f)),
		new Variant("andesite framing", stack(
				Blocks.POLISHED_ANDESITE_SLAB, 1.0f, Blocks.LODESTONE, 0.8f, Blocks.POLISHED_ANDESITE_SLAB, 0.95f)),
	};

	private PlinthShape() {
	}
}
