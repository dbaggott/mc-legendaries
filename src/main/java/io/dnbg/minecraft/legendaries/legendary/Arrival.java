package io.dnbg.minecraft.legendaries.legendary;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;

/**
 * Tells the world, once, that a legendary is in somebody's hands.
 *
 * <p>Once <em>ever</em>, per world, rather than once per player: the line marks the legendary
 * arriving, not a player acquiring it. Whoever it passes to afterwards, and however many times it
 * comes off the pedestal, it is the same item and the world has already heard about it.
 *
 * <p>Said across the screen rather than in chat, where a line scrolls away behind whatever is said
 * next — this is the one time a world hears about a legendary, so it is put where nobody has to have
 * been reading to catch it.
 *
 * <p>The verb is the legendary's own, from {@link LegendarySource#arrivalVerb()}: the Dragon Egg is
 * dug out of a block and is obtained, and the five with recipes are crafted things however the copy
 * in front of you reached its holder.
 *
 * <p>Called from the two places that know who to credit — the craft, where the player taking the
 * result is the one who made it, and {@link LegendaryRules}' sweep, which is what sees a legendary
 * that no craft produced.
 */
public final class Arrival {
	/**
	 * The ends of the ramp a legendary's name is written in, which each recipe's {@code item_name}
	 * spells out a character at a time.
	 */
	private static final int NAME_FIRST = 0xE96C0C;
	private static final int NAME_LAST = 0xF8CB4F;

	/**
	 * Vanilla's own title timings, sent rather than left to the client. A client keeps whatever
	 * timings it was last given, so a world that has run {@code /title times} would otherwise decide
	 * how long a line nobody gets a second showing of stays up. They are vanilla's values so that
	 * sending them cannot leave a client anywhere vanilla would not.
	 */
	private static final int FADE_IN_TICKS = 10;
	private static final int STAY_TICKS = 70;
	private static final int FADE_OUT_TICKS = 20;

	private Arrival() {
	}

	/**
	 * Announces this legendary if the world has not heard about it yet.
	 *
	 * <p>Marking and announcing are one step, so two callers reaching this on the same tick cannot
	 * both be the first.
	 */
	public static void announce(MinecraftServer server, Legendary legendary, Player holder) {
		LegendaryState state = LegendaryState.get(server);
		if (!state.markAnnounced(legendary)) {
			return;
		}
		Component name = inNameColours(legendary.displayName());
		Component credit = Component.empty()
				.append(Component.literal("has been " + legendary.arrivalVerb() + " by ")
						.withStyle(ChatFormatting.GRAY))
				.append(holder.getDisplayName());
		PlayerList everyone = server.getPlayerList();
		// Ahead of the text: it is the title packet that fixes how long the line lasts, out of the
		// timings the client is holding when it arrives, so timings sent after it miss this one.
		everyone.broadcastAll(
				new ClientboundSetTitlesAnimationPacket(FADE_IN_TICKS, STAY_TICKS, FADE_OUT_TICKS));
		everyone.broadcastAll(new ClientboundSetSubtitleTextPacket(credit));
		everyone.broadcastAll(new ClientboundSetTitleTextPacket(name));
	}

	/** Writes text in the ramp, a character at a time, the way a recipe writes a legendary's name. */
	private static Component inNameColours(String text) {
		MutableComponent coloured = Component.empty();
		// Never zero: a one-character name has no span, and takes the colour at the start on its own.
		int span = Math.max(text.length() - 1, 1);
		for (int i = 0; i < text.length(); i++) {
			coloured.append(Component.literal(String.valueOf(text.charAt(i)))
					.withColor(blend(NAME_FIRST, NAME_LAST, (double) i / span)));
		}
		return coloured;
	}

	private static int blend(int from, int to, double along) {
		return channel(ARGB.red(from), ARGB.red(to), along) << 16
				| channel(ARGB.green(from), ARGB.green(to), along) << 8
				| channel(ARGB.blue(from), ARGB.blue(to), along);
	}

	/**
	 * One channel of the ramp, to the half-to-even rounding the colours in the data were written
	 * with. {@code Math.round} and {@code ARGB.srgbLerp} both differ from it by one, on the character
	 * of an odd-length name that lands exactly halfway.
	 */
	private static int channel(int from, int to, double along) {
		return (int) Math.rint(from + (to - from) * along);
	}
}
