package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.spear.NetheriteSpear;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A bundle is a container that travels, and it is the one that would have undone the rest.
 *
 * <p>Bundles never touch {@code Slot.mayPlace}: insertion runs through the bundle item's
 * {@code overrideStackedOnOther} / {@code overrideOtherStackedOnMe}, so every container rule
 * elsewhere in this mod looks straight past it. And the hole is not merely "the spear is in a
 * bundle" — the bundle is an ordinary item, so it goes into a chest, and with it the spear.
 *
 * <p>{@code tryInsert} is the single funnel: {@code tryTransfer} routes through it, so refusing
 * here covers every way a stack reaches a bundle. Returning zero is how this method says it
 * accepted nothing.
 */
@Mixin(BundleContents.Mutable.class)
public abstract class BundleContentsMutableMixin {
	@Inject(method = "tryInsert", at = @At("HEAD"), cancellable = true)
	private void legendaries$refuseTheSpear(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		if (NetheriteSpear.is(stack)) {
			cir.setReturnValue(0);
		}
	}
}
