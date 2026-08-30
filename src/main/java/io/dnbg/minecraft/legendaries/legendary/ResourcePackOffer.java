package io.dnbg.minecraft.legendaries.legendary;

import io.dnbg.minecraft.legendaries.Legendaries;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.MinecraftServer.ServerResourcePackInfo;

/**
 * The resource pack this mod offers a joining client, and the one place that decides whether to
 * offer it.
 *
 * <p>The Legendary Pickaxe is a vanilla netherite pickaxe carrying a {@code
 * minecraft:custom_model_data} string. Unlike the {@code minecraft:custom_data} marker every rule in
 * the mod reads, that component <em>is</em> network-synchronized, so every client is told about it.
 * What turns it into a different-looking item is the pack, whose item definition selects on the
 * string and names the vanilla model as its fallback: a client that never applies the pack has no
 * override to consult and renders the ordinary pickaxe. That is why this offer can be declined
 * without leaving anything broken on screen, and why it is never sent as required — see {@code
 * src/main/resourcepack/assets/minecraft/items/netherite_pickaxe.json}.
 *
 * <p>Vanilla asks {@link MinecraftServer#getServerResourcePack()} once per connection, during the
 * configuration phase, and builds both the push packet and the wait for the client's answer out of
 * what it gets back. Answering that one question is the whole of the integration — {@link
 * io.dnbg.minecraft.legendaries.mixin.MinecraftServerMixin} and {@link
 * io.dnbg.minecraft.legendaries.mixin.DedicatedServerMixin} are what route it here.
 */
public final class ResourcePackOffer {
	/**
	 * Where the built pack is published, and what it hashes to.
	 *
	 * <p>Written by the build: the zip is assembled and hashed before the jar is packed, because the
	 * server has to name a URL and a hash for a file that does not exist yet at runtime and cannot
	 * be asked for either. Absent only if the jar was packed wrong, which is why failing to read it
	 * offers nothing rather than guessing.
	 *
	 * <p>Blank rather than absent in a jar that was not built to be released — a local or CI build,
	 * or a dev run. The URL would name an asset of a release only that build produces, so pointing
	 * clients at it means a failed download for every one of them; offering nothing is what a jar
	 * with no published pack behind it should do.
	 */
	private static final String PROPERTIES = "/legendaries-resourcepack.properties";

	/**
	 * Stable across joins and across versions, so a returning client replaces the copy it already
	 * has rather than accumulating another. Derived rather than written out so it cannot be typed
	 * differently in two places.
	 */
	private static final UUID ID =
			UUID.nameUUIDFromBytes("legendaries:resourcepack".getBytes(StandardCharsets.UTF_8));

	private static final Component PROMPT = Component.literal(
			"Legendaries gives the Legendary Pickaxe a look of its own. "
					+ "Decline and it renders as an ordinary netherite pickaxe.");

	private static final Optional<ServerResourcePackInfo> OFFER = load();

	private ResourcePackOffer() {
	}

	/**
	 * What {@link MinecraftServer#getServerResourcePack()} should answer, given what the server
	 * itself decided.
	 *
	 * <p>An operator who set {@code resource-pack} in {@code server.properties} has already said
	 * which pack this server serves, and that answer stands. Ours fills a silence; it does not
	 * overrule a choice — which is also the way to turn it off.
	 */
	public static Optional<ServerResourcePackInfo> resolve(Optional<ServerResourcePackInfo> configured) {
		return configured.isPresent() ? configured : OFFER;
	}

	private static Optional<ServerResourcePackInfo> load() {
		Properties properties = new Properties();
		try (InputStream in = ResourcePackOffer.class.getResourceAsStream(PROPERTIES)) {
			if (in == null) {
				Legendaries.LOGGER.error("{} is missing from the jar; offering no resource pack", PROPERTIES);
				return Optional.empty();
			}
			properties.load(in);
		} catch (IOException e) {
			Legendaries.LOGGER.error("Could not read {}; offering no resource pack", PROPERTIES, e);
			return Optional.empty();
		}
		String url = properties.getProperty("url");
		String sha1 = properties.getProperty("sha1");
		if (url == null || sha1 == null) {
			Legendaries.LOGGER.error("{} names no url and sha1; offering no resource pack", PROPERTIES);
			return Optional.empty();
		}
		// Said out loud rather than passed over, because the alternative to a client seeing a failed
		// download is a release that quietly never offers the pack it published.
		if (url.isBlank() || sha1.isBlank()) {
			Legendaries.LOGGER.info("Not a release build, so no resource pack is offered; "
					+ "the Legendary Pickaxe renders as an ordinary netherite pickaxe");
			return Optional.empty();
		}
		return Optional.of(new ServerResourcePackInfo(ID, url, sha1, false, PROMPT));
	}
}
