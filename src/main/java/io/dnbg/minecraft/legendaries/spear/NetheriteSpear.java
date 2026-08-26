package io.dnbg.minecraft.legendaries.spear;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * Identity of the one Netherite Spear.
 *
 * <p>The spear is the <em>vanilla</em> {@code minecraft:netherite_spear}, marked with a flag
 * inside {@link DataComponents#CUSTOM_DATA}. Two properties of that component are why it is
 * used rather than a component of our own:
 *
 * <ul>
 *   <li>It is registered {@code .persistent(...)} and NOT {@code .networkSynchronized(...)},
 *       so the marker is saved to disk and never sent to a client. A vanilla client can
 *       connect to a server running this mod and never sees an unknown component.
 *   <li>It is vanilla, so nothing here registers a custom item, block or component — which is
 *       what keeps the whole mod invisible to a client that does not have it.
 * </ul>
 *
 * <p>The marker is written by the recipe (see {@code data/legendaries/recipe/netherite_spear.json}),
 * not by code. This class only reads it.
 */
public final class NetheriteSpear {
	/** Key inside {@code minecraft:custom_data}. Must match the recipe's result components. */
	public static final String MARKER = "legendaries_spear";

	private NetheriteSpear() {
	}

	/**
	 * Whether this stack is the legendary spear.
	 *
	 * <p>An ordinary {@code netherite_spear} — one a creative player gave themselves, or one
	 * surviving in a world from before this mod — is deliberately NOT the legendary: it carries
	 * no marker, so none of the rules apply to it.
	 */
	public static boolean is(ItemStack stack) {
		if (stack.isEmpty() || !stack.is(Items.NETHERITE_SPEAR)) {
			return false;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.copyTag().getBooleanOr(MARKER, false);
	}
}
