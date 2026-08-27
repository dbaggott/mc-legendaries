# Legendaries

[![Build](https://img.shields.io/github/actions/workflow/status/dbaggott/mc-legendaries/ci.yml?branch=main&logo=github&label=build)](https://github.com/dbaggott/mc-legendaries/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/dbaggott/mc-legendaries)](LICENSE)

A Minecraft mod (Fabric) that adds **legendary** items: unique, one-per-world
artifacts with their own crafting recipes, their own rules, and no other way to
obtain them.

Two exist so far: the **Netherite Spear** and the **Mace**.

Every legendary shares the same rules — one per world, refused by every container, and returned to
a shared pedestal rather than ever being lost. What differs is how you get it and what it does.

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
until it despawns puts it back on its **pedestal**: a non-collidable stone
plinth that stands at world spawn from the moment a world is created, empty
until something comes home, and can be moved afterwards. Right-click the glass to
take one back.

A purple glass case rises on top whenever the pedestal is holding something,
narrower than the cap so it reads as set down there, and what it holds turns
slowly inside it, rendered with the item's own dropped-item model. Claim the last
legendary off the pedestal and the case comes down with it, leaving the bare
plinth waiting.

The plinth is display entities rather than placed blocks, so there is nothing to
mine, nothing to grief and nothing to walk into.

**Spears elsewhere.** No spear drops from any loot table any more, and no mob
drops the one it was holding — mob equipment drops bypass loot tables entirely,
so that took its own hook. Mobs still spawn with spears and fight with them; the
vanilla spear AI is untouched. Mobs are refused the legendary spear, and if one
gets hold of it regardless it returns to the pedestal when that mob dies or
despawns rather than leaving with it.

## The Mace

Crafted by the ordinary vanilla recipe — a heavy core over a breeze rod. The recipe's ingredients
and pattern are untouched; only its result is the legendary, so there is one mace per world and no
plain ones. It is unbreakable.

### Molten Blast

**Sneak and right-click** to erase every block within four of you — in all directions, including
straight down — and turn the shell left behind into molten rock. It goes off with an explosion
burst, flames and a boom, and the crater crackles for a couple of seconds as it cools.

Everything inside the radius goes. What varies is the lining: each shell block becomes magma,
netherrack or coal, bar the **15%** left as it is — so the crater's edge shows the ground it was cut
from rather than a uniform coat. That share is `unmelted`, and `config` turns it.

Everything else alive inside the radius takes **two and a half hearts**, armour does not soften it,
and it is thrown clear as hard as **two sticks of TNT** — hardest at the centre, fading to nothing
eight blocks out, and softened by blast protection the way an explosion is. That strength is
`knockback`, counted in hundredths of a stick, and `config` turns it. You are the exception to both:
the blast is centred on you, so charging you for it would tax every use.

Bedrock and anything else vanilla marks unbreakable survives, so a blast cannot hole the world floor
or the Nether roof. Nothing drops; a sphere that size is over two hundred blocks and would bury you in items. **60 second
cooldown**, shown on the item.

It is centred on you and it does not care that you are standing there. Expect to fall.

## Admin

```
/legendaries pedestal where               # where it is, and what is standing on it
/legendaries pedestal here                # move it to where you are standing
/legendaries pedestal at <x y z>          # move it to a specific block

/legendaries item give <players> <name>    # hand out a legendary
/legendaries item give pedestal <name>     # stand one in the case
/legendaries item delete <players> <name>  # take every copy back
/legendaries item delete pedestal <name>   # destroy the one in the case
```

```
/legendaries config get <name>                # what its ability is tuned to
/legendaries config set <name> <setting> <n>  # cooldown (seconds), radius (blocks),
                                              #   unmelted (percent), knockback (percent)
```

`<name>` is `netherite_spear` or `mace`, tab-completed; so is `<setting>`.

**`config` is a testing tool.** Retuning a blast is a command and a swing rather than an edit, a
rebuild and a relaunch. Values persist with the world, `radius` is capped at 16 because cost grows
with its cube, and a legendary with no ability says so rather than storing a number nothing reads.
`cooldown` is measured from your last swing against whatever the setting says now, so a new value
reaches a wait already counting down — shortening it past the time already elapsed ends that wait
there and then, and lengthening it can put the mace back on cooldown after the old wait had run
out.

**`item give` ignores the one-per-world rule** — that is what it is for, whether you are recovering
a legendary lost to something the backstop could not catch or testing a change. It does not mark the
world as having crafted one, so the crafting route stays open: a given copy is a copy, not the craft.
The item is built by assembling that legendary's own recipe, so it is identical to a crafted one.

If a duplicate is lost while the original is already home, it drops beside the pedestal rather than
onto it — there is one display slot per legendary, and deleting the extra would make the command a
way to destroy what it just handed out.

**`pedestal` is a target on both**, wherever a player selector goes. `give pedestal` stands a
legendary in the case, and is refused rather than dropped in the grass if that one is already
standing there — there is a single slot per legendary. `delete pedestal` destroys what is in that
slot and leaves the plinth — and refuses while the pedestal has not loaded, rather than clearing a
slot it cannot see and stranding what is standing in it.

Requires permission level 2. Moving an occupied pedestal carries whatever is on it.

Both legendaries share one pedestal, each in its own slot. Right-clicking takes one at random from
whatever is standing there.

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

`runClient` compiles the current source itself, so `build` beforehand is redundant — reach for
`build` when you want the jar.

### Sharing one world across worktrees

The dev run directory defaults to `run/` beside whatever you launched, so each worktree gets its own
empty world and its own keybinds. To point every worktree on a machine at one directory, set an
absolute path in **`~/.gradle/gradle.properties`** — a user-level file, so the path never reaches the
repo:

```
legendaries_run_dir=/Users/you/mc-legendaries-run
```

A clone that sets nothing keeps the ordinary `run/`. Note that two run configurations cannot share
the directory simultaneously: a client and a server both launched against it contend for the same
world lock.

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
