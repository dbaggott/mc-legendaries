package io.dnbg.minecraft.legendaries.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the private synched-data key for a text display's contents.
 *
 * <p>Used only to label the plinth variants {@code /legendaries debug plinths} puts in the world.
 * Comparing shapes is only useful if you can say which one you liked, and counting unlabelled
 * plinths left to right in a screenshot is how that goes wrong.
 */
@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
	@Accessor("DATA_TEXT_ID")
	static EntityDataAccessor<Component> textId() {
		throw new AssertionError("mixin accessor not applied");
	}
}
