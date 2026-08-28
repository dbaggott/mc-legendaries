package io.dnbg.minecraft.legendaries.legendary;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Every ability the mod knows about — and the thing a wait and a set of knobs belong to, rather than
 * whichever item you happened to trigger it from.
 *
 * <p>An ability and its carrier are deliberately separate, because one ability can be carried by
 * more than one legendary at a time. When it is, all of them share the one wait and the one set of
 * settings: firing the Molten Blast from a mace has to leave anything else carrying it waiting too,
 * or wearing a second carrier is simply a way to halve the cooldown.
 *
 * <p>The naming only runs one way — {@link Legendary} names the ability it carries, and nothing here
 * names its carriers. A table pointing both ways is a table that can disagree with itself.
 *
 * <p>What each one <em>does</em> hangs off the entry rather than off a {@code switch} somewhere
 * else, so an ability added without an implementation does not compile. Where that implementation
 * lives is its own business: this holds a reference to it, not the code.
 */
public enum Ability implements Tunable {
	MOLTEN_BLAST("Molten Blast", MoltenBlast::fire, LegendarySetting.COOLDOWN, LegendarySetting.RADIUS,
			LegendarySetting.UNMELTED, LegendarySetting.KNOCKBACK);

	private final String displayName;
	private final BiConsumer<ServerLevel, Player> fire;
	private final Set<LegendarySetting> settings;

	Ability(String displayName, BiConsumer<ServerLevel, Player> fire, LegendarySetting... settings) {
		this.displayName = displayName;
		this.fire = fire;
		EnumSet<LegendarySetting> declared = EnumSet.noneOf(LegendarySetting.class);
		Collections.addAll(declared, settings);
		this.settings = Collections.unmodifiableSet(declared);
	}

	/**
	 * The knobs this ability has.
	 *
	 * <p>Every carrier of it turns the same ones, which is the point of the settings belonging to the
	 * ability: two legendaries carrying one blast tune together or they are two blasts.
	 */
	@Override
	public Set<LegendarySetting> settings() {
		return settings;
	}

	/**
	 * Sets this ability off, centred on the player who triggered it.
	 *
	 * <p>Says nothing about whether they were allowed to — {@link AbilityCooldown} is the gate, and
	 * this runs once it has opened.
	 */
	public void fire(ServerLevel level, Player player) {
		this.fire.accept(level, player);
	}

	/** How this ability is named in the messages it shows a player. */
	@Override
	public String displayName() {
		return displayName;
	}

	/** The lowercase name this ability answers to on the command line. */
	@Override
	public String commandName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
