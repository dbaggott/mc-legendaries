package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.legendary.Legendary;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A legendary is never repaired into an ordinary item.
 *
 * <p>Vanilla's repair recipe combines two of the same item, and what it hands back is a
 * <em>new</em> stack: the components go, so a legendary put in beside its ordinary twin comes out as
 * neither. Both inputs are consumed, the result carries no marker, and nothing downstream can tell
 * what happened — it is not an item entity, so the loss backstop never sees it, and the world still
 * records the legendary as crafted, so it cannot be made again.
 *
 * <p>Being unbreakable is not protection. {@code canCombine} asks only for the same item, single
 * counts, and {@code max_damage} and {@code damage} present on both; the {@code unbreakable}
 * component removes neither, so a legendary satisfies it.
 *
 * <p>Nothing needed this until a legendary shared its item with something still craftable. The spear
 * has no unmarked counterpart because every vanilla spear recipe is gone, and the mace's own recipe
 * is overridden in place — so for both, the second stack this recipe wants cannot exist.
 */
@Mixin(RepairItemRecipe.class)
public abstract class RepairItemRecipeMixin {
	@Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
			at = @At("HEAD"), cancellable = true)
	private void legendaries$refuseRepairingALegendary(CraftingInput input, Level level,
			CallbackInfoReturnable<Boolean> cir) {
		for (ItemStack stack : input.items()) {
			if (Legendary.isAny(stack)) {
				// No result at all rather than a refusal on taking it: a repair grid holding a
				// legendary has nothing to preview that would not be a lie about what it makes.
				cir.setReturnValue(false);
				return;
			}
		}
	}
}
