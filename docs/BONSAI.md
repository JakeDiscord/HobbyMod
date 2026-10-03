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
| Prune a branch and its descendants | Aim at a branch and right-click with shears |
| Wire and bend a branch | Aim at a branch and right-click with a copper ingot |
| Turn the bend the other way | Sneak while using copper |
| Prune roots | Sneak and right-click with shears |
| Repot with fresh soil | Right-click with dirt |

Branch selection follows the player's viewing ray through the pot's outline.
The base trunk is protected. Every wiring operation uses one copper ingot and
adds 12 degrees of bend; the seventh bend on a branch breaks that subtree.
Shears consume durability in survival. Creative mode preserves materials/tools.
Root pruning and repotting each require at least 12 growth intervals since the
previous corresponding action. Pruning roots costs health; repotting refreshes
soil and sets water to 55%. Root pruning does not replace the soil.

Trees update once per 1,200 consecutive loaded ticks (one minute at 20 TPS),
adding a bud/branch about every two intervals when healthy. Each tree starts
with two trunk segments and grows to at most 28 branch segments. Oak and cherry
spread out more than birch. Leave room and use adequate daylight or block light
(level 9 or greater above the pot). Water between 15% and 85% is healthy; watering
above 75% already causes stress. Fresh soil lasts 40 intervals; roots become
crowded after 60. Repeated pruning, excessive bending, neglect and overwatering
reduce health. Stressed foliage turns brown and disappears at zero health;
proper care can restore it. No automatic harvesting or Create automation is
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

Rendering uses tapered cylinders, crossed leaf planes and reduced geometry
beyond 24 blocks. The first pot is a terracotta tray; species reuse vanilla bark
and leaves. Vanilla shearing, chain, branch-break and water sounds plus subtle
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
nodes, unloaded intervals, reload and backwards/large game-time changes.
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
LOD and dedicated-server startup.
