package io.dnbg.minecraft.legendaries.legendary;

import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * How long a legendary's ability makes its owner wait, measured against
 * {@link LegendarySetting#COOLDOWN} as it stands now rather than as it stood at the moment the
 * ability fired.
 *
 * <p>What is recorded is the tick the ability <em>fired</em> on, and the gate is a comparison
 * against the current setting. That is what carries a {@code /legendaries config set} into a wait
 * already running, in both directions and with no re-application step anyone can forget:
 * shortening the setting past the time already elapsed ends that wait outright, lengthening it
 * extends the same wait. Vanilla's {@code ItemCooldowns} cannot do it — what that stores is the
 * tick a wait ends on, fixed when the wait begins — and a knob whose whole point is retuning a
 * blast from the command line has to reach the blast you are currently waiting for.
 *
 * <p>Vanilla's cooldown is still pushed, because the swipe drawn over the item in the hotbar is
 * what a player actually reads and it is drawn from {@code ItemCooldowns}. It is re-pushed
 * whenever the setting changes so the drawing agrees with the gate; it is never what the gate
 * asks.
 *
 * <p>Nothing here is saved, and the key is the player OBJECT rather than their UUID, so a record
 * lives exactly as long as vanilla's own cooldown does: {@code Player.cooldowns} is final, built
 * with the player and copied by nothing, so a relog or a respawn builds a new player and drops the
 * wait and the swipe that draws it together. Weak keys only decide when a dead record is
 * reclaimed — a new player object never finds an old one's, whenever that happens.
 */
public final class AbilityCooldown {
	private static final int TICKS_PER_SECOND = 20;

	/**
	 * One player's last use of one ability: the server tick it fired on, and the vanilla cooldown
	 * group its swipe was pushed under.
	 *
	 * <p>The group is recorded rather than re-derived because deriving it takes the stack, and by
	 * the time the setting changes the player need not be holding one.
	 */
	private record Use(int tick, Identifier group) {
	}

	/** Read and written on the server thread only, which is what makes an unsynchronized map safe. */
	private static final Map<Player, EnumMap<Legendary, Use>> uses = new WeakHashMap<>();

	private AbilityCooldown() {
	}

	/** Whether this legendary's ability is available to this player right now. */
	public static boolean ready(MinecraftServer server, Player player, Legendary legendary) {
		Use last = lastUse(player, legendary);
		return last == null || elapsed(server, last) >= cooldownTicks(server, legendary);
	}

	/** Starts the wait after a use, and the swipe that shows it. */
	public static void begin(MinecraftServer server, Player player, Legendary legendary, ItemStack stack) {
		Identifier group = player.getCooldowns().getCooldownGroup(stack);
		uses.computeIfAbsent(player, ignored -> new EnumMap<>(Legendary.class))
				.put(legendary, new Use(server.getTickCount(), group));
		player.getCooldowns().addCooldown(group, cooldownTicks(server, legendary));
	}

	/**
	 * Redraws every swipe still standing for this legendary, against a setting that just changed.
	 *
	 * <p>The drawing only — {@link #ready} already reads the new value, so a player this misses
	 * waits exactly as long either way and merely sees a stale swipe until it runs out. Players who
	 * are offline are missed, and hold no record to be stale by the time they are back.
	 */
	public static void settingChanged(MinecraftServer server, Legendary legendary, LegendarySetting setting) {
		if (setting != LegendarySetting.COOLDOWN) {
			return;
		}
		int cooldown = cooldownTicks(server, legendary);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Use last = lastUse(player, legendary);
			if (last == null) {
				continue;
			}
			int remaining = cooldown - elapsed(server, last);
			if (remaining > 0) {
				player.getCooldowns().addCooldown(last.group(), remaining);
			} else {
				player.getCooldowns().removeCooldown(last.group());
			}
		}
	}

	private static Use lastUse(Player player, Legendary legendary) {
		EnumMap<Legendary, Use> byLegendary = uses.get(player);
		return byLegendary == null ? null : byLegendary.get(legendary);
	}

	private static int elapsed(MinecraftServer server, Use use) {
		return server.getTickCount() - use.tick();
	}

	private static int cooldownTicks(MinecraftServer server, Legendary legendary) {
		return LegendaryState.get(server).setting(legendary, LegendarySetting.COOLDOWN) * TICKS_PER_SECOND;
	}
}
