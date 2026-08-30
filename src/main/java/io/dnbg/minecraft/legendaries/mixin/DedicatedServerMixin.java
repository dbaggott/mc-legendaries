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
 * <p>The dedicated server only, deliberately. {@link net.minecraft.server.MinecraftServer} has a
 * concrete {@code getServerResourcePack} that returns empty and an integrated server inherits it, so
 * hooking the base class is what reaches single-player — and vanilla never answers it non-empty
 * anywhere but here. Answering it for the host puts the client's resource reload inside its own
 * in-process join handshake, and the join does not finish: the world sits on "Loading Terrain" with
 * nothing crashed and both threads healthy. Single-player installs the pack by hand instead; the
 * README says so.
 *
 * <p>This costs a guest joining an open-to-LAN world their offer too, since that world is an
 * integrated server. Nothing was broken for them — they arrive over a real connection rather than an
 * in-process one — so the narrower fix is to keep the base hook and gate it on the connection, which
 * this method cannot see: it takes no connection and answers once for the whole server.
 */
@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin {
	@Inject(method = "getServerResourcePack", at = @At("RETURN"), cancellable = true)
	private void legendaries$offerResourcePack(CallbackInfoReturnable<Optional<ServerResourcePackInfo>> cir) {
		cir.setReturnValue(ResourcePackOffer.resolve(cir.getReturnValue()));
	}
}
