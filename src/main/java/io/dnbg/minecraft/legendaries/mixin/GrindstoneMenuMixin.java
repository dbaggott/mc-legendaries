package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.legendary.Legendary;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A grindstone makes nothing out of a legendary.
 *
 * <p>Its input slots override {@code mayPlace} with {@code isDamageableItem() ||
 * hasAnyEnchantments()}, so the container rule in {@link SlotMixin} never runs for them and an
 * enchanted legendary is waved straight in. What follows is worse than losing the enchantments:
 * grinding two of the same item merges them, and the result is a copy of the <em>first</em> — so a
 * legendary ground against its ordinary twin is consumed and replaced by an ordinary item.
 *
 * <p>Emptying the result rather than refusing the slot. The legendary can still be put in and taken
 * back out, which is the property that matters — there is nothing to take that would consume it.
 */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {
	@Inject(method = "createResult", at = @At("TAIL"))
	private void legendaries$refuseGrindingALegendary(CallbackInfo ci) {
		AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
		if (Legendary.isAny(self.getSlot(GrindstoneMenu.INPUT_SLOT).getItem())
				|| Legendary.isAny(self.getSlot(GrindstoneMenu.ADDITIONAL_SLOT).getItem())) {
			self.getSlot(GrindstoneMenu.RESULT_SLOT).set(ItemStack.EMPTY);
		}
	}
}
