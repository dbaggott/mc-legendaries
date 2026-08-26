package io.dnbg.minecraft.legendaries.legendary;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /legendaries pedestal} — where the spear comes home to.
 *
 * <p>Exists because the pedestal's position is stored rather than derived. World spawn is only its
 * initial siting; without a way to move it, that "stored" would be a distinction with no
 * difference.
 */
public final class LegendaryCommand {
	private static final DynamicCommandExceptionType UNKNOWN_LEGENDARY = new DynamicCommandExceptionType(
			name -> Component.literal("No legendary called '" + name + "'"));

	private LegendaryCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
				dispatcher.register(build()));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("legendaries")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("pedestal")
						.then(Commands.literal("here")
								.executes(context -> move(context.getSource(),
										BlockPos.containing(context.getSource().getPosition()))))
						.then(Commands.literal("at")
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(context -> move(context.getSource(),
												BlockPosArgument.getLoadedBlockPos(context, "pos")))))
						.then(Commands.literal("where")
								.executes(context -> report(context.getSource()))))
				.then(Commands.literal("item")
						.then(Commands.literal("give")
								.then(Commands.argument("players", EntityArgument.players())
										.then(legendaryArg()
												// Legendary first, players second: a mistyped name
												// should say so rather than being masked by a
												// selector that matched nobody.
												.executes(context -> {
													Legendary legendary = namedLegendary(context);
													return give(context.getSource(),
															EntityArgument.getPlayers(context, "players"),
															legendary);
												}))))
						.then(Commands.literal("delete")
								.then(Commands.argument("players", EntityArgument.players())
										.then(legendaryArg()
												.executes(context -> {
													Legendary legendary = namedLegendary(context);
													return delete(context.getSource(),
															EntityArgument.getPlayers(context, "players"),
															legendary);
												})))));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> legendaryArg() {
		return Commands.argument("legendary", StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(
						Arrays.stream(Legendary.values()).map(Legendary::commandName), builder));
	}

	private static Legendary namedLegendary(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "legendary");
		for (Legendary legendary : Legendary.values()) {
			if (legendary.commandName().equals(name)) {
				return legendary;
			}
		}
		throw UNKNOWN_LEGENDARY.create(name);
	}

	/**
	 * Hands out a legendary, bypassing the one-per-world rule entirely.
	 *
	 * <p>That is the point of it: this is the operator's escape hatch, for recovering a legendary
	 * lost to something the backstop could not catch, or for testing. It does not mark the world as
	 * having crafted one, so the crafting route stays open — a given copy is a copy, not the craft.
	 *
	 * <p>The stack comes from the legendary's own recipe rather than being built here, so a given
	 * item is identical to a crafted one and cannot drift from it.
	 */
	private static int give(CommandSourceStack source, Collection<ServerPlayer> players, Legendary legendary) {
		ItemStack template = legendary.create(source.getServer());
		if (template.isEmpty()) {
			source.sendFailure(Component.literal(
					"No recipe for " + legendary.displayName() + " — a datapack may have removed it."));
			return 0;
		}
		for (ServerPlayer player : players) {
			ItemStack copy = template.copy();
			if (!player.getInventory().add(copy)) {
				player.drop(copy, false);
			}
		}
		source.sendSuccess(() -> Component.literal(
				"Gave " + legendary.displayName() + " to " + players.size() + " player(s)"), true);
		return players.size();
	}

	/** Removes every copy of a legendary from the named players, and says how many it found. */
	private static int delete(CommandSourceStack source, Collection<ServerPlayer> players, Legendary legendary) {
		int removed = 0;
		for (ServerPlayer player : players) {
			Inventory inventory = player.getInventory();
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				if (legendary.is(inventory.getItem(slot))) {
					inventory.setItem(slot, ItemStack.EMPTY);
					removed++;
				}
			}
		}
		int total = removed;
		source.sendSuccess(() -> Component.literal(
				"Removed " + total + " " + legendary.displayName() + " from " + players.size() + " player(s)"), true);
		return total;
	}

	private static int move(CommandSourceStack source, BlockPos target) {
		MinecraftServer server = source.getServer();
		ServerLevel home = LegendaryState.home(server);
		if (source.getLevel() != home) {
			// The pedestal is always built in the overworld, so a position read from anywhere else is
			// a set of coordinates from the wrong map — `here` in the Nether would site it eight times
			// too far in, and `at` validates loadedness against the caller's level rather than this one.
			source.sendFailure(Component.literal("The pedestal lives in the overworld. Run this from there."));
			return 0;
		}

		LegendaryState state = LegendaryState.get(server);
		BlockPos previous = state.pedestalPos();

		// Take everything off the OLD site before repointing the state, then put it all back at the
		// new one. Doing it in this order is what stops a move from stranding a legendary at a
		// position nothing points at.
		List<ItemStack> carried = new ArrayList<>();
		while (!Pedestal.isEmpty(server)) {
			ItemStack taken = Pedestal.takeOne(server);
			if (taken.isEmpty()) {
				// takeOne() refused rather than handing over an empty pedestal, so a legendary is still
				// standing at the old site and the state still says so. Moving now would repoint the
				// state away from it and the next chunk load would destroy it.
				source.sendFailure(Component.literal(
						"The pedestal has not loaded yet. Go and look at it, then try again."));
				for (ItemStack back : carried) {
					Pedestal.place(server, back);
				}
				return 0;
			}
			carried.add(taken);
		}
		if (previous != null) {
			Pedestal.clearEntities(home, previous);
		}

		state.setPedestalPos(target);
		// Raise the plinth at the new site whether or not anything came with it — an empty pedestal is
		// still a pedestal, and a move that left nothing standing would be indistinguishable from the
		// command having failed.
		Pedestal.ensure(server);
		for (ItemStack back : carried) {
			Pedestal.place(server, back);
		}

		source.sendSuccess(() -> Component.literal("Pedestal moved to " + target.toShortString()), true);
		return 1;
	}

	private static int report(CommandSourceStack source) {
		LegendaryState state = LegendaryState.get(source.getServer());
		BlockPos pos = state.pedestalPos();
		String where;
		if (pos == null) {
			// Reachable only in the ticks before the pedestal is raised, which is why it describes a
			// moment rather than a condition somebody can be left in.
			where = "not sited yet — the world has not finished starting";
		} else {
			List<String> home = state.onPedestal().stream().map(Legendary::displayName).toList();
			where = pos.toShortString() + (home.isEmpty() ? " (empty)" : " holding " + String.join(", ", home));
		}
		source.sendSuccess(() -> Component.literal("Pedestal: " + where), false);
		return 1;
	}
}
