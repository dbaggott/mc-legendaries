# Legendaries

[![Build](https://img.shields.io/github/actions/workflow/status/dbaggott/mc-legendaries/ci.yml?branch=main&logo=github&label=build)](https://github.com/dbaggott/mc-legendaries/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/dbaggott/mc-legendaries)](LICENSE)

A Minecraft mod (Fabric) that adds **legendary** items: unique, one-per-world
artifacts with their own rules, each defined by a single data file, and no other
way to obtain them.

Six exist so far: the **Netherite Spear**, the **Mace**, the **Dragon Egg**, the
**Legendary Pickaxe**, the **Legendary Sword** and the **Legendary Axe**.

Every legendary shares the same rules — one per world, refused by every container, never set down
as a block, and returned to a shared pedestal rather than ever being lost. What differs is how you
get it and what it does.

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
burst, flames, and a boom that carries the fireball's ignition and the hiss of the whole sphere
going out with it. The crater then pops its way quiet over about a second as it cools.

Everything inside the radius goes. What varies is the lining: each shell block becomes magma,
netherrack or coal, bar the **15%** left as it is — so the crater's edge shows the ground it was cut
from rather than a uniform coat. That share is `unmelted`, and `config` turns it.

Everything else alive inside the radius takes **two and a half hearts**, armour does not soften it,
and it is thrown clear as hard as **a stick and a half of TNT** — hardest at the centre, fading to
nothing eight blocks out, and softened by blast protection the way an explosion is. That strength is
`knockback`, counted in hundredths of a stick, and `config` turns it. You are the exception to both:
the blast is centred on you, so charging you for it would tax every use.

Bedrock and anything else vanilla marks unbreakable survives, so a blast cannot hole the world floor
or the Nether roof. Nothing drops; a sphere that size is over two hundred blocks and would bury you in items. **60 second
cooldown**, counted down above your hotbar.

The countdown is the ability's, not the mace's. Nothing is drawn on the item — the wait belongs to
the Molten Blast, so anything else that comes to carry it shares the one wait and the one countdown,
and shows it in a place a worn item could never reach. Reconnecting clears a wait.

It is centred on you and it does not care that you are standing there. Expect to fall.

## The Dragon Egg

The egg the dragon leaves, and the only legendary you do not craft. Killing the dragon for the first
time stands it on the exit portal exactly as it always did, and it comes off that portal the
legendary, shimmering as if enchanted — however you knock it loose. Punch it onto a torch, piston it
off its perch, or blow it up: all three hand you the same marked egg. One per world is the End's own
rule — only a world's first dragon leaves an egg, and re-summoning it never leaves another — so
nothing here has to enforce it.

Until you take it, it is an ordinary block sitting on the portal, because that is all vanilla ever
put there. The legendary begins when it becomes an item.

An egg that predates this mod is an ordinary egg until you set it down and take it again. The marker
is written where the block drops rather than carried by the item, so an old egg is placeable — the
placement rule only refuses one that is already a legendary — and harvesting it hands back the
marked one. That is the upgrade path for a world that already had its egg.

**Carrying it.** It gives you **five extra hearts** while it is in your inventory or your hand, and
takes them back the moment it is not — dropped, handed over, or died with. Above ten hearts when it
goes, you are back at ten. That number is `hearts`, and `config` turns it.

The new hearts arrive **empty**, the way vanilla's Health Boost does: pick the egg up on four hearts
and you are on four of fifteen, with room to heal rather than five hearts of free healing. Handing it
over still costs you whatever you were holding above ten, so dropping it and taking it back is never
a way to top yourself up.

**Putting it down.** You cannot. It is the first legendary that was ever a block, and placing one
would be a way to leave it somewhere — so every route to placing it is refused, and it says so above
your hotbar. Everything else you do while holding it is untouched: chests still open, doors still
swing, buttons still press.

## The Legendary Pickaxe

**Ordinary pickaxes are untouched.** Every vanilla pickaxe recipe still works and every pickaxe they
make is still a pickaxe — this one has a recipe of its own rather than replacing theirs. It is the
only legendary that shares its item with something you can still make normally.

**Getting it.** One crafting recipe, around a netherite pickaxe:

|   |   |   |
|---|---|---|
| Netherite ingot | Deepslate emerald ore | Netherite ingot |
| Efficiency V book | Netherite pickaxe | Efficiency V book |
| Deepslate coal ore | Sculk shrieker | Deepslate coal ore |

Every ore in it, and the shrieker, can only be picked up with Silk Touch — which is what the pickaxe
then carries, so the tool that makes one could have mined its own ingredients.

**The books are checked when you take the result, not when the recipe matches.** A vanilla ingredient
names an item type and nothing else, so the recipe can ask for an enchanted book and cannot ask what
is written in it. Two books of the wrong enchantment will assemble a pickaxe in the result slot that
refuses to come out, and says why. Like the one-per-world rule, this is a thing the recipe means and
cannot state.

**Carrying it.** It is unbreakable, comes with **Efficiency VI and Silk Touch** — a level above the
enchanting table's maximum, which only a recipe can write — and grants **Fire Resistance** for as
long as it is in your inventory or your hand.

**Gravel.** It digs gravel as fast as a shovel of the same metal would, which with Efficiency VI on
top makes it an Efficiency VI netherite shovel for that one block. Nothing else a shovel is for
changes; dirt and sand are still a pickaxe's problem. Note that Silk Touch means gravel always comes
up as gravel — this is not the tool to dig flint with.

**Keeping it.** The rules every legendary shares: refused by every container, never left on display,
and returned to the pedestal rather than lost. It is crafted, so it is one per world — the recipe
still previews after that, and refuses.

**How it looks.** The netherite pickaxe with a gem of amber set at the elbow, so the one pickaxe
that shares its item with an ordinary one does not share its appearance. The mod offers a small resource pack when you join and
the item carries a `minecraft:custom_model_data` string; the pack's item definition selects on that
string and names the vanilla model as its fallback, so the texture only ever replaces *this*
pickaxe. Decline the pack, or play without it, and you see a netherite pickaxe — nothing else
changes, and nothing renders wrong. See [Textures](#textures).

## The Legendary Sword and the Legendary Axe

**Ordinary swords and axes are untouched.** Like the pickaxe and unlike the spear, each has a recipe
of its own rather than replacing vanilla's, so every netherite sword and axe you could make before
you can still make now.

**One recipe shape, two items.** Both are built from what the game's three bosses leave behind, with
the tool being upgraded at the centre:

|   |   |   |
|---|---|---|
| Dragon breath | Dragon head | Dragon breath |
| Echo shard | *Netherite sword or axe* | Echo shard |
| Nether star | Sharpness V book | Nether star |

Two nether stars means two withers for each, and the dragon head means an End city — these sit past
everything else in the mod for cost.

**The book is checked when you take the result.** A recipe can ask for an enchanted book and cannot
ask what is written in it, so a book of the wrong enchantment assembles a result that refuses to
come out and says why. The same condition the pickaxe's two books carry.

**The Sword.** Unbreakable, **Sharpness VIII, Fire Aspect III and Looting III**, and **Speed** while
it is in your inventory or your hand. It is deliberately the hardest-hitting thing in the mod: the
spear carries the vanilla ceiling of Sharpness V, and this goes three past it.

**The Axe.** Unbreakable, **Sharpness VIII, Silk Touch and Efficiency V**, and **Strength** while
carried. Silk Touch on an axe is not idle — it is what takes a bee nest with the bees still in it.
Efficiency V rather than the pickaxe's VI, so the pickaxe stays the better tool for what a pickaxe
is for.

**They keep the rules the others do** — refused by every container, never left on display, returned
to the pedestal rather than lost, and one per world each.

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
/legendaries config get <subject>                # what that subject is tuned to
/legendaries config set <subject> <setting> <n>  # cooldown (seconds), radius (blocks),
                                                 #   unmelted (percent), knockback (percent),
                                                 #   hearts
```

`<subject>` is `molten_blast` or `dragon_egg`, tab-completed; so is `<setting>`, and it offers only
the knobs that subject actually has.

A knob names whatever it belongs to, which is not always the item in your hand. The blast's four
belong to the **ability** rather than to the mace carrying it, because two carriers of one ability
tune together. `hearts` belongs to the **legendary**, because what an item grants merely by being
carried has no ability to belong to. Anything with no knobs at all — the spear, the mace — is not a
subject and is not offered.

**`config` is a testing tool.** Retuning a blast is a command and a swing rather than an edit, a
rebuild and a relaunch. Values persist with the world, and `radius` is capped at 16 because cost
grows with its cube. `cooldown` is measured from your last swing against whatever the setting says
now, so a new value reaches a wait already counting down — shortening it past the time already
elapsed ends that wait there and then, and lengthening it extends the same wait. `hearts` reaches an
egg already in a pocket the same way, within a second, rather than only the next one picked up.

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

Every legendary shares one pedestal, each in its own slot. Right-clicking takes one at random from
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

## Textures

The Legendary Pickaxe has a texture of its own. It ships as a resource pack rather than as assets
inside the jar, because the jar's assets would only reach players who installed the mod — and no
other player has to.

The texture is the vanilla netherite pickaxe with a gem set into it, and it lives under this mod's
own namespace rather than overwriting `minecraft:item/netherite_pickaxe`. Overwriting that file
would retexture *every* netherite pickaxe in the game, which is the one thing this legendary must
not do.

**How it reaches a player.** The mod answers vanilla's own "does this server serve a resource pack?"
question with the pack published alongside the jar, and the server offers it during the configuration
phase, before the player is in the world. It is never sent as *required*: a player can decline, or
turn server packs off in their options, and simply sees the vanilla pickaxe. Because it is the
vanilla mechanism, an unmodded client handles it like any other server pack.

**Turning it off.** Set `resource-pack` in `server.properties` to a pack of your own and that answer
stands — the mod only fills the silence, it does not overrule a server that has already chosen.

**Why nothing breaks without it.** The pack does not introduce a new item model; it overrides the
vanilla netherite pickaxe's item definition with a `minecraft:select` on
`minecraft:custom_model_data`, and every other netherite pickaxe falls through to the same vanilla
model it always had. A client that never applies the pack has no override to consult at all, which is
why declining costs nothing and why the component is safe to put on the item for everyone.

The pack's `when` string and the `minecraft:custom_model_data` on the recipe result are the two ends
of one match, in `src/main/resourcepack/assets/minecraft/items/netherite_pickaxe.json` and
`src/main/resources/data/legendaries/recipe/legendary_pickaxe.json`. They are changed together.

The same pairing repeats for the Legendary Sword and the Legendary Axe: one item definition each,
one marker each, one texture each. Every ordinary netherite sword, axe and pickaxe falls through to
the vanilla model it always had.

**Single-player too.** The integrated server answers the same question the dedicated one does, so
your own world offers you the pack the same way. To apply it by hand instead, the zip is on the
release next to the jar and goes in `.minecraft/resourcepacks`.

**A pickaxe crafted before this keeps the old look.** An item's components are fixed when it is
made, and the marker is written by the recipe — so it rides on pickaxes crafted from here on, and a
world whose pickaxe already exists goes on seeing a netherite one. Nothing rewrites an item a player
is already holding.

## Target toolchain

| Tooling | Version |
|---|---|
| Minecraft (runtime) | 26.1 through the whole 26.3 line — one jar for all |
| Minecraft (build target) | 26.2 by default; CI rebuilds the same source against 26.1, 26.1.2, and a 26.3 snapshot |
| Resource pack format | 84 through 94 — 26.1's through the 26.3 snapshot's |
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
