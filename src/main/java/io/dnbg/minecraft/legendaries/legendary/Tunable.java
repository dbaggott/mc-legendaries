package io.dnbg.minecraft.legendaries.legendary;

import java.util.Set;

/**
 * Something {@code /legendaries config} can turn a knob on.
 *
 * <p>Knobs do not all belong to the same kind of thing. An {@link Ability}'s belong to the ability
 * rather than to whichever legendary fired it, because one ability can be carried by several at once
 * and they have to tune together. A {@link Legendary}'s belong to the item, because what it grants
 * merely by being carried has no ability to hang off.
 *
 * <p>They share one command argument because somebody configuring something names the thing rather
 * than its category. That puts both sets of names in one namespace, and their saved settings in one
 * map: no ability may answer to the same {@link #commandName()} as a legendary. {@link
 * LegendaryCommand} is where that is enforced, at load.
 *
 * <p>{@link #settings()} is what keeps the argument honest. Only a subject with knobs is suggested
 * or accepted, so "that has nothing to configure" is never an answer the command has to give — and
 * {@code config get} lists the knobs its subject actually has rather than every knob the mod knows
 * about.
 */
public interface Tunable {
	/**
	 * The key this one's settings are saved under.
	 *
	 * <p>{@link Enum#name()} on both implementations, so a world's saved settings survive an entry
	 * being reordered or removed.
	 */
	String name();

	/** The lowercase name this answers to on the command line. */
	String commandName();

	/** How this is named in the messages it shows a player. */
	String displayName();

	/** The knobs this one has. Empty means it is not configurable, and {@code config} will not see it. */
	Set<LegendarySetting> settings();
}
