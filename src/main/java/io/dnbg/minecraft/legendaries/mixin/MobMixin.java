package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.legendary.Legendary;
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
 * Keeps spears out of mobs' hands — with the emphasis on "keeps", not "prevents".
 *
 * <p>Prevention here is best-effort by nature. {@code Mob.pickUpItem} is virtual and nine classes
 * override it; a fox and a dolphin never call {@code equipItemIfPossible} at all, and
 * {@code PiglinAi.pickUpItem} discards the item entity <em>before</em> asking, then files the stack
 * into the piglin's own inventory when the answer is no. Hooking the equip call therefore misses
 * two of those and makes the third worse. {@code canHoldItem} is the real decision point for
 * everything that inherits it, so that is what is refused here — but it is not a guarantee, and it
 * is deliberately not treated as one.
 *
 * <p>The guarantee is recovery instead, and it lives in {@code EntityMixin}: a mob can only put the
 * spear in an equipment slot or a carried inventory, and both are read back when the mob is
 * removed. That holds for overrides nobody has enumerated.
 *
 * <p>Which is why {@link #legendaries$stripSpearsBeforeDropping} strips only <em>unmarked</em>
 * spears. Mob equipment does not drop through a loot table — {@code dropCustomDeathLoot} rolls
 * {@code dropChances} and calls {@code spawnAtLocation} directly, which
 * {@code LootTableEvents.MODIFY_DROPS} never sees — so this is what makes "mobs fight with spears
 * but never drop one" true. Matching the legendary here as well would have this code destroying the
 * very thing the rest of it exists to protect.
 */
@Mixin(Mob.class)
public abstract class MobMixin {
	@Inject(method = "canHoldItem", at = @At("HEAD"), cancellable = true)
	private void legendaries$refuseTheSpear(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (Legendary.isAny(stack)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "dropCustomDeathLoot", at = @At("HEAD"))
	private void legendaries$stripSpearsBeforeDropping(ServerLevel level, DamageSource source,
			boolean hitByPlayer, CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			ItemStack held = self.getItemBySlot(slot);
			if (held.is(ItemTags.SPEARS) && !Legendary.isAny(held)) {
				self.setItemSlot(slot, ItemStack.EMPTY);
			}
		}
	}
}
