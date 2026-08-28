package io.dnbg.minecraft.legendaries.legendary;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * A condition on the crafting grid that the recipe file cannot state.
 *
 * <p>A vanilla ingredient is a set of item types and nothing else — {@code Ingredient} is a
 * {@code HolderSet<Item>} whose only question is {@code acceptsItem}. So a recipe can ask for an
 * enchanted book and cannot ask what is written in it. Where a legendary's recipe means the second
 * thing, it says so here and the grid is checked separately.
 *
 * <p>Checked when the result is <em>taken</em>, beside the one-per-world gate in {@code SlotMixin},
 * rather than by refusing the match. Both refuse after the result has already previewed, so that is
 * not what separates them — what does is that a recipe which simply fails to match leaves a player
 * holding nine correct items and no idea which one is wrong, and this can say.
 */
public interface CraftRequirement {
	/** Whether this grid satisfies the condition. Only reached once the recipe itself has matched. */
	boolean satisfiedBy(List<ItemStack> grid);

	/** What a player whose grid does not satisfy it is told. */
	String unmet();

	/**
	 * Every enchanted book in the grid carries at least this level of one enchantment.
	 *
	 * <p>Every book rather than a count of them: a grid only reaches here having already matched the
	 * recipe, so the pattern has settled how many books there are and which slots they are in.
	 *
	 * <p>Read out of {@code stored_enchantments} rather than {@code enchantments}, because that is
	 * where a book keeps what it will confer — the enchantments actually on a book are what a book
	 * does as an item, which is nothing.
	 */
	record EveryBookHas(ResourceKey<Enchantment> enchantment, int level, String unmet)
			implements CraftRequirement {
		@Override
		public boolean satisfiedBy(List<ItemStack> grid) {
			for (ItemStack stack : grid) {
				if (stack.is(Items.ENCHANTED_BOOK) && storedLevel(stack) < level) {
					return false;
				}
			}
			return true;
		}

		private int storedLevel(ItemStack book) {
			ItemEnchantments stored = book.get(DataComponents.STORED_ENCHANTMENTS);
			if (stored == null) {
				return 0;
			}
			for (Object2IntMap.Entry<Holder<Enchantment>> written : stored.entrySet()) {
				if (written.getKey().is(enchantment)) {
					return written.getIntValue();
				}
			}
			return 0;
		}
	}
}
