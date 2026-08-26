package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.spear.NetheriteSpear;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps spears out of mobs' hands, in the two ways a mob can get one.
 *
 * <p><strong>Picking the legendary up.</strong> A mob equipping it would take it outside every
 * other rule here — it is no longer in a container, no longer an item entity, and no longer
 * anywhere the pedestal hook can see. Worse, the loss backstop cannot even catch the moment:
 * {@code pickUpItem} copies the stack, equips the copy, then {@code shrink}s the live one before
 * discarding the entity, so by the time {@code setRemoved} runs the stack is already empty and the
 * spear reads as an ordinary vanished item. The mob then despawns or burns and the one spear in
 * the world is gone for good.
 *
 * <p><strong>Dropping a vanilla one on death.</strong> Mob equipment does not drop through a loot
 * table: {@code dropCustomDeathLoot} rolls {@code dropChances} and calls {@code spawnAtLocation}
 * directly, which {@code LootTableEvents.MODIFY_DROPS} never sees. Stripping the equipment first is
 * what makes "mobs fight with spears but never drop one" true, rather than only true of chests.
 */
@Mixin(Mob.class)
public abstract class MobMixin {
	@Inject(method = "equipItemIfPossible", at = @At("HEAD"), cancellable = true)
	private void legendaries$refuseTheSpear(ServerLevel level, ItemStack stack,
			CallbackInfoReturnable<ItemStack> cir) {
		if (NetheriteSpear.is(stack)) {
			// An empty return is how this method says "equipped nothing", which leaves the item
			// entity intact on the ground rather than consumed.
			cir.setReturnValue(ItemStack.EMPTY);
		}
	}

	@Inject(method = "dropCustomDeathLoot", at = @At("HEAD"))
	private void legendaries$stripSpearsBeforeDropping(ServerLevel level, DamageSource source,
			boolean hitByPlayer, CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			if (self.getItemBySlot(slot).is(ItemTags.SPEARS)) {
				self.setItemSlot(slot, ItemStack.EMPTY);
			}
		}
	}
}
