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
 * The dedicated server's own answer, which is where an operator's {@code resource-pack} line in
 * {@code server.properties} arrives — and where it is left alone. See {@link MinecraftServerMixin}
 * for why both classes are hooked.
 */
@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin {
	@Inject(method = "getServerResourcePack", at = @At("RETURN"), cancellable = true)
	private void legendaries$offerResourcePack(CallbackInfoReturnable<Optional<ServerResourcePackInfo>> cir) {
		cir.setReturnValue(ResourcePackOffer.resolve(cir.getReturnValue()));
	}
}
