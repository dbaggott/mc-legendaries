package io.dnbg.minecraft.legendaries.legendary;

import java.util.Locale;

/**
 * The knobs {@code /legendaries config} can turn, and what they mean when nobody has turned them.
 *
 * <p>These exist for testing: retuning a blast means a command and a swing rather than an edit, a
 * rebuild and a relaunch. Adding one is an entry here — the command reads its name, its bounds and
 * its default straight off this table, so nothing else has to know the setting exists.
 */
public enum LegendarySetting {
	/** Seconds between uses of an ability. Zero removes the wait, which is the point while tuning. */
	COOLDOWN(60, 0, 86_400, "seconds"),

	/**
	 * Blocks of radius for an ability's area of effect.
	 *
	 * <p>Cost grows with the cube of this, and every block in range is a {@code setBlock} with
	 * neighbour updates and a client packet, all on one synchronous tick. 16 is a bound on how far a
	 * mistyped digit can go — roughly sixty-seven times the default's volume — rather than a
	 * measured safe ceiling. Nobody has profiled it; treat the top of the range as the interesting
	 * end, not the supported one.
	 */
	RADIUS(4, 0, 16, "blocks"),

	/**
	 * Percent of an ability's shell left as it was found, rather than transformed.
	 *
	 * <p>0 coats the crater in a uniform lining and 100 leaves it as ordinary rock. What the knob is
	 * really tuning is how much of the ground a crater was cut from still shows in its edge.
	 */
	UNMELTED(15, 0, 100, "percent"),

	/**
	 * How hard an ability throws what it catches, as a percent of one stick of TNT's impulse.
	 *
	 * <p>Percent rather than sticks because this is tuned by feel, and whole sticks would leave two
	 * usable settings below the default. 1000 is a bound on how far a mistyped digit can go rather
	 * than a measured ceiling; nobody has looked at what a server makes of an impulse that size.
	 */
	KNOCKBACK(200, 0, 1000, "percent");

	private final int defaultValue;
	private final int min;
	private final int max;
	private final String unit;

	LegendarySetting(int defaultValue, int min, int max, String unit) {
		this.defaultValue = defaultValue;
		this.min = min;
		this.max = max;
		this.unit = unit;
	}

	public int defaultValue() {
		return defaultValue;
	}

	public int min() {
		return min;
	}

	public int max() {
		return max;
	}

	public String unit() {
		return unit;
	}

	/** The lowercase name this setting answers to on the command line. */
	public String commandName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
