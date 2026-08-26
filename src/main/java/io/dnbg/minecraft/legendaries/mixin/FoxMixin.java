package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.legendary.Legendary;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A fox overrides {@code canHoldItem} rather than inheriting it, so {@link MobMixin}'s refusal does
 * not reach it — and its own override says yes to <em>anything</em> while its mouth is empty.
 *
 * <p>Foxes are the likeliest mob to meet a dropped spear: they pick things up on their own
 * initiative, wander, and despawn.
 */
@Mixin(Fox.class)
public abstract class FoxMixin {
	@Inject(method = "canHoldItem", at = @At("HEAD"), cancellable = true)
	private void legendaries$refuseLegendaries(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (Legendary.isAny(stack)) {
			cir.setReturnValue(false);
		}
	}
}
