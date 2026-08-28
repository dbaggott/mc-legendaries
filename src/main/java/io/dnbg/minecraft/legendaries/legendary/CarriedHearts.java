package io.dnbg.minecraft.legendaries.legendary;

import io.dnbg.minecraft.legendaries.Legendaries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The hearts a legendary adds to whoever is carrying it, and takes back the moment they are not.
 *
 * <p>A modifier on {@code max_health} rather than a mob effect. Health Boost only comes in whole
 * levels of two hearts, so five cannot be expressed as one at all — let alone a number somebody can
 * turn. The modifier is rebuilt from the setting on every pass instead, which is what carries a
 * {@code /legendaries config set} onto an egg already in a pocket, with no re-application step
 * anyone can forget.
 *
 * <p>Losing the hearts is vanilla's doing rather than this mod's: dropping a max-health modifier
 * clamps current health down to the new maximum. A player at fifteen hearts who hands the egg over
 * is at ten, and nothing here has to know which hearts were the extra ones.
 *
 * <p>The modifier is transient, so nothing about the bonus is written to the world and this sweep is
 * the only thing that decides: somebody who loses the egg while offline comes back without the
 * hearts. The cost is that reconnecting <em>with</em> it costs the extra health, though not the
 * hearts — a player's saved health is clamped against a maximum the modifier is not back on yet, and
 * it refills.
 */
public final class CarriedHearts {
	/**
	 * One modifier holding the whole bonus, because an id is a modifier's identity: a second
	 * legendary granting hearts has to add to the total rather than replace it, and losing one has to
	 * leave the rest standing.
	 */
	private static final Identifier MODIFIER_ID = Legendaries.id("carried_hearts");

	private static final int HEALTH_PER_HEART = 2;

	private CarriedHearts() {
	}

	/** Puts a player's hearts where whatever they are carrying says they should be. */
	public static void refresh(MinecraftServer server, ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}
		LegendaryState state = LegendaryState.get(server);
		int hearts = 0;
		for (Legendary legendary : Legendary.values()) {
			// Declaring the knob is what makes a legendary one that grants hearts. The bonus and the
			// setting are the same fact, so an entry cannot end up with one and not the other.
			if (legendary.settings().contains(LegendarySetting.HEARTS)
					&& LegendaryRules.carrying(player, legendary)) {
				hearts += state.setting(legendary, LegendarySetting.HEARTS);
			}
		}
		if (hearts <= 0) {
			maxHealth.removeModifier(MODIFIER_ID);
			return;
		}
		maxHealth.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER_ID,
				(double) hearts * HEALTH_PER_HEART, AttributeModifier.Operation.ADD_VALUE));
	}
}
