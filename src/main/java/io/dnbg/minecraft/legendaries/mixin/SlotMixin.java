package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.spear.NetheriteSpear;
import io.dnbg.minecraft.legendaries.spear.SpearRules;
import io.dnbg.minecraft.legendaries.spear.SpearState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Refuses the spear into any slot that is not the player's own.
 *
 * <p>This is the whole of the container rule for anything a player does through a screen. Every
 * vanilla storage screen — chest, trapped chest, barrel, ender chest, shulker box, hopper, dropper,
 * dispenser — builds plain {@link Slot}s over a non-player container, so one check covers them all.
 * The furnace family and the chiseled bookshelf override {@code mayPlace} with their own rules and
 * never reach this, but those already refuse a spear on their own terms.
 *
 * <p>The refusal is silent here by design: {@code mayPlace} is called for hover and layout, many
 * times a second, so a message would fire constantly rather than on an actual attempt.
 *
 * <p>Automation does NOT come through here — {@code mayPlace} is screen-only. See
 * {@link HopperBlockEntityMixin}.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {
	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void legendaries$refuseSpearOutsidePlayerInventory(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (!NetheriteSpear.is(stack)) {
			return;
		}
		Object container = ((Slot) (Object) this).container;
		// A crafting grid and its result are the player's own working space, not storage — the
		// spear has to be able to come OUT of a result slot, and quick-move has to be able to put
		// it back somewhere sane.
		// TransientCraftingContainer, not the CraftingContainer interface: a crafter block also
		// implements it, and a crafter is redstone-facing storage — exactly what this rule and the
		// hopper mixin exist to keep the spear out of.
		if (container instanceof Inventory || container instanceof TransientCraftingContainer
				|| container instanceof ResultContainer) {
			return;
		}
		cir.setReturnValue(false);
	}

	/**
	 * Refuses a second spear.
	 *
	 * <p>The gate is on taking the result rather than on the recipe matching: the recipe is plain
	 * data-driven {@code crafting_shaped} JSON, which cannot consult world state, and giving it a
	 * custom recipe type to do so would mean reimplementing {@code ShapedRecipe}'s serializer for
	 * one boolean. The visible cost is that the result still previews in the slot before it is
	 * refused, which is why the refusal says why.
	 */
	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void legendaries$refuseSecondSpear(Player player, CallbackInfoReturnable<Boolean> cir) {
		Slot self = (Slot) (Object) this;
		if (!(self instanceof ResultSlot) || player.level().isClientSide()) {
			return;
		}
		if (!NetheriteSpear.is(self.getItem())) {
			return;
		}
		MinecraftServer server = player.level().getServer();
		if (server == null || !SpearState.get(server).crafted()) {
			return;
		}
		SpearRules.refuse(player, "The Netherite Spear has already been crafted.");
		cir.setReturnValue(false);
	}
}
