package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.spear.NetheriteSpear;
import io.dnbg.minecraft.legendaries.spear.SpearState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records the one craft.
 *
 * <p>Taking the spear out of a crafting result is the only moment it comes into existence, and the
 * only moment it is safe to record — {@code assemble} runs speculatively every time the grid
 * changes, so marking the world there would burn the craft on a preview.
 *
 * <p>Refusing a SECOND craft is {@link SlotMixin}'s job, on {@code mayPickup}.
 */
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
	@Inject(method = "onTake", at = @At("HEAD"))
	private void legendaries$recordTheOneCraft(Player player, ItemStack stack, CallbackInfo ci) {
		if (player.level().isClientSide() || !NetheriteSpear.is(stack)) {
			return;
		}
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		SpearState state = SpearState.get(server);
		if (!state.crafted()) {
			state.markCrafted();
		}
	}
}
