# Legendaries

[![Build](https://img.shields.io/github/actions/workflow/status/dbaggott/mc-legendaries/ci.yml?branch=main&logo=github&label=build)](https://github.com/dbaggott/mc-legendaries/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/dbaggott/mc-legendaries)](LICENSE)

A Minecraft mod (Fabric) that adds **legendary** items: unique, one-per-world
artifacts with their own crafting recipes, their own rules, and no other way to
obtain them.

The mod is **common**, not client-only — every rule it adds is decided on the
logical server, so it installs on a dedicated server and vanilla clients can
connect.

## Status

**Nothing is implemented yet.** This is the build scaffold: the mod loads, logs
its name, and registers no content. What follows is the agreed design for the
first legendary, not a description of shipped behaviour.

## Planned: the Netherite Spear

One per world, and the only spear obtainable at all.

- Every vanilla spear recipe is removed — the six shaped crafting recipes and
  the netherite smithing transform — and replaced by a single new crafting
  recipe that yields the Netherite Spear.
- Spears are removed from the six vanilla loot tables that carry them
  (`bastion_treasure`, `buried_treasure`, `end_city_treasure`,
  `underwater_ruin_big`, `underwater_ruin_small`, `village_weaponsmith`).
- Crafting it once permanently consumes the recipe for that world.
- It is unbreakable, and comes pre-enchanted with **Lunge III, Sharpness V,
  Fire Aspect II, Looting III** — the vanilla maximum of each.
- Carrying it grants **Speed II**, in hand or in inventory.
- It cannot be put into any container: chests, barrels, ender chests, shulker
  boxes, hoppers, item frames, armor stands. The attempt is refused rather than
  punished.
- It cannot be permanently lost. If it is destroyed (lava, fire, cactus,
  explosion, the void) or despawns after being left on the ground, it
  rematerialises on its **pedestal** — a non-collidable display entity placed
  at world spawn on first use, relocatable afterwards by an admin command.

## Installing

Drop the jar in a `mods` folder alongside [Fabric API](https://modrinth.com/mod/fabric-api).
Which folder depends on where the world lives, because every rule this mod adds
is decided by whichever side is running the world:

- **Single-player** — your own `.minecraft/mods`. The integrated server runs it.
- **On a server** — the *server's* `mods` folder. Installing it only on your
  client does nothing on someone else's world, and is harmless.

Other players need nothing. The mod registers no custom item, block or data
component, so a vanilla client sees only vanilla things and can connect to a
server running it.

## Target toolchain

| Tooling | Version |
|---|---|
| Minecraft (runtime) | 26.1 through the whole 26.3 line — one jar for all |
| Minecraft (build target) | 26.2 by default; CI rebuilds the same source against 26.1, 26.1.2, and a 26.3 snapshot |
| Fabric Loader | 0.19.3 |
| Fabric Loom | 1.17.17 |
| JDK | Temurin 25 (pinned via `mise.toml`) |
| Gradle | 9.x via wrapper |

The claimed range holds because spears and the Lunge enchantment are present
unchanged across all four lines — each ships the same seven spear recipes and
the same `lunge.json`. The declared `fabric-api` floor is provisional: no
Fabric API call exists yet, so it is pinned to the oldest release built for
26.1 rather than to a tested requirement. It gets a real reason when the first
API use lands.

## Building

```bash
mise install          # Temurin 25
./gradlew build       # jar lands in build/libs/
./gradlew runClient   # dev client
./gradlew runServer   # dev dedicated server
```

## License

MIT — see [LICENSE](LICENSE).
