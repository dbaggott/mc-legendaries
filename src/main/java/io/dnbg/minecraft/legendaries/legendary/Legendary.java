package io.dnbg.minecraft.legendaries.legendary;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.component.CustomData;

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
 * <p>Markers are written by recipes, not by code: each legendary's recipe declares the marker in its
 * result components. This type only reads them. Adding a legendary is an entry here plus a recipe —
 * every rule in the mod iterates this enum rather than naming an item, so none of them need to
 * change.
 */
public enum Legendary {
	/** Replaces every vanilla spear recipe; see {@code data/legendaries/recipe/netherite_spear.json}. */
	NETHERITE_SPEAR("legendaries_spear", Items.NETHERITE_SPEAR, "The Netherite Spear",
			"legendaries:netherite_spear", MobEffects.SPEED, 1, false),

	/**
	 * Crafted by the vanilla recipe, which is overridden in place to mark its result — the
	 * ingredients and pattern are untouched, so it is still "the mace recipe" to a player.
	 */
	MACE("legendaries_mace", Items.MACE, "The Mace", "minecraft:mace", null, 0, true);

	private final String marker;
	private final Item item;
	private final String displayName;
	private final String recipeId;
	private final Holder<MobEffect> carriedEffect;
	private final int carriedAmplifier;
	private final boolean hasAbility;

	Legendary(String marker, Item item, String displayName, String recipeId,
			Holder<MobEffect> carriedEffect, int carriedAmplifier, boolean hasAbility) {
		this.marker = marker;
		this.item = item;
		this.displayName = displayName;
		this.recipeId = recipeId;
		this.carriedEffect = carriedEffect;
		this.carriedAmplifier = carriedAmplifier;
		this.hasAbility = hasAbility;
	}

	/**
	 * Whether this legendary has an ability whose cooldown and radius mean anything.
	 *
	 * <p>A property of the entry rather than a test against a particular constant, so a legendary
	 * that gains an ability declares it in one place with everything else about itself.
	 */
	public boolean hasAbility() {
		return hasAbility;
	}

	/** The lowercase name this legendary answers to on the command line. */
	public String commandName() {
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * Builds one, by assembling this legendary's own recipe.
	 *
	 * <p>Deliberately not built from components named in code. The recipe file is the single
	 * definition of what a legendary is — its marker, its unbreakability, the spear's enchantments —
	 * and a second copy here would be the one that drifts. {@code assemble} ignores its input for a
	 * fixed result, so an empty crafting grid is enough to ask the recipe what it makes.
	 *
	 * <p>Empty if the recipe is missing, which means a datapack removed or overrode it. Callers say
	 * so rather than substituting a bare item that would look like a legendary and obey none of the
	 * rules.
	 */
	public ItemStack create(MinecraftServer server) {
		ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.parse(recipeId));
		return server.getRecipeManager().byKey(key)
				.map(RecipeHolder::value)
				.filter(CraftingRecipe.class::isInstance)
				.map(recipe -> ((CraftingRecipe) recipe).assemble(CraftingInput.EMPTY))
				.orElse(ItemStack.EMPTY);
	}

	/** Key inside {@code minecraft:custom_data}. Must match this legendary's recipe result. */
	public String marker() {
		return marker;
	}

	/** How this legendary is named in the messages it shows a player. */
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
}
