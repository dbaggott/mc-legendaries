package io.dnbg.minecraft.legendaries.legendary;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;

/**
 * The operator's commands: {@code pedestal}, {@code item} and {@code config}.
 *
 * <p>{@code pedestal} exists because the pedestal's position is stored rather than derived — world
 * spawn is only its initial siting, and without a way to move it that "stored" would be a
 * distinction with no difference. {@code item} hands out and takes back legendaries, ignoring the
 * one-per-world rule on purpose. {@code config} turns the knobs — naming whatever each one belongs
 * to, which {@link Tunable} settles — so tuning a blast is a command and a swing rather than an
 * edit, a rebuild and a relaunch.
 */
public final class LegendaryCommand {
	private static final DynamicCommandExceptionType UNKNOWN_LEGENDARY = new DynamicCommandExceptionType(
			name -> Component.literal("No legendary called '" + name + "'"));
	private static final DynamicCommandExceptionType UNKNOWN_TUNABLE = new DynamicCommandExceptionType(
			name -> Component.literal("Nothing configurable called '" + name + "'"));
	private static final DynamicCommandExceptionType UNKNOWN_SETTING = new DynamicCommandExceptionType(
			name -> Component.literal("No setting called '" + name + "'"));
	private static final String NOT_LOADED =
			"The pedestal has not loaded yet. Go and look at it, then try again.";

	/**
	 * Every subject {@code config} accepts, by the name it answers to — abilities before the
	 * legendaries that carry them, which is the order they are suggested in.
	 *
	 * <p>Built rather than searched so that two subjects claiming one name fails at load instead of
	 * one quietly shadowing the other. They would share their saved settings as well as their name,
	 * which is the half nothing would report.
	 */
	private static final Map<String, Tunable> TUNABLES = tunablesByName();

	private static Map<String, Tunable> tunablesByName() {
		Map<String, Tunable> byName = new LinkedHashMap<>();
		Stream.<Tunable>concat(Arrays.stream(Ability.values()), Arrays.stream(Legendary.values()))
				.filter(tunable -> !tunable.settings().isEmpty())
				.forEach(tunable -> {
					Tunable clash = byName.put(tunable.commandName(), tunable);
					if (clash != null) {
						throw new IllegalStateException(clash.name() + " and " + tunable.name()
								+ " both answer to '" + tunable.commandName() + "'");
					}
				});
		return byName;
	}

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
								.then(Commands.literal("pedestal")
										.then(legendaryArg()
												.executes(context -> giveToPedestal(context.getSource(),
														namedLegendary(context)))))
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
								.then(Commands.literal("pedestal")
										.then(legendaryArg()
												.executes(context -> deleteFromPedestal(context.getSource(),
														namedLegendary(context)))))
								.then(Commands.argument("players", EntityArgument.players())
										.then(legendaryArg()
												.executes(context -> {
													Legendary legendary = namedLegendary(context);
													return delete(context.getSource(),
															EntityArgument.getPlayers(context, "players"),
															legendary);
												})))))
				.then(Commands.literal("config")
						.then(Commands.literal("get")
								.then(tunableArg()
										.executes(context -> reportSettings(context.getSource(),
												namedTunable(context)))))
						.then(Commands.literal("set")
								.then(tunableArg()
										.then(Commands.argument("setting", StringArgumentType.word())
												.suggests((context, builder) -> SharedSuggestionProvider.suggest(
														settingsOfNamedTunable(context)
																.map(LegendarySetting::commandName), builder))
												.then(Commands.argument("value", IntegerArgumentType.integer())
														.executes(context -> setSetting(context.getSource(),
																namedTunable(context), namedSetting(context),
																IntegerArgumentType.getInteger(context, "value"))))))));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> legendaryArg() {
		return Commands.argument("legendary", StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(
						Arrays.stream(Legendary.values()).map(Legendary::commandName), builder));
	}

	/**
	 * {@code config} names whatever the knob belongs to, which is not always the item in your hand.
	 *
	 * <p>A cooldown belongs to the ABILITY rather than to a carrier of it: two legendaries carrying
	 * one ability tune together, so naming a carrier would ask which of them the answer was about.
	 * What a legendary grants merely by being carried has no ability to belong to, so it belongs to
	 * the legendary. Both go in one argument because somebody configuring something names the thing.
	 *
	 * <p>Only subjects that have knobs are offered or accepted, so there is no "that one has nothing
	 * to configure" to answer.
	 */
	private static RequiredArgumentBuilder<CommandSourceStack, String> tunableArg() {
		return Commands.argument("subject", StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(TUNABLES.keySet(), builder));
	}

	private static Tunable namedTunable(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "subject");
		Tunable subject = TUNABLES.get(name);
		if (subject == null) {
			throw UNKNOWN_TUNABLE.create(name);
		}
		return subject;
	}

	/**
	 * The knobs the subject already typed has, for suggesting the setting that follows it.
	 *
	 * <p>Empty while that subject is still half-typed or unknown: suggestion runs against a partial
	 * command line, so it has to answer without one rather than refuse.
	 */
	private static Stream<LegendarySetting> settingsOfNamedTunable(CommandContext<CommandSourceStack> context) {
		Tunable subject = TUNABLES.get(StringArgumentType.getString(context, "subject"));
		return subject == null ? Stream.empty() : subject.settings().stream();
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
		ItemStack template = template(source, legendary);
		if (template.isEmpty()) {
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

	/**
	 * Stands a legendary on the pedestal, the same way one being claimed back does.
	 *
	 * <p>Refused when that legendary is already standing there. There is one slot per legendary, so
	 * a second copy could only be dropped on the ground beside the pedestal, and an operator who
	 * asked to put something <em>on</em> the pedestal is owed an answer rather than an item in the
	 * grass.
	 */
	private static int giveToPedestal(CommandSourceStack source, Legendary legendary) {
		MinecraftServer server = source.getServer();
		if (LegendaryState.get(server).onPedestal(legendary)) {
			source.sendFailure(Component.literal(legendary.displayName() + " is already on the pedestal"));
			return 0;
		}
		ItemStack template = template(source, legendary);
		if (template.isEmpty()) {
			return 0;
		}
		Pedestal.place(server, template);
		source.sendSuccess(() -> Component.literal(
				"Put " + legendary.displayName() + " on the pedestal"), true);
		return 1;
	}

	/** Destroys the legendary standing on the pedestal, leaving the pedestal itself standing. */
	private static int deleteFromPedestal(CommandSourceStack source, Legendary legendary) {
		MinecraftServer server = source.getServer();
		if (!LegendaryState.get(server).onPedestal(legendary)) {
			source.sendFailure(Component.literal(legendary.displayName() + " is not on the pedestal"));
			return 0;
		}
		if (Pedestal.take(server, legendary).isEmpty()) {
			// The state says it is standing there, so nothing to take means the pedestal has not
			// loaded. Going on would clear the state and leave the display holding it stranded.
			source.sendFailure(Component.literal(NOT_LOADED));
			return 0;
		}
		source.sendSuccess(() -> Component.literal(
				"Removed " + legendary.displayName() + " from the pedestal"), true);
		return 1;
	}

	/**
	 * Builds one legendary from its own recipe, or says why it cannot and hands back an empty stack.
	 *
	 * <p>What comes back is checked for the marker, not merely for existing. A datapack can override
	 * a definition so it still makes an item and no longer makes the legendary — and an unmarked item
	 * is not one: handing it out would put a plain mace in a hand under a legendary's name, and
	 * {@link Pedestal#place} would drop it on the floor of the command with nothing to show.
	 *
	 * <p>Empty is the caller's cue to stop, and the message is already sent by then.
	 */
	private static ItemStack template(CommandSourceStack source, Legendary legendary) {
		ItemStack template = legendary.create(source.getServer());
		if (template.isEmpty()) {
			source.sendFailure(Component.literal("Nothing defines " + legendary.displayName()
					+ " any more — a datapack may have removed it."));
			return ItemStack.EMPTY;
		}
		if (!legendary.is(template)) {
			source.sendFailure(Component.literal("What defines " + legendary.displayName()
					+ " no longer makes it — a datapack may have overridden the result."));
			return ItemStack.EMPTY;
		}
		return template;
	}

	private static LegendarySetting namedSetting(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "setting");
		for (LegendarySetting setting : LegendarySetting.values()) {
			if (setting.commandName().equals(name)) {
				return setting;
			}
		}
		throw UNKNOWN_SETTING.create(name);
	}

	/** Lists the settings one subject has, with the value actually in force. */
	private static int reportSettings(CommandSourceStack source, Tunable subject) {
		LegendaryState state = LegendaryState.get(source.getServer());
		StringBuilder report = new StringBuilder(subject.displayName() + ":");
		for (LegendarySetting setting : subject.settings()) {
			report.append("\n  ").append(setting.commandName()).append(" = ")
					.append(state.setting(subject, setting)).append(' ').append(setting.unit());
		}
		String text = report.toString();
		source.sendSuccess(() -> Component.literal(text), false);
		return 1;
	}

	/**
	 * Changes one setting.
	 *
	 * <p>Bounds are the setting's own rather than the argument type's, so the message can say what
	 * the limit is and why a value was refused. A new {@code cooldown} reaches a wait already
	 * running rather than only the next one, and needs nothing here to make it: {@link
	 * AbilityCooldown} reads the setting rather than a copy of it, on every pass.
	 */
	private static int setSetting(CommandSourceStack source, Tunable subject, LegendarySetting setting,
			int value) {
		if (!subject.settings().contains(setting)) {
			source.sendFailure(Component.literal(
					subject.displayName() + " has no " + setting.commandName() + " to set"));
			return 0;
		}
		if (value < setting.min() || value > setting.max()) {
			source.sendFailure(Component.literal(setting.commandName() + " must be between "
					+ setting.min() + " and " + setting.max() + " " + setting.unit()));
			return 0;
		}
		LegendaryState.get(source.getServer()).setSetting(subject, setting, value);
		source.sendSuccess(() -> Component.literal(subject.displayName() + " " + setting.commandName()
				+ " set to " + value + " " + setting.unit()), true);
		// One setting changed — a count, in the same currency as every other command here, because
		// this is what `execute store result` reads.
		return 1;
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
				source.sendFailure(Component.literal(NOT_LOADED));
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
