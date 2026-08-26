# Legendaries

Adds **legendary** items to Minecraft: unique, one-per-world artifacts with their own crafting
recipes, their own rules, and no other way to obtain them.

Install it on whichever side runs the world — your own game for single-player, or the server.
**Other players need nothing.** The mod registers no custom item, block or data component, so a
vanilla client can join a server running it and see only vanilla things.

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

## Admin

```
/legendaries pedestal where      # where it is, and whether the spear is on it
/legendaries pedestal here       # move it to where you are standing
/legendaries pedestal at <x y z> # move it to a specific block
```

Requires permission level 2. Moving an occupied pedestal carries the spear with it.

## Requires

[Fabric API](https://modrinth.com/mod/fabric-api).
