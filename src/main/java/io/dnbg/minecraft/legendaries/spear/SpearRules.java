package io.dnbg.minecraft.legendaries.spear;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.ShelfBlock;

/**
 * The rules that follow the spear around: what it grants, what it refuses, and what no longer
 * drops now that it exists.
 */
public final class SpearRules {
	/**
	 * Speed II, refreshed on a cadence shorter than its own duration so it never visibly flickers
	 * and never outlives the spear leaving the inventory by more than one refresh.
	 */
	private static final int EFFECT_INTERVAL_TICKS = 20;
	private static final int EFFECT_DURATION_TICKS = 40;
	private static final int SPEED_II = 1;

	private SpearRules() {
	}

	public static void register() {
		stripSpearDrops();
		ServerEntityEvents.ENTITY_LOAD.register(Pedestal::discardStaleOnLoad);
		grantSpeedWhileCarried();
		wireEntityInteractions();
		refuseDirectPlacement();
	}

	/**
	 * No spear ever drops, from any table.
	 *
	 * <p>Deliberately wider than "not in loot chests": spears are mob equipment in vanilla — there
	 * are dedicated spear AI behaviours — so a mob death drop is a second route to one. Mobs keep
	 * spawning with spears and fighting with them; only the drop is removed, which leaves the
	 * vanilla combat intact while keeping the crafted spear the only obtainable one.
	 */
	private static void stripSpearDrops() {
		LootTableEvents.MODIFY_DROPS.register((key, context, stacks) ->
				// Unmarked spears only. A mob that got hold of the legendary despite the refusals
				// can reach a drop, and stripping it here would have this mod destroying the one
				// item everything else in it exists to keep.
				stacks.removeIf(stack -> stack.is(ItemTags.SPEARS) && !NetheriteSpear.is(stack)));
	}

	private static void grantSpeedWhileCarried() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % EFFECT_INTERVAL_TICKS != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (carrying(player)) {
					player.addEffect(new MobEffectInstance(
							MobEffects.SPEED, EFFECT_DURATION_TICKS, SPEED_II, true, false, true));
				}
			}
		});
	}

	/**
	 * Whether the player is carrying the spear, in hand or in inventory.
	 *
	 * <p>Only the player's own slots count. A spear nested inside a shulker box in the inventory
	 * does not — though nothing can put it there, since the container rules refuse it.
	 */
	public static boolean carrying(Player player) {
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (NetheriteSpear.is(stack)) {
				return true;
			}
		}
		return NetheriteSpear.is(player.getOffhandItem()) || NetheriteSpear.is(player.getMainHandItem());
	}

	/**
	 * The two things a right-click on an entity can mean here: claiming the spear off its pedestal,
	 * and trying to hang it somewhere it must not go.
	 *
	 * <p>A frame or a stand is not storage, but it takes the spear out of a player's hands and
	 * leaves it on a wall — the same outcome the container rules exist to prevent.
	 */
	private static void wireEntityInteractions() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (entity instanceof Interaction && entity.entityTags().contains(Pedestal.TAG)) {
				return claimFromPedestal(player);
			}
			if (!(entity instanceof ItemFrame) && !(entity instanceof ArmorStand)) {
				return InteractionResult.PASS;
			}
			if (!NetheriteSpear.is(player.getItemInHand(hand))) {
				return InteractionResult.PASS;
			}
			refuse(player, "The Netherite Spear will not be left on display.");
			return InteractionResult.FAIL;
		});
	}

	/**
	 * Blocks that swallow a held item on right-click, with no screen in between.
	 *
	 * <p>{@code Slot.mayPlace} cannot see these: a shelf and a decorated pot take the stack straight
	 * out of the hand in {@code useItemOn} without ever building a menu. A shelf accepts anything,
	 * and a decorated pot only checks that it is currently empty. Hoppers feeding either are already
	 * refused, because both block entities are ordinary containers — it is only the hand-placement
	 * that needs its own answer.
	 *
	 * <p>A chiseled bookshelf is deliberately absent: it filters on {@code #bookshelf_books}, so a
	 * spear cannot reach it in the first place.
	 */
	private static void refuseDirectPlacement() {
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (!NetheriteSpear.is(player.getItemInHand(hand))) {
				return InteractionResult.PASS;
			}
			var block = level.getBlockState(hitResult.getBlockPos()).getBlock();
			if (!(block instanceof ShelfBlock) && !(block instanceof DecoratedPotBlock)) {
				return InteractionResult.PASS;
			}
			refuse(player, "The Netherite Spear will not be set down there.");
			return InteractionResult.FAIL;
		});
	}

	private static InteractionResult claimFromPedestal(Player player) {
		if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		MinecraftServer server = serverPlayer.level().getServer();
		if (server == null) {
			return InteractionResult.PASS;
		}
		ItemStack spear = Pedestal.take(server);
		if (spear.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!serverPlayer.getInventory().add(spear)) {
			serverPlayer.drop(spear, false);
		}
		return InteractionResult.SUCCESS;
	}

	/** Tells the player why, on the actionbar, where a refusal is read rather than scrolled past. */
	public static void refuse(Player player, String message) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(Component.literal(message), true);
		}
	}
}
