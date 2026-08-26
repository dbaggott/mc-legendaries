package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.spear.ClaimTracked;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records that an item entity is being removed because somebody picked it up.
 *
 * <p>{@link EntityMixin} sees every removal but not the reason for it, and a pickup and a despawn
 * both arrive as {@code DISCARDED}. Without this flag the spear would teleport to its pedestal the
 * instant a player walked over it.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin implements ClaimTracked {
	@Unique
	private boolean legendaries$claimedByPlayer;

	@Inject(method = "playerTouch", at = @At("HEAD"))
	private void legendaries$markClaimed(Player player, CallbackInfo ci) {
		this.legendaries$claimedByPlayer = true;
	}

	@Inject(method = "playerTouch", at = @At("RETURN"))
	private void legendaries$unmarkClaimed(Player player, CallbackInfo ci) {
		// Cleared on the way out: a touch that did NOT consume the stack (a full inventory) leaves
		// the entity alive, and a later despawn must still count as a loss.
		this.legendaries$claimedByPlayer = false;
	}

	@Override
	public boolean legendaries$wasClaimed() {
		return this.legendaries$claimedByPlayer;
	}
}
