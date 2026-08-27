package io.dnbg.minecraft.legendaries.legendary;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The rules that follow a legendary around: what it grants, what it refuses, and what no longer
 * drops now that it exists.
 */
public final class LegendaryRules {
	/**
	 * Speed II, refreshed on a cadence shorter than its own duration so it never visibly flickers
	 * and never outlives the spear leaving the inventory by more than one refresh.
	 */
	private static final int EFFECT_INTERVAL_TICKS = 20;
	private static final int EFFECT_DURATION_TICKS = 40;

	/** Reset per session; the pedestal is raised once the site's chunk is genuinely loaded. */
	private static boolean pedestalRaised;

	private LegendaryRules() {
	}

	public static void register() {
		stripSpearDrops();
		raisePedestalOnce();
		ServerEntityEvents.ENTITY_LOAD.register(Pedestal::discardStaleOnLoad);
		spinPedestal();
		grantCarriedEffects();
		wireMoltenBlast();
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
				stacks.removeIf(stack -> stack.is(ItemTags.SPEARS) && !Legendary.isAny(stack)));
	}

	/**
	 * Raises the pedestal, once, as soon as its chunk is loaded.
	 *
	 * <p>Not on server-started: an entity in an unloaded chunk is not in the world's index, so
	 * building at a moment the site is cold would find nothing standing and raise a second pedestal
	 * beside the first. Waiting for {@code isLoaded} makes the "is one already here?" check mean
	 * something.
	 */
	private static void raisePedestalOnce() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> pedestalRaised = false);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (pedestalRaised) {
				return;
			}
			ServerLevel home = LegendaryState.home(server);
			BlockPos site = Pedestal.position(server, LegendaryState.get(server));
			// areEntitiesLoaded, not isLoaded: the latter answers "is the chunk here", and the
			// entity index arrives separately behind a future. In the gap between the two, a
			// pedestal that is standing reads as absent, and ensure() would raise a second one
			// beside it — which the stale sweep then keeps, because it keys on position and both
			// sets are at the same position.
			if (!home.areEntitiesLoaded(ChunkPos.pack(site))) {
				return;
			}
			Pedestal.ensure(server);
			pedestalRaised = true;
		});
	}

	/** Keeps the legendaries turning on their pedestal. */
	private static void spinPedestal() {
		ServerTickEvents.END_SERVER_TICK.register(Pedestal::spin);
	}

	private static void grantCarriedEffects() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % EFFECT_INTERVAL_TICKS != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				for (Legendary legendary : Legendary.values()) {
					legendary.carriedEffect().ifPresent(effect -> {
						if (carrying(player, legendary)) {
							player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION_TICKS,
									legendary.carriedAmplifier(), true, false, true));
						}
					});
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
	public static boolean carrying(Player player, Legendary legendary) {
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (legendary.is(stack)) {
				return true;
			}
		}
		return legendary.is(player.getOffhandItem()) || legendary.is(player.getMainHandItem());
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
			if (!Legendary.isAny(player.getItemInHand(hand))) {
				return InteractionResult.PASS;
			}
			refuse(player, legendaryName(player.getItemInHand(hand)) + " will not be left on display.");
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
			BlockState state = level.getBlockState(hitResult.getBlockPos());
			Block block = state.getBlock();
			boolean refuse;
			if (block instanceof ShelfBlock) {
				refuse = shelfWouldTakeALegendary(player, hand, state);
			} else if (block instanceof DecoratedPotBlock) {
				refuse = Legendary.isAny(player.getItemInHand(hand));
			} else {
				return InteractionResult.PASS;
			}
			if (!refuse) {
				return InteractionResult.PASS;
			}
			refuse(player, "That will not be set down there.");
			return InteractionResult.FAIL;
		});
	}

	/**
	 * A shelf moves different things depending on whether it is powered, and the held item is only
	 * half the story.
	 *
	 * <p>Unpowered, {@code useItemOn} swaps the single item in hand. <strong>Powered, it calls
	 * {@code swapHotbar} and exchanges the player's whole hotbar with the shelf</strong> — so the
	 * spear travels even when the player is holding something else entirely, which is how it got
	 * onto a shelf despite this rule already existing.
	 *
	 * <p>The two cases are kept apart rather than collapsed into "refuse whenever the spear is in
	 * the hotbar", because that would stop a player using any shelf at all while carrying it.
	 */
	private static boolean shelfWouldTakeALegendary(Player player, InteractionHand hand, BlockState state) {
		if (Legendary.isAny(player.getItemInHand(hand))) {
			return true;
		}
		if (!state.getValue(ShelfBlock.POWERED)) {
			return false;
		}
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < Inventory.SELECTION_SIZE; slot++) {
			if (Legendary.isAny(inventory.getItem(slot))) {
				return true;
			}
		}
		return false;
	}

	private static InteractionResult claimFromPedestal(Player player) {
		if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		MinecraftServer server = serverPlayer.level().getServer();
		if (server == null) {
			return InteractionResult.PASS;
		}
		// One per click, chosen at random from whatever is standing there.
		ItemStack claimed = Pedestal.takeOne(server);
		if (claimed.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!serverPlayer.getInventory().add(claimed)) {
			serverPlayer.drop(claimed, false);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Sneak + right-click with the Mace fires a Molten Blast.
	 *
	 * <p>Sneak-gated rather than a plain right-click because the blast deletes the ground under the
	 * player's feet — an accidental trigger is expensive in a way a mis-swing is not. A keybind would read better still, but only for players who
	 * installed the mod; this works from a vanilla client.
	 */
	private static void wireMoltenBlast() {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack held = player.getItemInHand(hand);
			if (!Legendary.MACE.is(held) || !player.isShiftKeyDown()) {
				return InteractionResult.PASS;
			}
			if (level instanceof ServerLevel serverLevel) {
				MinecraftServer server = serverLevel.getServer();
				if (!AbilityCooldown.ready(server, player, Legendary.MACE)) {
					return InteractionResult.FAIL;
				}
				MoltenBlast.fire(serverLevel, player);
				AbilityCooldown.begin(server, player, Legendary.MACE, held);
			} else if (player.getCooldowns().isOnCooldown(held)) {
				// A client has no settings to read, so the swipe the server sent it is the whole of
				// what it knows about the wait. Refusing on that is what keeps the swing animation
				// off a modded client mid-cooldown; a vanilla one swings and the server refuses.
				return InteractionResult.FAIL;
			}
			// SUCCESS on both sides so the client plays the swing rather than waiting on the server.
			return InteractionResult.SUCCESS;
		});
	}

	/** How to name whatever legendary this stack is, for a message aimed at a player. */
	private static String legendaryName(ItemStack stack) {
		return Legendary.of(stack).map(Legendary::displayName).orElse("That");
	}

	/** Tells the player why, on the actionbar, where a refusal is read rather than scrolled past. */
	public static void refuse(Player player, String message) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(Component.literal(message), true);
		}
	}
}
