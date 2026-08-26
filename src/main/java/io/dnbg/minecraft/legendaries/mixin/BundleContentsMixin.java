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
 * {@code overrideStackedOnOther} / {@code overrideOtherStackedOnMe}, so every other container rule
 * in this mod looks straight past it. And the hole is not merely "the spear is in a bundle" — a
 * bundle is an ordinary item, so it goes into a chest, and the spear goes with it.
 *
 * <p><strong>The refusal has to happen here, and not at {@code tryInsert}.</strong> The two are not
 * interchangeable: {@code Mutable.tryTransfer} evaluates {@code slot.safeTake(...)} as the argument
 * to {@code tryInsert}, so the stack has already left the slot by the time {@code tryInsert} sees
 * it. Refusing there returns nothing and hands nothing back — the spear is destroyed outright, with
 * no item entity for the loss backstop to catch. Vanilla never notices, because {@code tryInsert}
 * cannot legitimately refuse a stack {@code tryTransfer} has already sized for it.
 *
 * <p>{@code canItemBeInBundle} is consulted by both callers <em>before</em> anything is removed —
 * {@code tryTransfer} checks it and returns without reaching {@code safeTake}, and it is the first
 * thing {@code tryInsert} does. One refusal, every route, nothing taken out of a slot first.
 */
@Mixin(BundleContents.class)
public abstract class BundleContentsMixin {
	@Inject(method = "canItemBeInBundle", at = @At("HEAD"), cancellable = true)
	private static void legendaries$refuseTheSpear(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (NetheriteSpear.is(stack)) {
			cir.setReturnValue(false);
		}
	}
}
