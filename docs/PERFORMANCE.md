# Rendering performance

Pottery keeps its full 6,784-quad mesh while editing or within four blocks. At four to twelve blocks it renders 1,792 quads, and beyond twelve blocks it renders 480. These meshes use the same smooth profile, cavity and rim. Picking, shaping, saved pots and collision still use the original geometry. Every shape change invalidates all levels.

Pottery and marble prepare vertex positions, normals, UVs and shading once per geometry change. The frame loop no longer allocates an array for every quad or repeats expensive clay-grain trigonometry. Glaze highlights use one view direction per pot. Wheel detection runs once per pot, rather than once per vertex.

Marble merges only coplanar grid faces with identical normals and finish. Curved cuts and gameplay geometry retain their detail, and marble textures are unchanged. A solid block drops from 6,534 to 774 render quads, about 88%. Both geometry caches use a 300,000-vertex budget and a 256-entry limit; a single larger mesh can exceed the budget to avoid rebuilding it each frame. Texture atlas sprites are resolved after reloads.

Painting updates existing dynamic textures instead of recreating them on every stroke. Resizing allocates a new texture; the texture cache remains capped at 128 entries. Aquarium fish appearances are configured when their species or colors change, rather than rebuilding NBT every frame. Aquarium fish, aquascape pieces and bonsai branches reuse immutable membership snapshots until membership changes.

## Verification

In a controlled scene with sixteen finished pots, the same camera and an 854 × 480 client window improved from 2 FPS to 14 FPS. This was Xvfb with Mesa software rendering in the cloud; it is not a promised frame rate or speedup on a gaming PC. Allocation per second remained high in this software renderer, so these measurements do not establish a general allocation-rate reduction.

Matching twenty-second Java Flight Recorder captures contained 390 render-thread samples with PotteryRenderer before and 129 after. Repeated wheel registry lookup disappeared from the sampled hot path. A CPU-only vertex-attribute benchmark measured approximately 1.050 ms before and 0.197 ms after for a full-detail finished pot (5.3×); this is not a GPU or whole-game benchmark.

The benchmark uses actual geometry and can be run after compiling common:

```sh
mkdir -p /tmp/hobby-render-bench
javac -cp common/build/classes/java/main -d /tmp/hobby-render-bench tools/performance/RenderingBenchmark.java
java -cp /tmp/hobby-render-bench:common/build/classes/java/main RenderingBenchmark
```

Forty unit tests and thirty-nine Minecraft GameTests passed. Client checks covered pottery rendering, a live marble cut and cache update, texture reload, new painting strokes and canvas resizing. Existing screenshots in the painting and pottery guides show the accompanying model and workflow revisions.
