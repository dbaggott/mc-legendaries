package io.dnbg.minecraft.legendaries.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches the private synched-data key for a block display's block.
 *
 * <p>{@link Display.BlockDisplay} has no public setter — {@code blockRenderState()} returns an
 * immutable record — so the only way to tell one which block to render is through its synched
 * data, and the key for that is a private static field.
 *
 * <p>The field name is unchanged across every Minecraft version this mod supports (26.1 through
 * the 26.3 snapshot). If a future version renames it, mixin's {@code "required": true} turns that
 * into a crash at load rather than a display that silently renders nothing.
 */
@Mixin(Display.BlockDisplay.class)
public interface BlockDisplayAccessor {
	@Accessor("DATA_BLOCK_STATE_ID")
	static EntityDataAccessor<BlockState> blockStateId() {
		throw new AssertionError("mixin accessor not applied");
	}
}
