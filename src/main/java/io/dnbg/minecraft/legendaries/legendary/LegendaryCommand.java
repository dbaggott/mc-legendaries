package io.dnbg.minecraft.legendaries.legendary;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import java.util.ArrayList;
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
								.executes(context -> report(context.getSource()))));
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
