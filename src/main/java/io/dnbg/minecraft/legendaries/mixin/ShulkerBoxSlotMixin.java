package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.spear.NetheriteSpear;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A shulker box slot decides for itself, so {@link SlotMixin} never sees it.
 *
 * <p>{@code ShulkerBoxSlot.mayPlace} is a complete override — it answers
 * {@code stack.getItem().canFitInsideContainerItems()} and never calls {@code super} — so an
 * injection on {@code Slot.mayPlace} is simply not on the path. That is the gap {@code SlotMixin}'s
 * own comment predicted in the abstract; a shulker box is the one place in vanilla where it is
 * reachable with the spear, because every other overriding slot filters by a type a spear is not.
 */
@Mixin(ShulkerBoxSlot.class)
public abstract class ShulkerBoxSlotMixin {
	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void legendaries$refuseTheSpear(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (NetheriteSpear.is(stack)) {
			cir.setReturnValue(false);
		}
	}
}
