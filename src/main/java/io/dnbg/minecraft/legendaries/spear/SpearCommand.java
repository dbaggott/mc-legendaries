package io.dnbg.minecraft.legendaries.spear;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /legendaries pedestal} — where the spear comes home to.
 *
 * <p>Exists because the pedestal's position is stored rather than derived. World spawn is only its
 * initial siting; without a way to move it, that "stored" would be a distinction with no
 * difference.
 */
public final class SpearCommand {
	private SpearCommand() {
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
		ServerLevel home = SpearState.home(server);
		if (source.getLevel() != home) {
			// The pedestal is always built in the overworld, so a position read from anywhere else is
			// a set of coordinates from the wrong map — `here` in the Nether would site it eight times
			// too far in, and `at` validates loadedness against the caller's level rather than this one.
			source.sendFailure(Component.literal("The pedestal lives in the overworld. Run this from there."));
			return 0;
		}

		SpearState state = SpearState.get(server);
		BlockPos previous = state.pedestalPos();

		// Take from the OLD site before repointing the state, then place at the new one. Doing it in
		// this order is what stops a move from stranding the spear at a position nothing points at.
		ItemStack carried = ItemStack.EMPTY;
		if (state.spearOnPedestal()) {
			carried = Pedestal.take(server);
			if (carried.isEmpty()) {
				// take() refused rather than handing over an empty pedestal, so the spear is still
				// standing at the old site and the state still says so. Moving now would repoint the
				// state away from a real spear and the next chunk load would destroy it.
				source.sendFailure(Component.literal(
						"The spear's pedestal has not loaded yet. Go and look at it, then try again."));
				return 0;
			}
		}
		if (previous != null) {
			Pedestal.clearEntities(home, previous);
		}

		state.setPedestalPos(target);
		if (!carried.isEmpty()) {
			Pedestal.place(server, carried);
		}

		source.sendSuccess(() -> Component.literal("Pedestal moved to " + target.toShortString()), true);
		return 1;
	}

	private static int report(CommandSourceStack source) {
		SpearState state = SpearState.get(source.getServer());
		BlockPos pos = state.pedestalPos();
		String where = pos == null
				? "not sited yet; it will appear at world spawn the first time the spear needs it"
				: pos.toShortString() + (state.spearOnPedestal() ? " (spear is there)" : " (spear is out in the world)");
		source.sendSuccess(() -> Component.literal("Pedestal: " + where), false);
		return 1;
	}
}
