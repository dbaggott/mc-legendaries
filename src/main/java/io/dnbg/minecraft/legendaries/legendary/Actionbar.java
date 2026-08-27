package io.dnbg.minecraft.legendaries.legendary;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The line of text above the hotbar, and the one thing in this mod that writes to it.
 *
 * <p>A player has exactly one of these, so two writers are one writer and a race: whichever spoke
 * last wins, at whatever cadence it happens to run on. That is why this exists as a door rather than
 * every caller reaching for {@code sendSystemMessage} — a refusal and a countdown would otherwise
 * blank each other, the refusal for a tick and the countdown for its next three seconds.
 *
 * <p>So there are two doors, and they are not equals. {@link #say} has something to announce and
 * takes the line; {@link #hold} is an ambient readout that keeps re-stating something already true,
 * and yields to whatever was said until that has had its time on screen.
 *
 * <p>The client gives a message {@link #SAY_TICKS} and fades it over the last third, which is what
 * shapes both. A {@code say} needs no repeating and holds the line for exactly the time it was
 * given. A {@code hold} has to be re-sent to stay solid at all, and is re-sent often enough to stay
 * out of the fade rather than as often as it is asked — a countdown reading in whole seconds has
 * nothing new to say in between.
 */
public final class Actionbar {
	/**
	 * What {@code Hud.setOverlayMessage} gives a message, in ticks, and how long a {@link #say}
	 * therefore owns the line for.
	 *
	 * <p>A copy of a client-side constant, which is the honest description of it: nothing on the
	 * server can read that value, and a client whose number differs would fade early or late. It is
	 * a display cadence either way, so being wrong about it costs a flicker rather than a rule.
	 */
	private static final int SAY_TICKS = 60;

	/** Read and written on the server thread only, which is what makes an unsynchronized map safe. */
	private static final Map<Player, Integer> spokenUntil = new WeakHashMap<>();

	private Actionbar() {
	}

	/**
	 * Says something, and takes the line for as long as it will be on screen.
	 *
	 * <p>Silently does nothing for a player who is not on a server — the callers are shared rules
	 * that run on both sides, and a client has nobody to tell.
	 */
	public static void say(Player player, Component message) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		spokenUntil.put(serverPlayer, tickCount(serverPlayer) + SAY_TICKS);
		send(serverPlayer, message);
	}

	/**
	 * States something ambient, unless something with more to say is still on screen.
	 *
	 * <p>Dropping the message rather than queueing it is the point: the next call is moments away
	 * and carries a fresher version of the same fact, so a held message that waited its turn would
	 * arrive stale.
	 */
	public static void hold(ServerPlayer player, Component message) {
		Integer until = spokenUntil.get(player);
		if (until != null && tickCount(player) < until) {
			return;
		}
		send(player, message);
	}

	/**
	 * Takes an ambient line down, for when what it was stating has stopped being true.
	 *
	 * <p>An empty message rather than a shorter life: how long a message stays is the client's to
	 * decide and nothing on the server can shorten it, so replacing one is the only way to end it
	 * early. Yields to a {@link #say} for the same reason {@link #hold} does.
	 */
	public static void clear(ServerPlayer player) {
		hold(player, Component.empty());
	}

	private static void send(ServerPlayer player, Component message) {
		player.sendSystemMessage(message, true);
	}

	private static int tickCount(ServerPlayer player) {
		return player.level().getServer().getTickCount();
	}
}
