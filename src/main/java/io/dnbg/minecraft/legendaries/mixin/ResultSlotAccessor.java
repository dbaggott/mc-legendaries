package io.dnbg.minecraft.legendaries.mixin;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the grid a result slot was filled from.
 *
 * <p>A {@link ResultSlot} holds its {@code craftSlots} privately and exposes no way to ask what made
 * the item in it. {@link SlotMixin} needs that to check a {@link
 * io.dnbg.minecraft.legendaries.legendary.CraftRequirement}, which is a condition on the
 * ingredients rather than on the result.
 *
 * <p>See {@link BlockDisplayAccessor} for why a private-field accessor is acceptable here and how it
 * fails if a future version moves the field.
 */
@Mixin(ResultSlot.class)
public interface ResultSlotAccessor {
	@Accessor("craftSlots")
	CraftingContainer legendaries$craftSlots();
}
