package io.dnbg.minecraft.legendaries.legendary;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tells the world, once, that a legendary is in somebody's hands.
 *
 * <p>Once <em>ever</em>, per world, rather than once per player: the line marks the legendary
 * arriving, not a player acquiring it. Whoever it passes to afterwards, and however many times it
 * comes off the pedestal, it is the same item and the world has already heard about it.
 *
 * <p>The verb is read from whether the world has a craft on record rather than from how the
 * legendary is defined. Those differ in the case that matters: the Dragon Egg is never crafted, so
 * it is obtained; and a legendary an operator hands out with {@code item give} was not crafted
 * either, so it does not claim to have been.
 */
public final class Arrival {
	private Arrival() {
	}

	/**
	 * Announces this legendary if the world has not heard about it yet.
	 *
	 * <p>Marking and announcing are one step, so two players holding it on the same tick cannot both
	 * be the first.
	 */
	public static void announce(MinecraftServer server, Legendary legendary, ServerPlayer holder) {
		LegendaryState state = LegendaryState.get(server);
		if (!state.markAnnounced(legendary)) {
			return;
		}
		String verb = state.crafted(legendary) ? "crafted" : "obtained";
		server.getPlayerList().broadcastSystemMessage(
				Component.literal(legendary.displayName() + " has been " + verb + " by ")
						.append(holder.getDisplayName()),
				false);
	}
}
