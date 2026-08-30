# Legendaries

Adds **legendary** items to Minecraft: unique, one-per-world artifacts with their own rules, each
defined by a single data file, and no other way to obtain them. Six exist so far — the **Netherite
Spear**, the **Mace**, the **Dragon Egg**, the **Legendary Pickaxe**, the **Legendary Sword** and the
**Legendary Axe**. Every one is one per world,
refused by every container, never set down as a block, and returned to a shared pedestal rather than
ever being lost.

Install it on whichever side runs the world — your own game for single-player, or the server.
**Other players need nothing.** The mod registers no custom item, block or data component, so a
vanilla client can join a server running it and see only vanilla things.

## The Mace

Crafted by the ordinary vanilla recipe — a heavy core over a breeze rod. The ingredients and pattern
are untouched; only the result is the legendary, so there is one mace per world and no plain ones.
It is unbreakable.

### Molten Blast

**Sneak and right-click** to erase every block within four of you — all directions, including
straight down — and turn the shell left behind into molten rock, with an explosion burst and flames.
Everything inside the radius goes; each shell block becomes magma, netherrack or coal, bar the
**15%** left as it is, which keeps the crater's edge showing the ground it was cut from. Bedrock
and anything else vanilla marks unbreakable survives, so a blast cannot hole the world floor or the
Nether roof.
Nothing drops. Sixty second cooldown.

It is centred on you and does not care that you are standing there. Expect to fall.

## The Netherite Spear

One per world, and the only spear obtainable at all.

### Getting it

Every vanilla spear recipe is gone — the six shaped crafting recipes and the netherite smithing
transform — replaced by a single crafting recipe of Trial Chamber and Nether endgame loot:

|   |   |   |
|---|---|---|
| Smithing template | Netherite ingot | Heavy core |
| Ominous trial key | Breeze rod | Netherite ingot |
| Breeze rod | Ominous trial key | Smithing template |

Craft it once and the recipe is spent for that world. No spear drops from any loot table any more,
and no mob drops the one it is holding — though mobs still spawn with spears and fight with them,
so the vanilla spear AI is untouched.

### Carrying it

It is unbreakable, comes enchanted with **Lunge III, Sharpness V, Fire Aspect II and Looting III** —
the vanilla maximum of each — and grants **Speed II** while it is in your inventory or your hand.

### Keeping it

It cannot be put into any container: chests, barrels, ender chests, shulker boxes, crafters,
bundles, shelves, decorated pots, item frames, armor stands. Hoppers will not take it either, so the
rule cannot be routed around with redstone, and a crafter cannot make one. Mobs are refused it too.

Dropping it, dying with it and handing it to another player all work normally.

### Losing it

You cannot. It is fire- and lava-immune already, because a netherite spear is. Anything that would
genuinely destroy it — an explosion, a cactus, the void — or leaving it on the ground until it
despawns puts it back on its **pedestal**: a non-collidable display that stands at world spawn from
the moment a world is created, empty until the spear comes home. Right-click it to take the spear
back.

If a mob gets hold of it despite the refusals, it returns to the pedestal when that mob dies or
despawns rather than leaving with it.

## The Dragon Egg

The egg the dragon leaves, and the only legendary you do not craft. It comes off the exit portal
already legendary and shimmering, however you knock it loose — onto a torch, with a piston, or with
TNT. One per world is the End's own rule: only a world's first dragon leaves an egg.

**It cannot be placed as a block.** Setting it down would be a way to leave it somewhere, so every
route to placing one is refused. Chests, doors and everything else still work while you hold it.

**Carrying it gives you five extra hearts**, lost the moment it leaves your hands by any route. They
arrive empty, the way Health Boost's do, so passing the egg around is never a way to top anybody up.
How many hearts is a setting.

## The Legendary Pickaxe

**Ordinary pickaxes are untouched** — this has a recipe of its own rather than replacing theirs, the
way the Legendary Sword and Legendary Axe do.

**It looks the part.** A gem of molten amber set into the netherite, so a legendary that shares its
item with an ordinary tool does not share its appearance. That arrives as a small resource pack the
server offers you when you join — optional, and declining it costs nothing but the look: the pack
retextures only the legendaries, and every ordinary pickaxe, sword and axe is untouched either way.

|   |   |   |
|---|---|---|
| Netherite ingot | Deepslate emerald ore | Netherite ingot |
| Efficiency V book | Netherite pickaxe | Efficiency V book |
| Deepslate coal ore | Sculk shrieker | Deepslate coal ore |

Every ore in it, and the shrieker, are Silk Touch drops — which is what the pickaxe then carries, so
the tool that makes one could have mined its own ingredients.

It is unbreakable, comes with **Efficiency VI and Silk Touch** — a level past what an enchanting
table will give you — and grants **Fire Resistance** for as long as you are carrying it. It digs
gravel as fast as a netherite shovel does, which with Efficiency VI on top makes it an Efficiency VI
shovel for that one block.

## The Legendary Sword and the Legendary Axe

**Ordinary swords and axes still work.** Each has its own recipe rather than replacing vanilla's, so
nothing you could make before is gone.

**One recipe, two items** — the far ends of the game, around the tool you are upgrading:

|   |   |   |
|---|---|---|
| Dragon breath | Dragon head | Dragon breath |
| Echo shard | *Netherite sword or axe* | Echo shard |
| Nether star | Sharpness V book | Nether star |

Two nether stars each means two withers each, the dragon head is mined off an End city ship, and the
echo shards come out of ancient city chests. The book is checked when you take the result, so the
wrong enchantment gives you a result that refuses to come out and tells you why.

**The Sword** is unbreakable, carries **Sharpness VIII, Fire Aspect III and Looting III**, and gives
**Speed** while you hold or carry it. Sharpness VIII kills faster than anything else here — though
the Axe, starting two damage higher, lands the bigger single hit.

**The Axe** is unbreakable, carries **Sharpness VIII, Silk Touch and Efficiency V**, and gives
**Strength**. Silk Touch on an axe takes a bee nest with the bees still inside.

Both get a look of their own from the same optional resource pack the pickaxe uses.

## Admin

```
/legendaries pedestal where                      # where it is, and what is standing on it
/legendaries pedestal here                       # move it to where you are standing
/legendaries pedestal at <x y z>                 # move it to a specific block

/legendaries item give <players|pedestal> <name>    # hand one out, or stand it in the case
/legendaries item delete <players|pedestal> <name>  # take every copy back

/legendaries config get <subject>                # what that subject is tuned to
/legendaries config set <subject> <setting> <n>  # turn one of its knobs
```

Requires permission level 2. Moving an occupied pedestal carries whatever is on it. Every legendary
shares the one pedestal, each in its own slot; right-clicking takes one back.

## Requires

[Fabric API](https://modrinth.com/mod/fabric-api).
