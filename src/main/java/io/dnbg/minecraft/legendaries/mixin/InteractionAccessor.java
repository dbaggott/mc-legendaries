package io.dnbg.minecraft.legendaries.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Interaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the private synched-data keys for an interaction entity's clickable box.
 *
 * <p>An {@link Interaction} defaults to 0x0, which cannot be clicked at all, and exposes no setter
 * for either dimension. Without these the pedestal would render and be inert.
 *
 * <p>See {@link BlockDisplayAccessor} for why a private-field accessor is acceptable here and how
 * it fails if a future version moves the field.
 */
@Mixin(Interaction.class)
public interface InteractionAccessor {
	@Accessor("DATA_WIDTH_ID")
	static EntityDataAccessor<Float> widthId() {
		throw new AssertionError("mixin accessor not applied");
	}

	@Accessor("DATA_HEIGHT_ID")
	static EntityDataAccessor<Float> heightId() {
		throw new AssertionError("mixin accessor not applied");
	}
}
