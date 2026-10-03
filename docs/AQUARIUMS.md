# Aquarium keeping

Craft an aquarium with eight glass around an iron ingot (small), iron block
(medium), or diamond (large). These are custom furniture models with contained
water, glass panels and dark trim. They do not construct vanilla glass walls or
place water in the world.

| Aquarium | Footprint | Height | Stocking capacity |
| --- | --- | --- | --- |
| Small | 1 × 1 | 1 block | 12 load |
| Medium | 2 × 1 | 1 block | 30 load |
| Large | 4 × 2 | 2 blocks | 32 residents / 84 load |

Placement extends along positive X/Z from the clicked corner and reserves only
these cells. Occupied space, entities, world borders and unloaded chunks are
checked before consuming the item. The extra cells are aquarium parts rather
than independent glass blocks. Right-click **any part** to open its screen.

## Using the screen

The top shows water quality, food, temperature, filter, decorations and the next
care step. The left lists resident fish: click one to select it; hover to see
species, sex and acclimation. The right lists compatible supplies in your own
inventory. Click a supply to use it on this tank. Arrows page through both lists.
No supplies are created by the screen; survival consumes actual items.

1. Bring a water bucket and click **Fill / change water**.
2. Wait five loaded minutes for cycling. The screen shows progress.
3. Add sand/gravel, then plants, rocks, driftwood and a filter.
4. Add compatible fish bags from your inventory. Refusals preserve the fish item.
5. Feed and clean as needed. Select a resident and use a bucket to take it out.

| Supply | Screen action |
| --- | --- |
| Water bucket | Fill / change water: quality +25, algae −15 |
| Fish bag / captured fish | Add that fish; captured fish return the empty bucket |
| Empty bucket | Take selected fish, preserving ID, age, health, genes and lineage |
| Fish Food | Feed: reserve +20; excess food lowers water quality |
| Shears | Clean glass: algae −30, quality +10; uses durability |
| Sand / gravel | Add substrate |
| Seagrass / kelp | Add plants after substrate (up to 16) |
| Cobblestone / stick | Add rock / driftwood (up to 8 / 4) |
| Aquarium Filter | Install filter |
| Magma cream / snowball | Set warm / cool water |
| Renamed name tag | Name selected fish |

Use **Remove decor** to remove the corresponding decoration/filter using its
inventory button. Removing kelp/gravel returns the standard seagrass/sand base
material. In that mode an empty bucket drains contained water. Fish stay saved
and can recover after refilling. Decoration positions currently use a preset
layout rather than a per-object placement editor.

Food is crafted from wheat and dried kelp (eight portions). The filter recipe
uses three iron ingots, charcoal and redstone. Starter fish bags use paper, a
vanilla raw fish and dye:

| Fish | Raw fish + dye | Water | Stocking load |
| --- | --- | --- | --- |
| Guppy | Tropical fish + orange | Warm | 1 |
| Neon tetra | Tropical fish + light blue | Warm | 1 |
| Zebra danio | Cod + white | Cool | 1 |
| Goldfish | Salmon + orange | Cool | 3 |
| Corydoras | Tropical fish + brown | Warm | 2 |
| Betta | Tropical fish + purple | Warm | 2 |
| Angelfish | Tropical fish + gray | Warm | 3 |
| Cherry barb | Tropical fish + red | Warm | 1 |

Two male bettas, bettas with guppies, and angelfish with neon tetras are
incompatible. Each addition checks temperature, cycling, quality, capacity and
compatibility. New fish acclimate for two minutes.

## Care, breeding and carrying

Chemistry advances once per 1,200 consecutive loaded ticks. Chunk unloading
pauses the clock; time discontinuities do not trigger offline catch-up. Plants
and filters offset stocking/feeding waste. Bright tanks accumulate algae.
Food is shared by all residents.

Poor water, no food, incompatible fish, wrong temperature, drainage or excessive
stocking cause 8 health loss per minute. Healthy conditions restore 4. Fish are
never silently deleted at zero health. Healthy, fed, acclimated adult pairs of
the same species breed at quality 75% or above, with a six-minute cooldown and
at most four fry per update. Fry inherit color/form alleles and parent IDs,
and mature over eight minutes. Stocking and compatibility limits also apply
to offspring.

Breaking any aquarium part dismantles its loaded companion parts and drops one
kit with the complete tank state. Placing it restores its size, contained water,
care, decorations and individual residents. Neighboring unrelated blocks are
preserved. No chunks are force-loaded for dismantling; orphan parts across an
unloaded boundary may need manual removal.

Older glass-multiblock saves retain resident data in their controller. Mine and
replace the controller to rebuild it as compact furniture; remove the old
vanilla glass/water manually. The new code deliberately does not erase old
surrounding builds during conversion.

## Rendering and development

The tank renderer draws its own enclosure and contained water. Fish use a
bundled white texture tinted by species/genetics, rounded body geometry, fins,
eyes, minimum aquarium lighting and bounded swimming. This avoids the previous
unreliable block-atlas fish texture. Fish are saved residents with no independent
entity AI. Geometry simplifies past 24 blocks and renders within 48 blocks.
Server updates use bounded block-entity snapshots. Screen actions validate
player distance/permissions and real inventory stacks on the server.

Fintastic's public source was reviewed during the initial implementation;
no code or assets were copied. There is no GeckoLib or Create dependency.

Standalone Java 21 checks live in `tests/aquarium/AquariumChecks.java`. They cover
1,000 breeding tanks, 32,000 residents and 307,200 swim poses, as well as cycling,
care, compatibility, genetics, caps, footprint sizes and unloading clocks.
Compile AquariumData, AquariumClock, AquariumMotion and AquariumChecks, then
run `java AquariumChecks` with their output directory on the classpath.

Five Minecraft GameTests in `neoforge/src/gametest` cover contained fill/drain,
compact placement with no world water, part routing/dismantling, occupied-space
preservation and persistence/captured fish. Run with
`./gradlew --no-daemon -Pgametest :neoforge:runServer`, then `test runall`;
follow README server shutdown instructions. Development tests stay outside normal
release jars.

Full Gradle compilation remains blocked in this cloud workspace by unavailable
Architectury dependencies. Minecraft GameTests, the GUI and transparent/client
rendering have not been play-tested. Verify inventory returns, simultaneous
clients, small GUI scales, glass/water views, carrying tanks and chunk boundaries
in a configured client before release.
