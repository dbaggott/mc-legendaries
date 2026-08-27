package io.dnbg.minecraft.legendaries.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the private synched-data key choosing which model transform an item display renders with.
 *
 * <p>An item model carries a transform per context — held, in a frame, in the GUI, lying on the
 * ground — and they differ in more than size. Picking {@code GROUND} is what makes the legendary in
 * the case look like the dropped item players already recognise, rather than a held item shrunk
 * until it happens to fit.
 */
@Mixin(Display.ItemDisplay.class)
public interface ItemDisplayAccessor {
	@Accessor("DATA_ITEM_DISPLAY_ID")
	static EntityDataAccessor<Byte> itemDisplayId() {
		throw new AssertionError("mixin accessor not applied");
	}
}
