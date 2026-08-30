package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.legendary.Actionbar;
import io.dnbg.minecraft.legendaries.legendary.Legendary;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A legendary is carried, never set down as a block.
 *
 * <p>{@code place} is where a block item stops being an item, and it is the single funnel: a plain
 * right-click on the ground and a sneaking one over a block that would otherwise have opened both
 * arrive here, and so does anything else that asks a block item to become a block.
 *
 * <p>Refusing on {@code UseBlockCallback} instead would have to decide whether a click was a
 * placement before vanilla had decided, and a legendary in the hand would stop the player opening
 * the chest they were pointing at.
 *
 * <p>The refusal is the server's alone, and cannot be anything else: the marker lives in
 * {@code minecraft:custom_data}, which is persistent and never sent to a client, so a client holding
 * the egg sees an ordinary block item and predicts the placement. The block appears for the tick it
 * takes the server's answer to arrive, and then goes. That is the price of the marker being
 * invisible to vanilla clients, which is the property the whole mod is built on.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void legendaries$refusePlacement(BlockPlaceContext context,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (!Legendary.isAny(context.getItemInHand())) {
			return;
		}
		Player player = context.getPlayer();
		if (player != null) {
			Actionbar.say(player, Component.literal(
					Legendary.nameOf(context.getItemInHand()) + " cannot be placed."));
		}
		cir.setReturnValue(InteractionResult.FAIL);
	}
}
