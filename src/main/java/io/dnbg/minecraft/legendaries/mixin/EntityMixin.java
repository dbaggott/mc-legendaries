package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.spear.ClaimTracked;
import io.dnbg.minecraft.legendaries.spear.NetheriteSpear;
import io.dnbg.minecraft.legendaries.spear.Pedestal;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The loss backstop: the spear cannot be destroyed out of the world.
 *
 * <p>{@code setRemoved} is the single funnel every removal passes through, which is why the hook
 * is here rather than on the individual causes — despawning after five minutes on the ground,
 * falling into the void, being blown up, and {@code /kill} all arrive at the same place.
 *
 * <p>What is deliberately NOT caught:
 * <ul>
 *   <li>A pickup, flagged by {@link ItemEntityMixin} — otherwise walking over the spear would
 *       send it home.
 *   <li>{@code UNLOADED_TO_CHUNK} and {@code CHANGED_DIMENSION}, which are bookkeeping. An item in
 *       an unloaded chunk is not lost, it is asleep.
 *   <li>Lava and fire, which never reach here at all: {@code netherite_spear} is registered
 *       {@code .fireResistant()} in vanilla, so it simply does not burn.
 * </ul>
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "setRemoved", at = @At("HEAD"))
	private void legendaries$returnSpearOnLoss(Entity.RemovalReason reason, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (!(self instanceof ItemEntity itemEntity) || self.level().isClientSide()) {
			return;
		}
		if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED) {
			return;
		}
		ItemStack stack = itemEntity.getItem();
		if (!NetheriteSpear.is(stack)) {
			return;
		}
		if (itemEntity instanceof ClaimTracked tracked && tracked.legendaries$wasClaimed()) {
			return;
		}
		MinecraftServer server = self.level().getServer();
		if (server != null) {
			Pedestal.place(server, stack);
		}
	}
}
