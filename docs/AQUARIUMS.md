# Aquarium keeping MVP

Aquariums are a separate hobby, with their own **HobbyMod: Aquariums** creative
tab. Fish are individual saved residents, not mobs with independent world AI.
Nothing in this feature requires GeckoLib or Create.

## Build and stock a tank

Craft a tank kit with eight glass around an iron ingot (small), iron block
(medium), or diamond (large). Place it in clear space. The placement point is
one bottom corner; the enclosure extends along positive X and Z. Placement
preflights the entire footprint, world border, build height, loaded chunks,
player permissions and living entities before consuming the kit or placing any
blocks. It won't overwrite an occupied build. Sizes include floor and walls:

| Kit | Exterior W × H × D | Water volume | Maximum residents |
| --- | --- | --- | --- |
| Small | 5 × 4 × 4 | 12 blocks | 12 |
| Medium | 7 × 4 × 5 | 30 blocks | 30 |
| Large | 9 × 5 × 6 | 84 blocks | 32 |

The generated glass tank has an open top, with water one block below the rim.
The control block is the metal block at the bottom corner. All care and selection
controls below target that block; you can view the live overlay through the glass.

Use a water bucket on the control block to fill it. One bucket represents filling
the aquarium, rather than requiring a bucket for every cell. Wait five loaded
minutes for cycling. Add sand/gravel, seagrass/kelp, rocks and driftwood. A filter
and plants help keep quality stable. Add compatible fish when quality is at least
65%. New arrivals acclimate for two loaded minutes before breeding.

Fish bags are crafted from paper, the vanilla raw fish listed below, and a dye.
This is an accessible starter-stock recipe, not a new wild-spawning ecosystem.
Each released bag gets an individual ID, sex and genes. Adults can be selected,
captured, named, moved between tanks and bred for different colors and forms.

| Fish | Starter raw fish + dye | Temperature | Stocking load |
| --- | --- | --- | --- |
| Guppy | Tropical fish + orange | Warm | 1 |
| Neon tetra | Tropical fish + light blue | Warm | 1 |
| Zebra danio | Cod + white | Cool | 1 |
| Goldfish | Salmon + orange | Cool | 3 |
| Corydoras | Tropical fish + brown | Warm | 2 |
| Betta | Tropical fish + purple | Warm | 2 |
| Angelfish | Tropical fish + gray | Warm | 3 |
| Cherry barb | Tropical fish + red | Warm | 1 |

Total stocking load must fit the water volume, as well as the 32-resident cap.
Two male bettas are incompatible; bettas/guppies and angelfish/neon tetras are
also incompatible. Admission checks explain why a fish is refused and preserve
the item. Male/female betta pairs can breed, but incompatible offspring won't
be added. These temperature and compatibility bands are simplified gameplay rules.

## Controls

| Hold / action | Result at the control block |
| --- | --- |
| Empty hand | Select the next resident |
| Fish bag / captured fish bucket | Release a compatible fish; captured buckets return the bucket |
| Empty bucket | Capture the selected fish with its ID, genes, age, health and lineage |
| Sneak + empty bucket | Drain the whole tank; residents remain saved and become stressed |
| Water bucket | Fill or change water; quality +25, algae −15 |
| Fish Food | Food reserve +20; feeding when already at 60+ food costs 8 quality |
| Shears | Clean: algae −30, quality +10, one durability in survival |
| Sand / gravel | Add substrate |
| Seagrass / kelp | Add a plant, up to 16, after substrate |
| Cobblestone / stick | Add rock / driftwood, up to 8 / 4 |
| Aquarium Filter | Fit a filter |
| Sneak + decoration/filter material | Remove the last corresponding decoration/filter; return its base material |
| Magma cream / snowball | Set warm / cool water |
| Renamed name tag | Name the selected fish |

Sneaking is explicitly allowed to interact with the controller. Creative mode
preserves consumables and durability. Removing kelp/gravel returns the standard
seagrass/sand material for this abstract decoration system. There is no precise
per-object aquascaping editor yet; decoration locations use a fixed layout.

Fish Food is wheat + dried kelp, yielding eight portions. Filters use three iron
ingots around charcoal, with redstone below. Starter fish bags do not return
an empty bucket, so their recipes do not manufacture free buckets. Capturing a
resident consumes one actual empty bucket and releasing it returns that bucket.
Advanced tooltips show carried fish IDs and parent IDs; sneaking at a tank shows
the selected fish's color/form alleles and food/algae/plant load.

## Water, breeding and recovery

The server checks the enclosure once per second and advances chemistry once per
1,200 consecutive loaded ticks (one minute at 20 TPS). Missing water, broken
walls and partially unloaded tanks have explicit warnings. Wall repair accepts
vanilla glass and impermeable replacements; floor replacements may be solid.
The footprint remains fixed: capture residents and rebuild a larger kit to
resize. A smaller/malformed loaded tank preserves its bounded resident list and
reports overstocking instead of deleting fish.

Quality decreases with stocking load, excess food and heavy algae; plants and a
filter offset that load. Bright tanks accumulate algae, reduced by plant load.
Food is shared by the tank and consumed each minute according to stocking load.
Fish lose 8 health per minute when water is missing, walls are broken, quality
is below 50%, food runs out, temperature is wrong, stocking is excessive or
residents are incompatible. Healthy conditions restore 4 health per minute.
Health never deletes a fish: even residents at zero can recover through care.

Healthy, acclimated male/female pairs of the same species breed at quality 75%
or above, when fed and aged at least six minutes. Parents have a six-minute
cooldown. At most four fry are admitted per minute, respecting compatibility,
stocking load and resident caps. Fry start small and mature over eight minutes.
Each child inherits one color allele and one form allele from each parent.
The tiny genotype uses two 0–15 color alleles and two 0–3 form alleles, plus sex,
age, health and two parent UUIDs. Color and body/fin shape use those values.
Lineage references are IDs; the simulation never recursively loads ancestors.

Breaking the controller normally drops a kit containing the full aquarium state
and dismantles its generated vanilla glass and water when the whole footprint
is loaded. Replacement blocks are preserved. Placing a carried kit restores the
same size, water, care, decorations and residents without a free water-quality
reset. Never force-load chunks to dismantle a partially unloaded tank; leftover
shell blocks in unloaded areas need manual cleanup.

## Rendering and synchronization

Actual Minecraft glass and source water provide the transparent enclosure.
Client geometry adds substrate, hardscape, plants, filter and algae patches.
Eight fish varieties have different silhouettes, markings, gene-based colors
and fin shapes. Deterministic bounded swimming and tail animation avoid entity
AI/pathfinding. All paths stay inside the rectangular water volume. Corydoras
stay near the substrate. Residents render within 48 blocks; beyond 24 blocks
fins/eyes simplify and movement updates less often. Vanilla textures and sounds
are prototype assets; there are no copied Fintastic assets.

Server changes use bounded block-entity snapshots; chunk loading sends the same
saved state. A live compact overlay shows quality, temperature, resident count,
selected fish and actionable warnings. Nearby clients see care/selection changes.
Chunk unloading pauses the one-minute clock without catching up. `/time set`
changes daylight, not the game-time clock; discontinuities reset the interval.

Create pumps/feeders, wild species, custom painted fish textures, detailed
schooling/territorial AI, disease, nutrient/CO₂ simulation and competitions are
future work. No automation hooks or additional rendering dependency are added
in this MVP.

## Reference review

[Fintastic](https://github.com/VoidArkana/Fintastic) is the primary reference.
I reviewed its public `master` source: aquarium glass registration/rendering,
fish variants, breedable water animals, breeding/follow-parent goals and animation
classes. The current Modrinth page could not be fetched because this workspace's
network policy rejects that host. That public source branch is not evidence of
the current GeckoLib/genetics implementation described in the brief. This feature
implements its own compact resident/genetics data and fixed tanks, without
copying Fintastic code, models or textures.

## Verification

Standalone Java 21 checks are outside release source sets:

```sh
mkdir -p /tmp/hobbymod-aquarium-checks
javac -d /tmp/hobbymod-aquarium-checks \
  common/src/main/java/io/github/jakediscord/hobbymod/aquarium/AquariumData.java \
  common/src/main/java/io/github/jakediscord/hobbymod/aquarium/AquariumClock.java \
  common/src/main/java/io/github/jakediscord/hobbymod/aquarium/AquariumMotion.java \
  tests/aquarium/AquariumChecks.java
java -cp /tmp/hobbymod-aquarium-checks AquariumChecks
./gradlew build
```

These passed using Eclipse's compiler on Java 21 here: 1,000 breeding tanks,
32,000 final residents and 307,200 bounded swim poses across all species and
sizes. Checks cover cycling, acclimation, incompatible species/temperature,
capture identity, overfeeding, cleaning, recoverable drainage stress, inherited
alleles, breeding/stocking caps, malformed/duplicate residents, size reduction
and unloaded/reversed-time clock behavior. They do not test GPU performance.

Four additional Minecraft GameTests are in `neoforge/src/gametest` and use a
separate spacious template. With a configured environment, run
`./gradlew --no-daemon -Pgametest :neoforge:runServer`, then `test runall` and verify
four `AQUARIUM_TEST_PASS` messages. They cover physical fill/drain, broken glass
and removed water, occupied-footprint preservation, chunk/update-tag persistence,
carried fish and mining. Stop the server using the repository README instructions.

Full compilation and those GameTests are **blocked here**: the documented cloud
activation script is absent and the Architectury Gradle plugin is unavailable
in the local cache, while its Maven host is blocked by network policy. No client
play-test has run. Before release, build normally and run the four GameTests,
then check two clients, tank placement/carrying, bucket returns, live overlays,
water/glass transparency at multiple angles, damage/repair, chunk boundaries,
GUI scales and hundreds of visible residents on actual client hardware.
