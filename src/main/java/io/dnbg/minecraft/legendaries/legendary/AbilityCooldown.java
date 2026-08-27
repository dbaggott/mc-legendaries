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
	 * The colour the time itself is drawn in — vanilla's own for {@link
	 * net.minecraft.world.item.Rarity#RARE}, which is what an enchanted item's name is written in.
	 *
	 * <p>Only the time carries it. The ability's name is a label that never changes and the time is
	 * the part worth looking at, so colouring the whole line would spend the distinction on nothing.
	 */
	private static final ChatFormatting REMAINING_COLOUR = ChatFormatting.AQUA;

	/**
	 * How often the countdown is re-stated, in ticks.
	 *
	 * <p>A second, because that is the unit it is read in and there is nothing new to say in
	 * between. It also has to be short enough to keep the message out of the client's fade, which
	 * {@link Actionbar} documents; a second is comfortably inside it.
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
	 * Puts every running wait on its owner's screen, and forgets the ones that have run out.
	 *
	 * <p>A wait that ends says nothing: the countdown reaching its last second and going is the whole
	 * of it. Players who are offline are skipped, and hold no record to be stale by the time they are
	 * back.
	 */
	public static void showWaits(MinecraftServer server) {
		if (server.getTickCount() % REFRESH_TICKS != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			EnumMap<Ability, Integer> waits = firedAt.get(player);
			if (waits == null) {
				continue;
			}
			Component showing = null;
			Iterator<Map.Entry<Ability, Integer>> running = waits.entrySet().iterator();
			while (running.hasNext()) {
				Map.Entry<Ability, Integer> wait = running.next();
				Ability ability = wait.getKey();
				int remaining = cooldownTicks(server, ability) - elapsed(server, wait.getValue());
				if (remaining <= 0) {
					running.remove();
				} else if (showing == null) {
					showing = Component.literal(ability.displayName() + " — ")
							.append(Component.literal(remainingText(remaining)).withStyle(REMAINING_COLOUR));
				}
			}
			if (showing != null) {
				Actionbar.hold(player, showing);
			}
		}
	}

	/**
	 * What is left, as a player reads it: seconds up to a minute, and minutes and seconds past one.
	 *
	 * <p>Rounded up, so a wait never reads as over while any of it is left — which also means the
	 * minute mark reads as {@code 1m} rather than spending its first second reading {@code 60s}.
	 */
	private static String remainingText(int ticks) {
		int seconds = (ticks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
		if (seconds <= SECONDS_PER_MINUTE) {
			return seconds + "s";
		}
		int rest = seconds % SECONDS_PER_MINUTE;
		return rest == 0 ? seconds / SECONDS_PER_MINUTE + "m"
				: seconds / SECONDS_PER_MINUTE + "m " + rest + "s";
	}

	private static int elapsed(MinecraftServer server, int firedTick) {
		return server.getTickCount() - firedTick;
	}

	private static int cooldownTicks(MinecraftServer server, Ability ability) {
		return LegendaryState.get(server).setting(ability, LegendarySetting.COOLDOWN) * TICKS_PER_SECOND;
	}
}
