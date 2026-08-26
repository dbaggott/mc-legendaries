package io.dnbg.minecraft.legendaries.spear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.dnbg.minecraft.legendaries.Legendaries;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The per-world facts about the spear: whether it has been crafted, and where its pedestal is.
 *
 * <p>Stored on the OVERWORLD only, and read from there no matter which dimension asks. A
 * {@code SavedDataStorage} is per-level, so keeping this per-dimension would give the Nether its
 * own spear — "one in the entire world" has to mean one across every dimension.
 *
 * <p>The pedestal position is <em>stored</em> rather than derived from world spawn. World spawn is
 * only where it is sited on a world's first tick; an admin can move it afterwards, and moving world
 * spawn later must not drag the pedestal along with it.
 */
public class SpearState extends SavedData {
	private static final Codec<SpearState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("crafted").orElse(false).forGetter(state -> state.crafted),
			BlockPos.CODEC.optionalFieldOf("pedestal").forGetter(state -> Optional.ofNullable(state.pedestalPos)),
			Codec.BOOL.fieldOf("spear_on_pedestal").orElse(false).forGetter(state -> state.spearOnPedestal))
			.apply(instance, SpearState::new));

	private static final SavedDataType<SpearState> TYPE = new SavedDataType<>(
			Legendaries.id("spear"), SpearState::new, CODEC, DataFixTypes.LEVEL);

	private boolean crafted;
	private BlockPos pedestalPos;
	private boolean spearOnPedestal;

	public SpearState() {
	}

	private SpearState(boolean crafted, Optional<BlockPos> pedestalPos, boolean spearOnPedestal) {
		this.crafted = crafted;
		this.pedestalPos = pedestalPos.orElse(null);
		this.spearOnPedestal = spearOnPedestal;
	}

	/** The overworld, which is where this state lives regardless of who is asking. */
	public static ServerLevel home(MinecraftServer server) {
		return server.getLevel(Level.OVERWORLD);
	}

	public static SpearState get(MinecraftServer server) {
		return home(server).getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean crafted() {
		return crafted;
	}

	public void markCrafted() {
		this.crafted = true;
		setDirty();
	}

	/** Set when the pedestal is raised, on a world's first tick; null only before that. */
	public BlockPos pedestalPos() {
		return pedestalPos;
	}

	public void setPedestalPos(BlockPos pos) {
		this.pedestalPos = pos;
		setDirty();
	}

	public boolean spearOnPedestal() {
		return spearOnPedestal;
	}

	public void setSpearOnPedestal(boolean onPedestal) {
		this.spearOnPedestal = onPedestal;
		setDirty();
	}
}
