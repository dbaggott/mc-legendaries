package io.dnbg.minecraft.legendaries.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the private synched-data keys for a display's scale and offset.
 *
 * <p>A {@link Display} renders its block at full size, centred on the entity, and exposes no setter
 * for either. Without these every tier of the pedestal would be a 1x1x1 cube, which is one block
 * stacked three times rather than a base, a column and a cap.
 *
 * <p>Both field names are unchanged across every Minecraft version this mod supports, checked the
 * same way as {@link BlockDisplayAccessor}'s. A rename becomes a crash at load rather than a
 * pedestal that quietly renders as a stack of cubes.
 */
@Mixin(Display.class)
public interface DisplayTransformAccessor {
	@Accessor("DATA_SCALE_ID")
	static EntityDataAccessor<Vector3fc> scaleId() {
		throw new AssertionError("mixin accessor not applied");
	}

	@Accessor("DATA_TRANSLATION_ID")
	static EntityDataAccessor<Vector3fc> translationId() {
		throw new AssertionError("mixin accessor not applied");
	}

	/** The rotation applied before the scale, so a display spins about its own origin. */
	@Accessor("DATA_LEFT_ROTATION_ID")
	static EntityDataAccessor<Quaternionfc> leftRotationId() {
		throw new AssertionError("mixin accessor not applied");
	}

	/** How many ticks the client takes to reach a newly set transformation. */
	@Accessor("DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID")
	static EntityDataAccessor<Integer> interpolationDurationId() {
		throw new AssertionError("mixin accessor not applied");
	}

	/**
	 * The field that restarts the client's interpolation clock.
	 *
	 * <p>Only this one does. {@code Display.onSyncedDataUpdated} sets {@code updateStartTick} for
	 * this key alone — a changed rotation merely marks the render state dirty — so an animation that
	 * does not resend this never starts a second leg.
	 */
	@Accessor("DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID")
	static EntityDataAccessor<Integer> interpolationDelayId() {
		throw new AssertionError("mixin accessor not applied");
	}
}
