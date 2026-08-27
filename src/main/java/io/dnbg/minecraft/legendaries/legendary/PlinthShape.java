package io.dnbg.minecraft.legendaries.legendary;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;

/**
 * The plinth's silhouette, and the candidates being compared against it.
 *
 * <p>A plinth is plates and a column: thin bands top and bottom, something with a face between
 * them. A display entity renders whatever block it is given at whatever scale it is given, so both
 * halves are available — but they want opposite treatment, and that is the whole content of this
 * file.
 *
 * <h2>The column is scaled evenly; the plates are not</h2>
 *
 * <p>Scale a block unevenly and its texture stretches with it. That is fatal for the column,
 * because the column is chosen for its face: squash the concentric motif on a lodestone and the
 * motif is what you ruin. {@link #even} is the rule there — one scale on every axis, so the block's
 * own proportions and every pixel of its face survive.
 *
 * <p>A plate is the opposite case. It has to be thin, and there is no vanilla stone block thin
 * enough — a slab, the thinnest, is still half a block, so a foot and a cap built from slabs cost a
 * block of height between them and the plinth becomes a column with no room for anything else.
 * Plates are therefore squashed, and {@link #plate} takes the thickness it should end up rather
 * than a scale, so the number in the table is the thing being chosen.
 *
 * <h2>Which block a plate may be made of</h2>
 *
 * <p><strong>Squashing merges rows of the texture, so only a block whose rows resemble each other
 * may be a plate.</strong> A brick or tile pattern has strong row-to-row structure and turns to
 * mush; a flat or finely-speckled texture loses nothing you could name. {@link #PLATE} is the
 * darkest block in the stone family whose rows differ least — compare it against
 * {@code polished_deepslate}, which was the plate before and whose banding is visibly smeared at
 * these thicknesses.
 *
 * <p>So the prohibition is on stretching a block <em>chosen for its face</em>, not on stretching.
 * Anything with a motif — the column, the case — is {@link #even}. Only plates are squashed, and
 * only from a block picked for surviving it.
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

	/** The column: chosen for the concentric frame on its side, which is why it is never squashed. */
	private static final Block COLUMN = Blocks.LODESTONE;

	/**
	 * The plates.
	 *
	 * <p>Picked by measuring how much each candidate's texture changes from one row to the next,
	 * because that is exactly what squashing destroys. Of the dark stone-family blocks this was the
	 * flattest, so it survives being a tenth of a block tall with nothing visibly smeared, and it is
	 * dark enough to frame the column rather than blend into it.
	 */
	private static final Block PLATE = Blocks.NETHERITE_BLOCK;

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

	/** How wide the case is. Narrower than the cap, so it reads as set down on the plinth. */
	private static final float CASE_SCALE = 0.7f;

	/** How tall this block is in its own right, before any scaling. */
	private static float naturalHeight(Block block) {
		return block instanceof SlabBlock ? 0.5f : 1.0f;
	}

	/**
	 * Builds a profile a tier at a time, each sitting flush on the one below.
	 *
	 * <p>Nothing outside here computes a {@code bottomY}: the builder carries the running height, so
	 * a tier cannot be left floating above the one under it or sunk into it. Getting that wrong by
	 * hand is what once made the plinth come apart in-world.
	 */
	private static final class Profile {
		private final List<Tier> tiers = new ArrayList<>();
		private float y = 0.0f;

		/** A tier scaled evenly, so the block's proportions and its face both survive. */
		Profile even(Block block, float scale) {
			return add(new Tier(block, scale, scale, scale, y));
		}

		/**
		 * A plate of exactly {@code thickness}, however tall its block naturally is.
		 *
		 * <p>The caller names the thickness rather than a scale because the thickness is the thing
		 * being chosen; the squash needed to reach it is arithmetic.
		 */
		Profile plate(Block block, float width, float thickness) {
			return add(new Tier(block, width, thickness / naturalHeight(block), width, y));
		}

		private Profile add(Tier tier) {
			tiers.add(tier);
			y = tier.topY();
			return this;
		}

		/**
		 * Closes the profile with the glass case, so no profile can omit one.
		 *
		 * <p>Every profile getting a case is what makes {@link #VARIANTS} a like-for-like row rather
		 * than plinths compared against bare stacks.
		 */
		Tier[] cased() {
			even(CASE, CASE_SCALE);
			return tiers.toArray(new Tier[0]);
		}
	}

	/**
	 * The plinth, as a function of what it is made of.
	 *
	 * <p>One copy of the numbers, so a variant differs from the live shape in exactly the block it
	 * names and nothing can drift between them.
	 *
	 * <p><strong>The base is wider than the cap, and that is what makes them look equal.</strong>
	 * They were both 1.0 and the base read as narrower, because silhouette is not what the eye
	 * measures here: the cap's widest plate shows its whole top face, while the base's showed a
	 * ledge of 0.05 peeking from under the plate above it. Widening the base gives it a top face to
	 * be seen by. The reference the shape is modelled on does the same thing.
	 */
	private static Tier[] profile(Block plate, Block column) {
		return new Profile()
				.plate(plate, 1.14f, 0.16f)
				.plate(plate, 0.98f, 0.10f)
				.even(column, 0.8f)
				.plate(plate, 0.86f, 0.06f)
				.plate(plate, 0.9f, 0.10f)
				.plate(plate, 1.0f, 0.14f)
				.cased();
	}

	/**
	 * The pedestal as it actually stands, glass case included.
	 *
	 * <p>A stepped foot, a lodestone column, a stepped cap and the case. The steps are what make it
	 * read as a plinth rather than a post, and they are only affordable because plates are squashed
	 * — six of them together cost half a block of height, where six slabs would cost three.
	 */
	public static final Tier[] LIVE = profile(PLATE, COLUMN);

	/**
	 * How tall the plinth stands and how wide it is at its widest, case included.
	 *
	 * <p>These size the click target. It was a hard-coded 1.6 square, which stopped being the right
	 * answer the moment the shape changed: the plinth reaches 2.06, so the case and the legendary
	 * inside it stood above the box and aiming at the item — the obvious thing to aim at — missed.
	 * Reading the numbers off the shape means the target cannot fall behind it again.
	 */
	public static final float HEIGHT = extent(Tier::topY);
	public static final float WIDTH = extent(Tier::scaleX);

	private static float extent(java.util.function.Function<Tier, Float> of) {
		float most = 0.0f;
		for (Tier tier : LIVE) {
			most = Math.max(most, of.apply(tier));
		}
		return most;
	}

	/** Where the item floats: the middle of the glass case, so it is held inside it. */
	public static final float CASE_CENTRE_Y = caseCentre();

	/**
	 * How wide an item is at GROUND, in blocks.
	 *
	 * <p>Not a guess: {@code models/item/generated.json} gives the {@code ground} display transform
	 * a scale of 0.5, and {@code handheld} — which every legendary so far uses — inherits it. So an
	 * item rendered at GROUND and left alone is half a block across.
	 */
	private static final float GROUND_WIDTH = 0.5f;

	/** How much of the case's width the whole row of legendaries spans, leaving the rest as air. */
	private static final float CASE_FILL = 0.95f;

	/**
	 * How much of its own slot a legendary fills, so two of them do not touch.
	 */
	private static final float SLOT_FILL = 0.85f;

	/**
	 * The width one legendary gets, for a case holding {@code slots} of them.
	 *
	 * <p><strong>The spread and the scale are one decision, which is why they are computed
	 * together.</strong> A fixed spread and a separately chosen scale is what put a spear's tip
	 * outside the glass: two items 0.49 wide at ±0.2 reach 0.445, where the case reaches 0.35, and
	 * they overlap in the middle besides. Dividing the row among the slots means the legendaries
	 * always fit however many there are — a third one narrows all three rather than throwing the
	 * outer two out of the case.
	 */
	public static float slotWidth(int slots) {
		return CASE_SCALE * CASE_FILL / slots;
	}

	/**
	 * Applied on top of the item's own GROUND transform, so the legendary sits inside the case.
	 *
	 * <p>Derived rather than chosen, for the same reason as {@link #slotWidth}: a hard-coded scale
	 * goes stale the moment the case moves or a legendary is added. This was 1.6 — half a block
	 * scaled up to 0.8, inside a 0.7 case — which is how the item came to be wider than the box
	 * holding it.
	 */
	public static float itemScale(int slots) {
		return slotWidth(slots) * SLOT_FILL / GROUND_WIDTH;
	}

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
	 * <p>Each varies one thing against {@link #LIVE}: the plate block, or the column. The plate
	 * candidates are the runners-up on the row-to-row measure, kept so the choice can be re-made by
	 * eye rather than taken on the number's word.
	 */
	public static final Variant[] VARIANTS = {
		new Variant("live", LIVE),
		new Variant("deepslate plates", profile(Blocks.POLISHED_DEEPSLATE, COLUMN)),
		new Variant("blackstone plates", profile(Blocks.POLISHED_BLACKSTONE, COLUMN)),
		new Variant("basalt plates", profile(Blocks.SMOOTH_BASALT, COLUMN)),
		new Variant("chiseled column", profile(PLATE, Blocks.CHISELED_STONE_BRICKS)),
		new Variant("chiseled deepslate column", profile(PLATE, Blocks.CHISELED_DEEPSLATE)),
		new Variant("slab plates (no squash)", new Profile()
				.plate(Blocks.POLISHED_DEEPSLATE_SLAB, 1.0f, 0.5f)
				.even(COLUMN, 0.8f)
				.plate(Blocks.POLISHED_DEEPSLATE_SLAB, 0.95f, 0.475f)
				.cased()),
	};

	private PlinthShape() {
	}
}
