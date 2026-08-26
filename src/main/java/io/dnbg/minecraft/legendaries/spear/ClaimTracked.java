package io.dnbg.minecraft.legendaries.spear;

/**
 * Implemented onto {@code ItemEntity} by a mixin, so the removal hook can ask an item entity
 * whether a player is what removed it.
 *
 * <p>It exists because one mixin cannot cast to another: {@code EntityMixin} and
 * {@code ItemEntityMixin} target different classes, and mixin resolves such a cast against the
 * target's real hierarchy, where the other mixin does not appear. A shared interface is in that
 * hierarchy, because the mixin puts it there.
 */
public interface ClaimTracked {
	boolean legendaries$wasClaimed();
}
