# Legendaries

[![Build](https://img.shields.io/github/actions/workflow/status/dbaggott/mc-legendaries/ci.yml?branch=main&logo=github&label=build)](https://github.com/dbaggott/mc-legendaries/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/dbaggott/mc-legendaries)](LICENSE)

A Minecraft mod (Fabric) that adds **legendary** items: unique, one-per-world
artifacts with their own crafting recipes, their own rules, and no other way to
obtain them.

One legendary exists so far: the **Netherite Spear**.

The mod is **common**, not client-only — every rule it adds is decided on the
logical server, so it installs on a dedicated server and vanilla clients can
connect.

## The Netherite Spear

One per world, and the only spear obtainable at all.

**Getting it.** Every vanilla spear recipe is gone — the six shaped crafting
recipes and the netherite smithing transform — replaced by a single crafting
recipe of Trial Chamber and Nether endgame loot:

|   |   |   |
|---|---|---|
| Smithing template | Netherite ingot | Heavy core |
| Ominous trial key | Breeze rod | Netherite ingot |
| Breeze rod | Ominous trial key | Smithing template |

Like every vanilla shaped recipe it also accepts the left-right mirror of that
grid. Crafting it once consumes the recipe for that world: the result still
previews, but it cannot be taken again.

**Carrying it.** It is unbreakable, comes enchanted with **Lunge III,
Sharpness V, Fire Aspect II and Looting III** — the vanilla maximum of each —
and grants **Speed II** while it is in your inventory or your hand.

**Keeping it.** It cannot be put into any container: chests, barrels, ender
chests, shulker boxes, crafters, bundles, shelves, decorated pots, item frames,
armor stands. Hoppers will not take it either, so the rule cannot be routed
around with redstone, and a crafter cannot make one. Dropping it, dying with it
and handing it to another player all work normally.

**Losing it.** You cannot. It is fire- and lava-immune already, because vanilla
registers `netherite_spear` as fire-resistant. Anything that would genuinely
destroy it — an explosion, a cactus, the void — or leaving it on the ground
until it despawns puts it back on its **pedestal**: a non-collidable display
entity that stands at world spawn from the moment a world is created, empty
until the spear comes home, and can be moved afterwards. Right-click it to take
the spear back.

**Spears elsewhere.** No spear drops from any loot table any more, and no mob
drops the one it was holding — mob equipment drops bypass loot tables entirely,
so that took its own hook. Mobs still spawn with spears and fight with them; the
vanilla spear AI is untouched. Mobs are refused the legendary spear, and if one
gets hold of it regardless it returns to the pedestal when that mob dies or
despawns rather than leaving with it.

## Admin

```
/legendaries pedestal where      # where it is, and whether the spear is on it
/legendaries pedestal here       # move it to where you are standing
/legendaries pedestal at <x y z> # move it to a specific block
```

Requires permission level 2. Moving an occupied pedestal carries the spear.

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

The claimed range holds for the things this mod depends on, which is not the
same as nothing having moved. Each line ships the same seven spear recipes and
the same `lunge.json`, and every class the mixins target — including the two
private fields the pedestal reaches for — is byte-identical across 26.1,
26.1.2, 26.2 and the 26.3 snapshot.

Plenty else did move. The entity-type constants are the example that bit:
`EntityType.BLOCK_DISPLAY` in 26.1 became `EntityTypes.BLOCK_DISPLAY` in 26.2,
so either spelling breaks half the range. The pedestal looks its entity types up
in `BuiltInRegistries` by id instead, which is stable across all four — and the
CI matrix is what caught it.

The `fabric-api` floor is no longer provisional: the CI floor row compiles this
mod's actual API calls against 0.143.12, so the declared minimum is a tested
claim rather than a guess.

A mixin that loses its target fails at load with `"required": true`, rather
than silently doing nothing — and the non-blocking 26.3 snapshot row is there to
surface that the week it happens.

## Building

```bash
mise install          # Temurin 25
./gradlew build       # jar lands in build/libs/
./gradlew runClient   # dev client
./gradlew runServer   # dev dedicated server
```

## Releasing

Published to [Modrinth](https://modrinth.com/mod/re-legendaries) and GitHub Releases from one
workflow. To cut a release:

1. Write `.modrinth/changelogs/<new-version>.md`.
2. Bump `mod_version` in `gradle.properties` and merge to `main`.

`.github/workflows/release.yml` fires on that bump, refuses if the version did not actually change
or if the tag already exists, then builds and publishes. Forgetting step 1 fails the publish with a
message naming the missing file, so every release has written notes by construction. The Actions tab
also has a manual `Run workflow` for retries, which skips the gate.

`.modrinth/` is the source of truth for the project page — description, metadata, icon and gallery.
Changing anything under it on `main` syncs Modrinth to match, so the web UI is a read-through of the
repo rather than somewhere state is edited. The icon and gallery steps skip when the files are
absent, which they currently are.

Both workflows need a `MODRINTH_API_KEY` repository secret.

## License

MIT — see [LICENSE](LICENSE).
