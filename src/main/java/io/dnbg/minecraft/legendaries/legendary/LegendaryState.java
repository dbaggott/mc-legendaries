package io.dnbg.minecraft.legendaries.legendary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.dnbg.minecraft.legendaries.Legendaries;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The per-world facts about every legendary: which have been crafted, which are on the pedestal, and
 * where the pedestal is.
 *
 * <p>Stored on the OVERWORLD only, and read from there no matter which dimension asks. A
 * {@code SavedDataStorage} is per-level, so keeping this per-dimension would give the Nether its own
 * set — "one in the entire world" has to mean one across every dimension.
 *
 * <p>Everything here is keyed by {@link Legendary#name()} rather than by ordinal, so reordering or
 * removing an entry in the enum cannot silently reassign a world's saved state to a different item.
 * An unrecognised name is dropped on read, which is what makes removing a legendary safe.
 *
 * <p>The pedestal position is <em>stored</em> rather than derived from world spawn. World spawn is
 * only where it is sited on a world's first tick; an admin can move it afterwards, and moving world
 * spawn later must not drag the pedestal along with it.
 */
public class LegendaryState extends SavedData {
	private static final Codec<Set<Legendary>> LEGENDARY_SET = Codec.STRING.listOf().xmap(
			names -> {
				Set<Legendary> set = EnumSet.noneOf(Legendary.class);
				for (String name : names) {
					for (Legendary legendary : Legendary.values()) {
						if (legendary.name().equals(name)) {
							set.add(legendary);
						}
					}
				}
				return set;
			},
			set -> set.stream().map(Legendary::name).toList());

	private static final Codec<LegendaryState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			LEGENDARY_SET.fieldOf("crafted").orElseGet(() -> EnumSet.noneOf(Legendary.class))
					.forGetter(state -> state.crafted),
			BlockPos.CODEC.optionalFieldOf("pedestal").forGetter(state -> Optional.ofNullable(state.pedestalPos)),
			LEGENDARY_SET.fieldOf("on_pedestal").orElseGet(() -> EnumSet.noneOf(Legendary.class))
					.forGetter(state -> state.onPedestal),
			Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("settings").orElseGet(Map::of)
					.forGetter(state -> state.settings))
			.apply(instance, LegendaryState::new));

	private static final SavedDataType<LegendaryState> TYPE = new SavedDataType<>(
			Legendaries.id("legendaries"), LegendaryState::new, CODEC, DataFixTypes.LEVEL);

	private final Map<String, Integer> settings = new HashMap<>();
	private final Set<Legendary> crafted = EnumSet.noneOf(Legendary.class);
	private final Set<Legendary> onPedestal = EnumSet.noneOf(Legendary.class);
	private BlockPos pedestalPos;

	public LegendaryState() {
	}

	private LegendaryState(Set<Legendary> crafted, Optional<BlockPos> pedestalPos, Set<Legendary> onPedestal,
			Map<String, Integer> settings) {
		this.crafted.addAll(crafted);
		this.onPedestal.addAll(onPedestal);
		this.pedestalPos = pedestalPos.orElse(null);
		this.settings.putAll(settings);
	}

	/**
	 * Overridden settings, keyed {@code SUBJECT.setting}.
	 *
	 * <p>The subject is whatever the setting describes — an ability where several legendaries carry
	 * it and tune together, the legendary itself where the knob is for what carrying it grants. One
	 * map for both, because {@link Tunable} keeps their names apart.
	 *
	 * <p>A flat string-keyed map rather than a field per setting: these exist to be changed while
	 * testing, and a new one should not need a codec change and a world-format migration. An entry
	 * nothing recognises is simply never read.
	 */
	public int setting(Tunable subject, LegendarySetting setting) {
		return settings.getOrDefault(key(subject, setting), setting.defaultValue());
	}

	public void setSetting(Tunable subject, LegendarySetting setting, int value) {
		settings.put(key(subject, setting), value);
		setDirty();
	}

	private static String key(Tunable subject, LegendarySetting setting) {
		return subject.name() + "." + setting.commandName();
	}

	/** The overworld, which is where this state lives regardless of who is asking. */
	public static ServerLevel home(MinecraftServer server) {
		return server.getLevel(Level.OVERWORLD);
	}

	public static LegendaryState get(MinecraftServer server) {
		return home(server).getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean crafted(Legendary legendary) {
		return crafted.contains(legendary);
	}

	public void markCrafted(Legendary legendary) {
		if (crafted.add(legendary)) {
			setDirty();
		}
	}

	/** Set when the pedestal is raised, on a world's first tick; null only before that. */
	public BlockPos pedestalPos() {
		return pedestalPos;
	}

	public void setPedestalPos(BlockPos pos) {
		this.pedestalPos = pos;
		setDirty();
	}

	public boolean onPedestal(Legendary legendary) {
		return onPedestal.contains(legendary);
	}

	/** Everything currently standing on the pedestal, in enum order. */
	public Set<Legendary> onPedestal() {
		return EnumSet.copyOf(onPedestal);
	}

	public void setOnPedestal(Legendary legendary, boolean present) {
		boolean changed = present ? onPedestal.add(legendary) : onPedestal.remove(legendary);
		if (changed) {
			setDirty();
		}
	}
}
