package io.dnbg.minecraft.legendaries.legendary;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;

/**
 * Every legendary the mod knows about, and the one place that decides whether a stack is one.
 *
 * <p>A legendary is the <em>vanilla</em> item, marked with a flag inside
 * {@link DataComponents#CUSTOM_DATA}. Two properties of that component are why it is used rather
 * than a component of our own:
 *
 * <ul>
 *   <li>It is registered {@code .persistent(...)} and NOT {@code .networkSynchronized(...)}, so the
 *       marker is saved to disk and never sent to a client. A vanilla client can connect to a
 *       server running this mod and never sees an unknown component.
 *   <li>It is vanilla, so nothing here registers a custom item, block or component — which is what
 *       keeps the whole mod invisible to a client that does not have it.
 * </ul>
 *
 * <p>Markers are written by data, not by code: each legendary's {@link LegendarySource} declares the
 * marker in the components of whatever it produces. This type only reads them. Adding a legendary is
 * an entry here plus that one file — every rule in the mod iterates this enum rather than naming an
 * item, so none of them need to change.
 */
public enum Legendary implements Tunable {
	/** Replaces every vanilla spear recipe; see {@code data/legendaries/recipe/netherite_spear.json}. */
	NETHERITE_SPEAR("legendaries_spear", Items.NETHERITE_SPEAR, "The Legendary Spear",
			"legendary_spear", new LegendarySource.FromRecipe("legendaries:netherite_spear"),
			MobEffects.SPEED, 1, null),

	/**
	 * Crafted by the vanilla recipe, which is overridden in place to mark its result — the
	 * ingredients and pattern are untouched, so it is still "the mace recipe" to a player.
	 */
	MACE("legendaries_mace", Items.MACE, "The Legendary Mace", "legendary_mace",
			new LegendarySource.FromRecipe("minecraft:mace"), null, 0, Ability.MOLTEN_BLAST),

	/**
	 * The egg the dragon leaves, marked where it drops; see
	 * {@code data/minecraft/loot_table/blocks/dragon_egg.json}.
	 *
	 * <p>Not crafted, so nothing marks the world as having made one and the craft gate never comes
	 * into it. One per world is the End's doing instead: only a world's first dragon leaves an egg,
	 * and re-summoning it never leaves another.
	 *
	 * <p>It grants hearts rather than an effect, which is what declaring {@link
	 * LegendarySetting#HEARTS} means here — see {@link CarriedHearts}.
	 */
	DRAGON_EGG("legendaries_dragon_egg", Items.DRAGON_EGG, "The Dragon Egg", "dragon_egg",
			new LegendarySource.FromBlockDrop(Blocks.DRAGON_EGG), null, 0, null,
			LegendarySetting.HEARTS),

	/**
	 * Crafted from a netherite pickaxe, two Efficiency V books, and ore that only Silk Touch can
	 * lift — which is what the pickaxe then carries, so the one that makes it could have mined its
	 * own ingredients.
	 *
	 * <p>Vanilla pickaxes are untouched: this has a recipe of its own rather than overriding
	 * theirs, so every ordinary pickaxe is still craftable and this is the one that is not ordinary.
	 *
	 * <p>The recipe file cannot ask which enchantment is on a book, so the Efficiency V part rides
	 * on the source as a {@link CraftRequirement}.
	 */
	PICKAXE("legendaries_pickaxe", Items.NETHERITE_PICKAXE, "The Legendary Pickaxe",
			"legendary_pickaxe", new LegendarySource.FromRecipe("legendaries:legendary_pickaxe",
					new CraftRequirement.EveryBookHas(Enchantments.EFFICIENCY, 5,
							"Both books must be Efficiency V.")),
			MobEffects.FIRE_RESISTANCE, 0, null),

	/**
	 * Crafted from a netherite sword and what the far ends of the game hold — Sharpness VIII where
	 * the spear carries the vanilla ceiling of V.
	 *
	 * <p>Vanilla swords are untouched: this has a recipe of its own rather than replacing theirs, so
	 * an ordinary netherite sword is still craftable and this is the one that is not ordinary.
	 *
	 * <p>The Sharpness V on the book is the part the recipe file cannot ask for, so it rides on the
	 * source as a {@link CraftRequirement} — the same shape as the pickaxe's books.
	 */
	SWORD("legendaries_sword", Items.NETHERITE_SWORD, "The Legendary Sword",
			"legendary_sword", new LegendarySource.FromRecipe("legendaries:legendary_sword",
					new CraftRequirement.EveryBookHas(Enchantments.SHARPNESS, 5,
							"The book must be Sharpness V.")),
			MobEffects.SPEED, 0, null),

	/**
	 * The sword's recipe with an axe at its heart, and the same reach past the enchanting table:
	 * Sharpness VIII on a tool, with Silk Touch and Efficiency V under it.
	 *
	 * <p>Silk Touch on an axe is not idle — it is what takes a bee nest with its bees still in it.
	 *
	 * <p>Efficiency V rather than the pickaxe's VI, so the pickaxe stays the better tool for the
	 * thing a pickaxe is for.
	 */
	AXE("legendaries_axe", Items.NETHERITE_AXE, "The Legendary Axe",
			"legendary_axe", new LegendarySource.FromRecipe("legendaries:legendary_axe",
					new CraftRequirement.EveryBookHas(Enchantments.SHARPNESS, 5,
							"The book must be Sharpness V.")),
			MobEffects.STRENGTH, 0, null);

	private final String marker;
	private final Item item;
	private final String displayName;
	private final String commandName;
	private final LegendarySource source;
	private final Holder<MobEffect> carriedEffect;
	private final int carriedAmplifier;
	private final Ability ability;
	private final Set<LegendarySetting> settings;

	Legendary(String marker, Item item, String displayName, String commandName,
			LegendarySource source, Holder<MobEffect> carriedEffect, int carriedAmplifier,
			Ability ability, LegendarySetting... settings) {
		this.marker = marker;
		this.item = item;
		this.displayName = displayName;
		this.commandName = commandName;
		this.source = source;
		this.carriedEffect = carriedEffect;
		this.carriedAmplifier = carriedAmplifier;
		this.ability = ability;
		EnumSet<LegendarySetting> declared = EnumSet.noneOf(LegendarySetting.class);
		Collections.addAll(declared, settings);
		this.settings = Collections.unmodifiableSet(declared);
	}

	/**
	 * The knobs this legendary itself has, as opposed to the ones its ability has.
	 *
	 * <p>These are the ones for what it grants by being carried, which belongs to the item: two
	 * legendaries granting hearts are two separate bonuses that stack, where two carrying one ability
	 * share a single cooldown.
	 */
	@Override
	public Set<LegendarySetting> settings() {
		return settings;
	}

	/**
	 * The ability this legendary carries, if it carries one.
	 *
	 * <p>A property of the entry rather than a test against a particular constant, so a legendary
	 * that gains an ability declares it in one place with everything else about itself. Several
	 * entries naming the same ability is the supported case, not an accident — see {@link Ability},
	 * which is where the wait and the settings then live.
	 */
	public Optional<Ability> ability() {
		return Optional.ofNullable(ability);
	}

	/**
	 * The name this legendary answers to on the command line: the name the item itself carries, in
	 * lower case.
	 *
	 * <p>Stated per entry rather than derived from {@link #name()}, which is the key a world's saved
	 * settings and the pedestal's slot tags are written under and so cannot follow what a legendary
	 * is called. The two are free to differ: the spear's entry is {@code NETHERITE_SPEAR} and the
	 * item is the Legendary Spear.
	 */
	@Override
	public String commandName() {
		return commandName;
	}

	/**
	 * The condition on the crafting grid that this legendary's recipe means but cannot state.
	 *
	 * <p>Empty for a legendary that is not crafted at all, and for the ordinary case where the
	 * ingredients the file lists are the whole of what it asks for.
	 */
	public Optional<CraftRequirement> craftRequirement() {
		return source instanceof LegendarySource.FromRecipe recipe
				? Optional.ofNullable(recipe.requirement())
				: Optional.empty();
	}

	/** Builds one, by asking whatever data file defines it; see {@link LegendarySource}. */
	public ItemStack create(MinecraftServer server) {
		return source.create(server);
	}

	/** Key inside {@code minecraft:custom_data}. Must match what this legendary's source produces. */
	public String marker() {
		return marker;
	}

	/**
	 * How this legendary is named in the messages it shows a player.
	 *
	 * <p>"The" and then the name the item itself carries, so a player reads the same name in a
	 * message and on the item in their hand.
	 */
	@Override
	public String displayName() {
		return displayName;
	}

	/** The effect carried while this legendary is in a player's inventory, if it grants one. */
	public Optional<Holder<MobEffect>> carriedEffect() {
		return Optional.ofNullable(carriedEffect);
	}

	public int carriedAmplifier() {
		return carriedAmplifier;
	}

	/**
	 * Whether this stack is this legendary.
	 *
	 * <p>An unmarked copy of the same item — one a creative player gave themselves, or one surviving
	 * from before this mod — is deliberately NOT the legendary: it carries no marker, so none of the
	 * rules apply to it.
	 */
	public boolean is(ItemStack stack) {
		if (stack.isEmpty() || !stack.is(item)) {
			return false;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.copyTag().getBooleanOr(marker, false);
	}

	/**
	 * Which legendary is <em>made of</em> this item, if any.
	 *
	 * <p>The one test in the mod that does not read a marker, because it runs where no marker exists
	 * yet: vanilla is about to hand out a plain item, and this decides whether the legendary is what
	 * it should have been handing out. Everything downstream of that answer reads the marker as
	 * usual — see {@link io.dnbg.minecraft.legendaries.mixin.FallingBlockEntityMixin}, which is the
	 * only caller.
	 */
	public static Optional<Legendary> madeOf(Item item) {
		for (Legendary legendary : values()) {
			if (legendary.item == item) {
				return Optional.of(legendary);
			}
		}
		return Optional.empty();
	}

	/** Which legendary this stack is, if any. */
	public static Optional<Legendary> of(ItemStack stack) {
		for (Legendary legendary : values()) {
			if (legendary.is(stack)) {
				return Optional.of(legendary);
			}
		}
		return Optional.empty();
	}

	/** Whether this stack is any legendary — the test every rule in the mod is written against. */
	public static boolean isAny(ItemStack stack) {
		return of(stack).isPresent();
	}

	/** How to name whatever legendary this stack is, for a message aimed at a player. */
	public static String nameOf(ItemStack stack) {
		return of(stack).map(Legendary::displayName).orElse("That");
	}
}
