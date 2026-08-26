package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.legendary.Legendary;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Closes the automation route into a container.
 *
 * <p>{@link SlotMixin} only covers what a player does through a screen. A hopper moves items
 * without one, so without this the container rule is bypassable with redstone: a hopper under the
 * spear picks it up and files it into a chest.
 *
 * <p>Both static {@code addItem} overloads are the funnel — one for sucking an item entity off the
 * ground, one for moving a stack between containers. Droppers and dispensers need no handling of
 * their own: nothing can get the spear into one to begin with.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
	@Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z",
			at = @At("HEAD"), cancellable = true)
	private static void legendaries$refuseSpearFromGround(Container container, ItemEntity itemEntity,
			CallbackInfoReturnable<Boolean> cir) {
		if (Legendary.isAny(itemEntity.getItem())) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;"
			+ "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)"
			+ "Lnet/minecraft/world/item/ItemStack;",
			at = @At("HEAD"), cancellable = true)
	private static void legendaries$refuseSpearBetweenContainers(Container source, Container destination,
			ItemStack stack, Direction direction, CallbackInfoReturnable<ItemStack> cir) {
		if (Legendary.isAny(stack)) {
			// Returning the stack unchanged is how this method says "nothing moved".
			cir.setReturnValue(stack);
		}
	}
}
