package io.dnbg.minecraft.legendaries.legendary;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * The data file that says what a legendary is, and how to ask it.
 *
 * <p>Deliberately not components named in code. A legendary's marker, its unbreakability, the
 * spear's enchantments and the egg's shimmer are all declared in data, and this only reads them —
 * so there is no second copy here to drift from the file the game itself is obeying.
 *
 * <p>Which file it is depends on how the legendary comes into the world, and each source builds one
 * by performing the act that produces it. A crafted legendary is defined by its recipe, and asking
 * the recipe what it makes is what a crafting table does. One that is dug up is defined by the loot
 * table of the block it comes out of, and rolling that table is what breaking the block does. Either
 * way the mod ends up holding what the game would have handed a player.
 */
public sealed interface LegendarySource {
	/**
	 * Builds one.
	 *
	 * <p>Empty if the definition is gone, which means a datapack removed or overrode it. Callers say
	 * so rather than substituting a bare item that would look like a legendary and obey none of the
	 * rules.
	 */
	ItemStack create(MinecraftServer server);

	/**
	 * A legendary that is crafted, defined by the result of its own recipe.
	 *
	 * <p>{@code assemble} ignores its input for a fixed result, so an empty crafting grid is enough
	 * to ask the recipe what it makes.
	 *
	 * <p>A {@link CraftRequirement} rides here rather than on {@link Legendary} because only a
	 * crafted legendary can have one — there is no grid to put a condition on when a legendary is dug
	 * out of a block.
	 */
	record FromRecipe(String recipeId, CraftRequirement requirement) implements LegendarySource {
		/** A recipe whose ingredients the file states in full, which is most of them. */
		public FromRecipe(String recipeId) {
			this(recipeId, null);
		}

		@Override
		public ItemStack create(MinecraftServer server) {
			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.parse(recipeId));
			return server.getRecipeManager().byKey(key)
					.map(RecipeHolder::value)
					.filter(CraftingRecipe.class::isInstance)
					.map(recipe -> ((CraftingRecipe) recipe).assemble(CraftingInput.EMPTY))
					.orElse(ItemStack.EMPTY);
		}
	}

	/**
	 * A legendary that is dug out of a block, defined by that block's drop.
	 *
	 * <p>The table is the block's own rather than one named here, so overriding vanilla's file is all
	 * it takes to make what the world hands out and what this hands back the same item — and a
	 * datapack that repoints the block at a different table is followed rather than contradicted.
	 *
	 * <p>The roll is synthetic: no block is being broken and nobody is holding a tool. Position and
	 * tool are supplied because the block parameter set requires them, not because the table this is
	 * asked for reads either.
	 */
	record FromBlockDrop(Block block) implements LegendarySource {
		@Override
		public ItemStack create(MinecraftServer server) {
			ResourceKey<LootTable> table = block.getLootTable().orElse(null);
			if (table == null) {
				return ItemStack.EMPTY;
			}
			ServerLevel level = LegendaryState.home(server);
			LootParams params = new LootParams.Builder(level)
					.withParameter(LootContextParams.BLOCK_STATE, block.defaultBlockState())
					.withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
					.withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
					.create(LootContextParamSets.BLOCK);
			List<ItemStack> dropped = server.reloadableRegistries().getLootTable(table).getRandomItems(params);
			return dropped.isEmpty() ? ItemStack.EMPTY : dropped.getFirst();
		}
	}
}
