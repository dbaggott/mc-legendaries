package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.legendary.Actionbar;
import io.dnbg.minecraft.legendaries.legendary.CraftRequirement;
import io.dnbg.minecraft.legendaries.legendary.Legendary;
import io.dnbg.minecraft.legendaries.legendary.LegendaryRules;
import io.dnbg.minecraft.legendaries.legendary.LegendaryState;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Refuses a legendary into any slot that is not the player's own.
 *
 * <p>This is the container rule for storage — anything a player puts a legendary <em>away</em> in
 * through a screen. Every vanilla storage screen — chest, trapped chest, barrel, ender chest,
 * shulker box, hopper, dropper, dispenser — builds plain {@link Slot}s over a non-player container,
 * so one check covers them all.
 *
 * <p>It does not reach a screen that overrides {@code mayPlace}, and not all of those refuse a
 * legendary on their own terms. The furnace family and the chiseled bookshelf do. The grindstone and
 * the anvil do not, and they consume what they are given rather than storing it, so they are refused
 * where they decide what to make instead — see {@link GrindstoneMenuMixin} and {@link
 * AnvilMenuMixin}.
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
	private void legendaries$refuseOutsidePlayerInventory(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (!Legendary.isAny(stack)) {
			return;
		}
		if (LegendaryRules.ownWorkingSpace(((Slot) (Object) this).container)) {
			return;
		}
		cir.setReturnValue(false);
	}

	/**
	 * Refuses a second copy of a legendary.
	 *
	 * <p>The gate is on taking the result rather than on the recipe matching: the recipe is plain
	 * data-driven {@code crafting_shaped} JSON, which cannot consult world state, and giving it a
	 * custom recipe type to do so would mean reimplementing {@code ShapedRecipe}'s serializer for
	 * one boolean. The visible cost is that the result still previews in the slot before it is
	 * refused, which is why the refusal says why.
	 */
	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void legendaries$refuseSecondCopy(Player player, CallbackInfoReturnable<Boolean> cir) {
		Slot self = (Slot) (Object) this;
		if (!(self instanceof ResultSlot) || player.level().isClientSide()) {
			return;
		}
		Legendary legendary = Legendary.of(self.getItem()).orElse(null);
		if (legendary == null) {
			return;
		}
		MinecraftServer server = player.level().getServer();
		if (server == null || !LegendaryState.get(server).crafted(legendary)) {
			return;
		}
		Actionbar.say(player, Component.literal(legendary.displayName() + " has already been crafted."));
		cir.setReturnValue(false);
	}

	/**
	 * Refuses a grid that matched the recipe without meeting what the recipe meant.
	 *
	 * <p>A vanilla ingredient can name an item type and nothing else, so a recipe asking for an
	 * enchanted book accepts any book. {@link CraftRequirement} is the rest of the condition, and
	 * this is where it is enforced — on taking the result, beside the one-per-world gate above,
	 * because both are things the recipe itself could not decide.
	 *
	 * <p>Saying why is the point. A player holding nine correct-looking items and a result that
	 * silently refuses has nothing to go on, so the requirement carries its own message.
	 */
	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void legendaries$refuseUnmetCraftRequirement(Player player, CallbackInfoReturnable<Boolean> cir) {
		Slot self = (Slot) (Object) this;
		if (!(self instanceof ResultSlot) || player.level().isClientSide()) {
			return;
		}
		CraftRequirement requirement = Legendary.of(self.getItem())
				.flatMap(Legendary::craftRequirement)
				.orElse(null);
		if (requirement == null) {
			return;
		}
		List<ItemStack> grid = ((ResultSlotAccessor) self).legendaries$craftSlots().getItems();
		if (requirement.satisfiedBy(grid)) {
			return;
		}
		Actionbar.say(player, Component.literal(requirement.unmet()));
		cir.setReturnValue(false);
	}
}
