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
	 * <p>Capped well below what the machine would tolerate: cost grows with the cube of this, and a
	 * setting meant for experimenting should not be able to stall a server by mistyping a digit.
	 */
	RADIUS(4, 0, 16, "blocks");

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
