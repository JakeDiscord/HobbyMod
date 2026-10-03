# Bonsai MVP

Craft a Bonsai Pot from five bricks and one dirt block:

```text
Brick   empty   Brick
Brick   Dirt    Brick
```

The pot is also in the separate **HobbyMod: Bonsai** creative tab.
Place it and use an oak, birch or cherry sapling to start a tree. Break a pot
normally to carry its saved tree, including age, health, branch shape and wiring.
Pots stack to one so individual trees remain identifiable; carried trees show
species, age and health in their tooltip.

| Action | Control |
| --- | --- |
| Inspect care and age | Right-click with an empty hand |
| Water | Right-click with a water bucket; survival returns the empty bucket |
| Remove leaves, then prune the branch | Aim shears at a branch: first click removes foliage from it and its twigs; second click cuts the branch |
| Wire and bend a branch | Aim at a branch and right-click with a copper ingot |
| Reverse both the turn and outward bend | Sneak while using copper |
| Prune roots | Sneak and right-click with shears |
| Repot with fresh soil | Right-click with dirt |

Branch selection follows the player's viewing ray through the pot's outline.
The base trunk is protected from cutting, but its foliage can be removed.
Defoliation is saved on every affected node and survives moving/reloading the
tree. First-click leaf removal preserves all wood and costs 2 health; new buds
can produce new foliage. A second click still cuts the selected branch and its
descendants, even if a new twig has since grown. Every wiring operation uses one copper ingot and
adds 12 degrees of bend; the seventh bend on a branch breaks that subtree.
Shears consume durability in survival. Creative mode preserves materials/tools.
Root pruning and repotting each require at least 12 growth intervals since the
previous corresponding action. Pruning roots costs health; repotting refreshes
soil and sets water to 55%. Root pruning does not replace the soil.

Care updates once per 1,200 consecutive loaded ticks (one minute at 20 TPS),
adding a bud/branch about every two intervals when healthy. Each tree starts
with two trunk segments and grows to at most 28 branch segments. Initial trunk
segments and every new branch unfold continuously over 1,200 loaded ticks,
with partial-tick rendering between ticks. Every generation starts with zero
visible length, radius and leaf size; later branches growing from branches use
the same one-minute progression as the planted tree. Growth progress is saved in each
node, so unloading or carrying the tree pauses growth rather than completing it. Oak and cherry
spread out more than birch. Leave room and use adequate daylight or block light
(level 9 or greater above the pot). Water between 15% and 85% is healthy; watering
above 75% already causes stress. Fresh soil lasts 40 intervals; roots become
crowded after 60. Repeated pruning, excessive bending, neglect and overwatering
reduce health. Stressed foliage turns brown and disappears at zero health;
proper care can restore it. Each minute, healthy conditions add 2 health
points (capped at 100); any stress condition subtracts 4 points total. Branch
pruning costs 6 points per removed segment, wiring costs 1, root pruning costs
8, repotting costs 4, and watering when already above 75% costs 12. Good care
can restore health even from zero; new branches require at least 35 health.
The compact live overlay shows age, water and health while aiming at the pot.
Recovery or stress appears only when relevant; sneak to see soil/root ages.
It reflects other players' care too. Soil darkens continuously with its water
content and dries back out as water drops. Routine care no longer duplicates
the overlay with a long action-bar message; tool feedback is brief. Sneak gestures for
copper and shears explicitly allow block interaction, so Minecraft does not
bypass the pot while sneaking. No automatic harvesting or Create automation is
provided: these are individual ornamental trees for tending and display.

## Persistence and multiplayer

Gameplay runs on the server. Nodes store stable ID/parent, normalized segment
length/radius, orientation, age, health, bud state, wire state and cumulative
bend. Parent IDs must precede children. Invalid links, duplicates, nonfinite
geometry and excess nodes are rejected on load. Traversals are bounded and
nonrecursive. Growth freezes while unloaded; reload starts a fresh growth
interval without a catch-up burst. Growth uses game time rather than daylight,
so `/time set` does not age trees. Discontinuous or backwards game time resets
the interval.

This MVP uses vanilla block-entity update packets containing a bounded full
snapshot (at most 28 nodes) after care or coarse growth updates. Chunk loading
also synchronizes the saved graph. Fine-grained graph-delta packets remain a
future optimization. Item block-entity data preserves the graph when moved.

Rendering uses a wider, flared base, tapered cylinders, overlapping irregular
leaf volumes and reduced geometry
beyond 24 blocks. The first pot is a terracotta tray; species reuse vanilla bark
and leaves. Face UVs preserve a fixed 64-pixel-per-block texture density rather
than stretching a whole texture across each face. Vanilla shearing, chain, branch-break and water sounds plus subtle
composter particles accompany care. Additional pot shapes, bespoke assets,
disease simulation, cuttings, grafting, flowering seasons, deadwood carving,
competitions and lineage are future work.

## Verification

The standalone regression checks live outside release source sets. With Java 21
JDK available, run from the project root:

```sh
mkdir -p /tmp/hobbymod-bonsai-checks
javac -d /tmp/hobbymod-bonsai-checks \
  common/src/main/java/io/github/jakediscord/hobbymod/bonsai/BonsaiGraph.java \
  common/src/main/java/io/github/jakediscord/hobbymod/bonsai/BonsaiGrowthClock.java \
  tests/bonsai/BonsaiGraphChecks.java
java -cp /tmp/hobbymod-bonsai-checks BonsaiGraphChecks
./gradlew build
```

Checks cover 3,000 aged trees across all three species, bounds, parent ordering,
subtree pruning, overwatering/neglect, root pruning versus repotting, repeated
bending, duplicate/invalid parents, NaN/infinite geometry, 10,000 attempted deep
nodes, unloaded intervals, partial-growth reload, continuous one-minute growth,
later-generation branch and foliage growth, two-click shearing and persisted
defoliation, subtree foliage removal, soil wetness/drying, reverse bending,
immediate foliage health, care feedback and backwards/large
game-time changes.
Operations run serially on the Minecraft server thread; pruning and growth do
not mutate the graph concurrently.

In this workspace the standalone checks passed using the Eclipse Java compiler
on Java 21. The documented cloud activation script and JDK compiler are absent;
Gradle dependency resolution is blocked by the network policy's rejection of
`maven.architectury.dev`. Full mod compilation and Minecraft runtime validation
have therefore **not** passed here. No graphical client play-test has been run.
Before release, build in the configured development environment and check two
clients observing care/growth, carried-tree placement and save/reload, chunk
unload, recipe/tool durability, branch targeting, leaf transparency, close/far
LOD, first/second-click shearing, leaf state after carrying/reloading, wet soil
and drying, compact text at different GUI scales and dedicated-server startup.
