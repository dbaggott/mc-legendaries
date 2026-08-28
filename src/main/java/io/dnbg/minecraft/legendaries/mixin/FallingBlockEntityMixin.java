package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.legendary.Legendary;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A legendary that falls and breaks drops the legendary, not the plain block.
 *
 * <p>This is the route a player actually takes to a dragon egg, and it does not touch the block's
 * loot table. {@code DragonEggBlock.attack} teleports rather than breaking, so the egg cannot be
 * mined at all — it is knocked about until it falls onto something that will not hold it, or pushed
 * off its perch by a piston. Either way it lands as a {@link FallingBlockEntity}, which drops the
 * bare {@code Block} through {@code spawnAtLocation} with no loot table anywhere in the call. Left
 * alone, the one harvest anybody performs yields an unmarked egg that obeys none of the rules,
 * while blowing it up — which does go through the table — yields the legendary.
 *
 * <p>So the drop is swapped for the legendary here, and the legendary is still built by asking the
 * data file rather than by naming components: this decides <em>whether</em> a legendary is what
 * should have dropped, never what one is.
 *
 * <p>Redirected rather than injected because {@code tick} drops the block at three separate points —
 * landing where it cannot stay, being cancelled, and falling out of the world — and all three are
 * the same decision. One redirect covers them without naming which of them is which.
 */
@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin {
	// No owner on the target: the call is inherited from Entity but compiled against
	// FallingBlockEntity, and naming either one is a guess that only holds while the compiler keeps
	// making the same choice. The signature alone is unambiguous here.
	@Redirect(method = "tick", at = @At(value = "INVOKE",
			target = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;"
					+ "Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
	private ItemEntity legendaries$dropTheLegendary(FallingBlockEntity self, ServerLevel level, ItemLike dropped) {
		Legendary legendary = Legendary.madeOf(dropped.asItem()).orElse(null);
		if (legendary == null) {
			return self.spawnAtLocation(level, dropped);
		}
		ItemStack stack = legendary.create(level.getServer());
		// An empty stack means a datapack removed the definition. Dropping the plain block is what
		// vanilla would have done, and is better than dropping nothing at all.
		return stack.isEmpty() ? self.spawnAtLocation(level, dropped) : self.spawnAtLocation(level, stack);
	}
}
