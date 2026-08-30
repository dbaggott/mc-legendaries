package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.legendary.ResourcePackOffer;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.MinecraftServer.ServerResourcePackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Offers {@link ResourcePackOffer}'s pack on a server that serves none of its own.
 *
 * <p>This reaches single-player: an integrated server inherits this method rather than overriding
 * it, and the base answer is always empty. A dedicated server replaces the method outright, so its
 * own copy needs {@link DedicatedServerMixin} — the pair is what covers both, not redundancy.
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@Inject(method = "getServerResourcePack", at = @At("RETURN"), cancellable = true)
	private void legendaries$offerResourcePack(CallbackInfoReturnable<Optional<ServerResourcePackInfo>> cir) {
		cir.setReturnValue(ResourcePackOffer.resolve(cir.getReturnValue()));
	}
}
