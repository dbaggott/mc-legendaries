package io.dnbg.minecraft.legendaries;

import io.dnbg.minecraft.legendaries.spear.SpearCommand;
import io.dnbg.minecraft.legendaries.spear.SpearRules;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point. The mod is common rather than client-only: every rule it adds — recipes, loot,
 * world-persisted item state — is decided on the logical server, so a dedicated server runs it with
 * no client involved.
 *
 * <p>Keep this thin. Each legendary item owns its own registration and wiring and is called
 * from here.
 */
public class Legendaries implements ModInitializer {
	public static final String MOD_ID = "legendaries";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		SpearRules.register();
		SpearCommand.register();
		LOGGER.info("Loaded {}", MOD_ID);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
