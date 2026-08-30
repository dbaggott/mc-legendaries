package io.dnbg.minecraft.legendaries.legendary;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * How long an ability makes its owner wait, and the countdown that says so.
 *
 * <p>The wait belongs to the ABILITY, not to the item it was fired from. One ability can be carried
 * by several legendaries at once, and a wait held per item would make a second carrier a way to
 * halve the cooldown rather than another way to use the same one.
 *
 * <p>What is recorded is the tick the ability <em>fired</em> on, and everything else is arithmetic
 * against {@link LegendarySetting#COOLDOWN} as it stands now rather than as it stood then. That is
 * what carries a {@code /legendaries config set} into a wait already running, in both directions and
 * with no re-application step anyone can forget: shortening the setting past the time already
 * elapsed ends that wait outright, lengthening it extends the same wait. The countdown is derived
 * the same way on every pass, so it follows the knob without being told the knob moved.
 *
 * <p><strong>Nothing is drawn on the item.</strong> Vanilla's cooldown swipe is a slot decoration,
 * and the HUD draws no armor slot — so an ability fired from something worn would have nowhere to
 * put one, and a wait shown for one carrier and not another is worse than a wait shown for neither.
 * The countdown goes above the hotbar instead, through {@link Actionbar}, which every carrier can
 * reach because it is a property of the player rather than of a slot. A vanilla client needs
 * nothing installed to read it.
 *
 * <p>One player sees one line, so one wait is shown at a time — in enum order, which is arbitrary
 * but fixed, so a second ability waiting behind the first is hidden rather than flickering against
 * it.
 *
 * <p>Nothing here is saved, and the key is the player OBJECT rather than their UUID, so a relog or a
 * respawn builds a new player and drops the wait with it: reconnecting clears a wait. Weak keys only
 * decide when a dead record is reclaimed — a new player object never finds an old one's, whenever
 * that happens.
 */
public final class AbilityCooldown {
	private static final int TICKS_PER_SECOND = 20;
	private static final int SECONDS_PER_MINUTE = 60;

	/**
	 * The colours the line is written in: the ability's name, then the time left. Nothing punctuates
	 * the two halves, so the change of colour is what separates them.
	 *
	 * <p>The name's has no formatting code behind it, so it travels as an RGB value in the style
	 * rather than as a palette entry — which a vanilla client renders with nothing installed, the
	 * property this whole mod is built on. The time's is a palette entry and stays one:
	 * {@link ChatFormatting#AQUA} is vanilla's colour for {@link
	 * net.minecraft.world.item.Rarity#RARE}, which is what an enchanted item's name is written in.
	 *
	 * <p>The name's is the deep end of the amber a legendary's own name is written in, so the two
	 * read as the same mod talking.
	 */
	private static final int NAME_COLOUR = 0xE96C0C;
	private static final ChatFormatting REMAINING_COLOUR = ChatFormatting.AQUA;

	/**
	 * How often the countdown is re-stated, in ticks.
	 *
	 * <p>A second, because that is the unit it is read in and there is nothing new to say in
	 * between. It also has to be short enough to keep the message out of the client's fade, which
	 * {@link Actionbar} documents; a second is comfortably inside it.
	 *
	 * <p>The sweep itself runs every tick regardless. A wait ends on whichever tick it ends on, and
	 * taking the countdown down on that tick rather than on the next multiple of this is the whole
	 * reason the two cadences are separate.
	 */
	private static final int REFRESH_TICKS = TICKS_PER_SECOND;

	/**
	 * The tick each player's last use of each ability fired on.
	 *
	 * <p>An entry is dropped on the first pass that finds its wait run out — so a player holding no
	 * entry is a player waiting for nothing, and the map cannot grow past the abilities actually in
	 * flight.
	 *
	 * <p>Read and written on the server thread only, which is what makes an unsynchronized map safe.
	 */
	private static final Map<Player, EnumMap<Ability, Integer>> firedAt = new WeakHashMap<>();

	private AbilityCooldown() {
	}

	/** Whether this ability is available to this player right now. */
	public static boolean ready(MinecraftServer server, Player player, Ability ability) {
		EnumMap<Ability, Integer> waits = firedAt.get(player);
		Integer fired = waits == null ? null : waits.get(ability);
		return fired == null || elapsed(server, fired) >= cooldownTicks(server, ability);
	}

	/**
	 * Starts the wait after a use.
	 *
	 * <p>A cooldown of zero needs no case of its own: the record is written, and the next pass finds
	 * nothing left of the wait and drops it again.
	 */
	public static void begin(MinecraftServer server, Player player, Ability ability) {
		firedAt.computeIfAbsent(player, ignored -> new EnumMap<>(Ability.class))
				.put(ability, server.getTickCount());
	}

	/**
	 * Puts every running wait on its owner's screen, and takes it down as each runs out.
	 *
	 * <p>A wait that ends says nothing, and does not linger either: a message the server has stopped
	 * re-sending still has the rest of its time on screen to serve, so the last second read would sit
	 * there over an ability that is already back. Ending it is a positive act, which is what
	 * {@link Actionbar#clear} is for.
	 *
	 * <p>Players who are offline are skipped, and hold no record to be stale by the time they are
	 * back.
	 */
	public static void showWaits(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			EnumMap<Ability, Integer> waits = firedAt.get(player);
			if (waits == null) {
				continue;
			}
			Component showing = null;
			boolean ended = false;
			Iterator<Map.Entry<Ability, Integer>> running = waits.entrySet().iterator();
			while (running.hasNext()) {
				Map.Entry<Ability, Integer> wait = running.next();
				Ability ability = wait.getKey();
				int remaining = cooldownTicks(server, ability) - elapsed(server, wait.getValue());
				if (remaining <= 0) {
					running.remove();
					ended = true;
				} else if (showing == null) {
					showing = Component.literal(ability.displayName() + " ").withColor(NAME_COLOUR)
							.append(Component.literal(remainingText(remaining)).withStyle(REMAINING_COLOUR));
				}
			}
			if (showing == null) {
				if (ended) {
					Actionbar.clear(player);
				}
			} else if (server.getTickCount() % REFRESH_TICKS == 0) {
				Actionbar.hold(player, showing);
			}
		}
	}

	/**
	 * What is left, as a player reads it: bare seconds under a minute, minutes and seconds at or over
	 * one — so a wait passing the minute mark reads {@code 1m 1s}, {@code 1m 0s}, {@code 59s}.
	 *
	 * <p>The seconds are kept at every whole minute rather than dropped, because dropping them is
	 * what would put {@code 1m} between {@code 1m 1s} and {@code 59s} — a step that reads as a jump
	 * back up. A minute is a point the countdown passes through, not a place it arrives at.
	 *
	 * <p>Rounded up, so a wait never reads as over while any of it is left.
	 */
	private static String remainingText(int ticks) {
		int seconds = (ticks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
		if (seconds < SECONDS_PER_MINUTE) {
			return seconds + "s";
		}
		return seconds / SECONDS_PER_MINUTE + "m " + seconds % SECONDS_PER_MINUTE + "s";
	}

	private static int elapsed(MinecraftServer server, int firedTick) {
		return server.getTickCount() - firedTick;
	}

	private static int cooldownTicks(MinecraftServer server, Ability ability) {
		return LegendaryState.get(server).setting(ability, LegendarySetting.COOLDOWN) * TICKS_PER_SECOND;
	}
}
