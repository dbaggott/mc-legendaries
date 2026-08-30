package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.legendary.Arrival;
import io.dnbg.minecraft.legendaries.legendary.Legendary;
import io.dnbg.minecraft.legendaries.legendary.LegendaryState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records the one craft, and announces it.
 *
 * <p>Taking a legendary out of a crafting result is the only moment it comes into existence, and the
 * only moment it is safe to record — {@code assemble} runs speculatively every time the grid
 * changes, so marking the world there would burn the craft on a preview.
 *
 * <p>It is also the only moment that knows <em>who crafted it</em>. {@link Arrival}'s other caller
 * credits whoever is holding one, which for a craft is a different player as soon as the crafter
 * hands it over — so the announcement is made here rather than left to that sweep.
 *
 * <p>Refusing a SECOND craft is {@link SlotMixin}'s job, on {@code mayPickup}.
 */
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
	@Inject(method = "onTake", at = @At("HEAD"))
	private void legendaries$recordAndAnnounceTheOneCraft(Player player, ItemStack stack, CallbackInfo ci) {
		if (player.level().isClientSide()) {
			return;
		}
		Legendary legendary = Legendary.of(stack).orElse(null);
		if (legendary == null) {
			return;
		}
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		LegendaryState.get(server).markCrafted(legendary);
		Arrival.announce(server, legendary, player);
	}
}
