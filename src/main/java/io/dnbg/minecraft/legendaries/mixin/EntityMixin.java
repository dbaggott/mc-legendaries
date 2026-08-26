package io.dnbg.minecraft.legendaries.mixin;

import io.dnbg.minecraft.legendaries.spear.ClaimTracked;
import io.dnbg.minecraft.legendaries.spear.NetheriteSpear;
import io.dnbg.minecraft.legendaries.spear.Pedestal;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The loss backstop: the spear cannot be destroyed out of the world.
 *
 * <p>{@code setRemoved} is the single funnel every removal passes through, which is why the hook
 * is here rather than on the individual causes — despawning after five minutes on the ground,
 * falling into the void, being blown up, and {@code /kill} all arrive at the same place. It covers
 * two carriers: an item entity lying on the ground, and a mob that picked the spear up despite the
 * refusals in {@code MobMixin} and {@code FoxMixin}.
 *
 * <p>What is deliberately NOT caught:
 * <ul>
 *   <li>A pickup, flagged by {@link ItemEntityMixin} — otherwise walking over the spear would
 *       send it home.
 *   <li>{@code UNLOADED_TO_CHUNK} and {@code CHANGED_DIMENSION}, which are bookkeeping. An item in
 *       an unloaded chunk is not lost, it is asleep.
 *   <li>Lava and fire, which never reach here at all: {@code netherite_spear} is registered
 *       {@code .fireResistant()} in vanilla, so it simply does not burn.
 * </ul>
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "setRemoved", at = @At("HEAD"))
	private void legendaries$returnSpearOnLoss(Entity.RemovalReason reason, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (self.level().isClientSide()) {
			return;
		}
		if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED) {
			return;
		}
		MinecraftServer server = self.level().getServer();
		if (server == null) {
			return;
		}
		if (self instanceof ItemEntity itemEntity) {
			legendaries$returnFromGround(server, itemEntity);
		} else if (self instanceof Mob mob) {
			legendaries$returnFromMob(server, mob);
		}
	}

	@Unique
	private static void legendaries$returnFromGround(MinecraftServer server, ItemEntity itemEntity) {
		ItemStack stack = itemEntity.getItem();
		if (!NetheriteSpear.is(stack)) {
			return;
		}
		if (itemEntity instanceof ClaimTracked tracked && tracked.legendaries$wasClaimed()) {
			return;
		}
		Pedestal.place(server, stack);
	}

	/**
	 * Recovers the spear from a mob that got hold of it anyway.
	 *
	 * <p>This is the guarantee, and the refusals in {@code MobMixin} and {@code FoxMixin} are only
	 * the first line. {@code Mob.pickUpItem} is virtual with nine overrides, and they do not agree
	 * on how they take an item — a fox splits the stack, a piglin discards the entity before it
	 * decides — so no set of refusals can be shown to be complete. What CAN be enumerated is where
	 * the spear ends up if one is missed: an equipment slot, or a carried inventory. Both are read
	 * back here.
	 *
	 * <p>No duplicate is possible on death: {@code dropCustomDeathLoot} clears the slot after
	 * dropping, so a spear that dropped is already gone from equipment by the time this runs.
	 */
	@Unique
	private static void legendaries$returnFromMob(MinecraftServer server, Mob mob) {
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			ItemStack held = mob.getItemBySlot(slot);
			if (NetheriteSpear.is(held)) {
				mob.setItemSlot(slot, ItemStack.EMPTY);
				Pedestal.place(server, held);
				return;
			}
		}
		if (!(mob instanceof InventoryCarrier carrier)) {
			return;
		}
		SimpleContainer inventory = carrier.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack held = inventory.getItem(i);
			if (NetheriteSpear.is(held)) {
				inventory.setItem(i, ItemStack.EMPTY);
				Pedestal.place(server, held);
				return;
			}
		}
	}
}
