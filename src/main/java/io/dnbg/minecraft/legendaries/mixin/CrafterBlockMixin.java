package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.legendary.Legendary;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A crafter cannot make a legendary.
 *
 * <p>Every other route to a legendary runs through a {@code Slot} — which is where the one-craft gate
 * lives, recorded on {@code ResultSlot.onTake} and refused in {@code SlotMixin.mayPickup}. A
 * crafter block reaches the recipe without constructing one: {@code dispenseFrom} goes straight
 * from {@code getPotentialResults} to {@code assemble} to dispensing the stack. So the gate never
 * fires, every redstone pulse yields another legendary, and — worse — the world is never marked as
 * having crafted one, leaving the crafting-table route open behind it.
 *
 * <p>Refusing at {@code getPotentialResults} makes the crafter behave as though the grid matches
 * nothing, which is the outcome it already knows how to handle.
 *
 * <p>The result is assembled and inspected rather than matched by recipe id, so a datapack that
 * declares its own recipe for the same item is refused too.
 */
@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {
	@Inject(method = "getPotentialResults", at = @At("RETURN"), cancellable = true)
	private static void legendaries$refuseLegendaries(ServerLevel level, CraftingInput input,
			CallbackInfoReturnable<Optional<RecipeHolder<CraftingRecipe>>> cir) {
		Optional<RecipeHolder<CraftingRecipe>> found = cir.getReturnValue();
		if (found.isEmpty()) {
			return;
		}
		if (Legendary.isAny(found.get().value().assemble(input))) {
			cir.setReturnValue(Optional.empty());
		}
	}
}
