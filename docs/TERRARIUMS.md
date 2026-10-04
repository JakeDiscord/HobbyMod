# Terrariums

Terrariums live beside aquariums in **HobbyMod: Habitats**. The starter enclosure
is 2×1×1, with the same placement facing, clear glass, native Minecraft block
models and in-world editor as aquariums. It contains land rather than water.
Existing aquariums keep their fish and water care.

## Setup and supplies

Craft the enclosure with eight glass surrounding a copper ingot. Place it on a
solid surface, then right-click to open **Ecosystem**, **Layout** or **Guide**.
The displayed inventory is your real inventory; supplies are consumed in
survival, and successful misting/trimming wears the tool.

| Supply | Use in Ecosystem |
| --- | --- |
| Gravel | Add drainage beneath the substrate |
| Dirt, sand or moss block | Select one of three substrate beds; swapping returns the old material |
| Terrarium mister | Raise moisture and humidity; craft from a glass bottle and iron nugget |
| Glowstone dust | Install a light strip for dark rooms |
| Terrarium heat lamp | Warm the enclosure and emit real level-14 Minecraft block light; craft three iron ingots over glass/redstone/glass, with blaze powder below |
| Magma cream / snowball | Select warm 24–28°C / mild 18–24°C; warmer closed enclosures dry slightly faster |
| Leaf litter | Feed colonies; craft leaves plus dried kelp for four portions |
| Springtail colony | Introduce three tiny inhabitants; craft litter, sugar and white dye |
| Isopod colony | Introduce three tiny inhabitants; craft litter and gray dye |
| Shears | Trim the most recently placed plant that still has height to trim |

Use **Remove / collect** to return installed drainage, substrate, lamps or up to
three residents of the selected colony type. A substrate cannot be removed
until its inhabitants are collected. Held supplies also work by right-clicking
the enclosure; sneak selects removal. Opening the lid improves ventilation and
speeds drying; closing it conserves moisture. Misting shows a brief spray, wet
soil darkens, and very humid closed glass shows light condensation.

![Terrarium care with live moisture, humidity and colony counts](terrarium-care.png)

## Layout and exact rotation

In **Layout**, choose a block from your inventory, then click the top view to
place it. The five starter plants are moss, fern, azalea, poppy and oak sapling.
Other allowed Minecraft blocks can form rocks, branches, backgrounds and
ornaments. The enclosure holds at most 28 pieces. Removal returns the actual
placed material.

Choose **Edit in 3D**, then click a piece in the actual world. Drag its arrows to
move it or its cubes to stretch along an axis. Right-drag orbits; wheel zooms.
Curved rotation arrows now surround the selected piece alongside its move
arrows and scale cubes. Drag the red ring for pitch, green for yaw or blue for
roll; hold Shift while dragging to snap the rotation change to 15° steps.
Angles remain precise without typing into separate GUI fields. The server
checks distance, permissions and bounds; tilted pieces shrink or move to fit
inside the enclosure. Decoration orientation, scale and location are saved
and carried with the tank. Reset size remains available; aquariums retain their
existing 90° rotation shortcut.

![Curved rotation arrows alongside movement and scale handles](terrarium-precise-editor.png)

![Planted terrariums with native block models and cosmetic residents](terrarium-display.png)

## Peaks, valleys and indoor lighting

From the 3D editor choose **Edit terrain**. Select **Raise**, **Lower** or
**Smooth**, then hold or drag on the substrate. A brush outline follows the
surface. Shift temporarily lowers; Shift + wheel changes brush size. Hold in
one place to keep changing that spot as the surface rises or falls. Choose
**Edit objects** to return to decoration handles. Soil, sand and moss all
support shaping; the saved height field stays bounded beneath the glass.
Decorations stay grounded and shrink if necessary to fit beneath the lid;
residents follow the terrain height while crawling forward.

Gravel drainage and the sand bed are substantially thicker than the original
flat layer. Valleys retain a minimum substrate thickness above drainage. Relief
is saved when chunks unload and when the enclosure is mined and carried.

A **Terrarium Heat Lamp** mounts a small hood and warm bulb under the lid.
Install it from Ecosystem or remove it in Remove / collect mode. It emits real
Minecraft block light, warms the habitat and supplies light level 14 indoors;
ordinary light strips supply ecosystem light level 12. The lamp and terrain
survive carrying. Unlit habitats use ambient lighting rather than making tiny
inhabitants glow in a dark room. The care light readout refreshes as lamps or
room lighting change.

![A sculpted sand landscape with thicker drainage and indoor heat-lamp lighting](terrarium-terrain.png)

## A gentle display ecosystem

The server advances care once per loaded minute. Moisture, humidity, light,
food and average resident comfort are visible in the care screen. Plants help
retain humidity; a lamp overrides environmental light. Leaf litter is consumed
slowly, and a dry or hungry colony loses comfort rather than dying randomly.
Misting and feeding allow it to recover. Unloaded or broken enclosures pause
simulation; elapsed unloaded time does not cause a care penalty on return.

Closed, planted, comfortable colonies with at least two mature inhabitants can
produce young every eight loaded minutes. Residents inherit a color variant
and record a parent ID. Tiny juveniles grow, and the population stops at 48.
Collected colony items preserve individual IDs, ages, comfort, lineage and
variants, with variant names shown in their tooltip. Mining the enclosure
returns its kit with all care state, inhabitants and layout saved.

Springtails and segmented isopods are cosmetic residents, not world entities.
Their seeded paths stay inside the enclosure; no pathfinding or escaping world
entities run when the lid opens. Distant rendering reduces animation detail.
Plant integrations can extend the `hobbymod:terrarium_plants` block tag.
Full animal entities, disease, paludariums and custom background assets are
future extensions.

## Validation

Eight shared terrarium tests cover recoverable neglect, long-running capped breeding,
resident collection, save compatibility, 6,000 rotated layouts and bounded
seeded motion. Five native GameTests exercise real land-kit placement,
survival supply consumption and refunds, colony identity round trips, arbitrary
rotation and mining/replacement. These bring the suite to 48 shared tests and
45 native GameTests. The normal release build excludes development GameTests.

Graphical NeoForge client testing used Java 21, Xvfb and Mesa software OpenGL.
Screenshots were captured and opened with the image-viewing tool. Checks
included visible native plants behind glass, dry land without an aquarium
water plane, tiny colored residents, all five plants, real supply clicks,
colony collection and restoration, breeding after loaded time advancement,
unloaded-time pause, open/closed lid controls, exact 37.5° / 12.25° / −8.5°
rotation, multiple nearby enclosures and editor/care layouts at smaller window
sizes. The follow-up checks exercised curved rotation handles, hold-to-raise,
lowering, smoothing, thicker sand/gravel, forward-facing crawlers over hills,
and installing/removing a heat lamp in a sealed room. Additional tests cover
model heading alignment, bounded relief, decoration grounding and carrying
the shaped enclosure with its emitted lamp light. Compiled code and simulations alone do not establish visual correctness.
