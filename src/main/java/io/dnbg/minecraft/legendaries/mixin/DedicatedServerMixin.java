package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.legendary.ResourcePackOffer;
import java.util.Optional;
import net.minecraft.server.MinecraftServer.ServerResourcePackInfo;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Offers {@link ResourcePackOffer}'s pack on a dedicated server that serves none of its own — and
 * leaves an operator's {@code resource-pack} line in {@code server.properties} alone where there is
 * one.
 *
 * <p>The dedicated server only, deliberately. {@link net.minecraft.server.MinecraftServer} declares
 * this method and an integrated server inherits it, so hooking the base class would offer the pack
 * in single-player too — and vanilla never does that, because it implements
 * {@code getServerResourcePack} on this class alone. Pushing one from an integrated server puts the
 * client's resource reload inside its own join handshake, and the join never finishes: the world
 * sits on "Loading Terrain" with nothing crashed and both threads healthy. Single-player installs
 * the pack by hand instead; the README says so.
 */
@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin {
	@Inject(method = "getServerResourcePack", at = @At("RETURN"), cancellable = true)
	private void legendaries$offerResourcePack(CallbackInfoReturnable<Optional<ServerResourcePackInfo>> cir) {
		cir.setReturnValue(ResourcePackOffer.resolve(cir.getReturnValue()));
	}
}
