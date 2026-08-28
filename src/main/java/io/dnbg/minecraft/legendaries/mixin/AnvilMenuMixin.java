package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.legendary.Legendary;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * An anvil makes nothing out of a legendary.
 *
 * <p>Both of its input slots accept anything — the predicates behind them return true
 * unconditionally — so {@link SlotMixin}'s container rule never sees them. A legendary offered as
 * the sacrifice is consumed, and what comes out is a copy of the other item.
 *
 * <p>Emptied rather than refused, for the reason in {@link GrindstoneMenuMixin}: what matters is
 * that nothing takeable consumes the legendary, not that it cannot be set down in the slot.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
	@Inject(method = "createResult", at = @At("TAIL"))
	private void legendaries$refuseForgingALegendary(CallbackInfo ci) {
		AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
		if (Legendary.isAny(self.getSlot(AnvilMenu.INPUT_SLOT).getItem())
				|| Legendary.isAny(self.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem())) {
			self.getSlot(AnvilMenu.RESULT_SLOT).set(ItemStack.EMPTY);
		}
	}
}
