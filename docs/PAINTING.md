# Painting

Painting is an original easel-and-palette hobby, inspired by the hands-on workflow
of Joy of Painting. It uses Minecraft materials and native interface widgets.
No code or artwork from that mod is included.

## Start a painting

1. Craft an **easel**, a **canvas**, a **painter's palette**, and a **paintbrush**.
   The easel needs a solid floor and two blocks of headroom. Both its lower and
   upper sections accept interaction.
2. Right-click the easel with a canvas to mount it and open the studio. Carry your
   palette and paintbrush; the palette also works from your offhand.
3. Choose the pixel resolution with **Pixels**. The short edge cycles through
   **16, 32, 64 and 128** pixels. Shape and physical frame size stay independent
   of resolution. You can change resolution while editing; existing pixels are
   resampled with nearest-neighbor scaling rather than erased. Lowering the
   resolution reduces detail; a marked canvas asks for confirmation before shrinking.
4. Click a dye swatch to select it. If empty, this loads one matching dye from
   your actual inventory. Right-click a swatch to load another dye. One dye gives
   **512 paint**, with a maximum of 4,096 per color on each palette.
5. Hold the mouse to paint. Strokes connect sampled points so fast dragging does
   not leave gaps. Paint and canvases are synchronized and checked by the server.

| Canvas | Aspect ratio | Pixel size at short-edge resolution 64 |
| --- | --- | --- |
| Square | 1:1 | 64 × 64 |
| Landscape | 3:2 | 96 × 64 |
| Portrait | 2:3 | 64 × 96 |
| Panoramic | 2:1 | 128 × 64 |
| Round | Circular 1:1 | 64 × 64, clipped to a circle |

The largest painting is 256 × 128 pixels. Round canvases clip painting, flood
fill, sampling, display and PNG export to the circular shape.

## Studio controls

- **Brush:** round brush with sizes 1, 2, 4, 8 or 16 pixels.
- **Box:** square brush using the selected size.
- **Pen:** one-pixel precision, independent of the brush size.
- **Fill:** fill a connected region of matching pixels.
- **Soft:** graded brush edges for clouds, shading and softer transitions.
- **Pick:** select a color from the existing artwork, then continue painting.
- **Paint percentage:** 25%, 50%, 75% or 100% opacity.
- **Shift-click a dye:** select the second mixing color. **Mix** cycles through
  0%, 25%, 50%, 75% and 100%. The large swatch previews the resulting color.
  Use white and black to make lighter and darker shades. Mixed paint consumes
  both participating pigments; sampled colors still require loaded paint.
- **Mouse wheel over the canvas:** zoom from 1× to 8× around the pointer.
  **Middle-button drag:** pan. Returning to 1× recenters the canvas.

One paint unit covers up to 64 changed pixels in a submitted stroke batch; a
long stroke or large fill costs more. Repainting identical pixels is free.
Both materials and payment are validated before changing the canvas. Controls
wait for pending strokes so a tool/color change cannot alter queued strokes.
Closing the editor waits for queued work to finish. There is no automatic undo.

## Finish, display and share

Enter a title and choose **Sign**. The canvas records its title and the signing
player's name, and locks further painting. Choose **Edit** on the easel to reopen
it for changes. Signing is optional for displaying a canvas.

Choose **Take** to recover the exact canvas and leave the easel ready for another
project. Place the canvas against a solid wall to hang it. The actual artwork is
visible on the easel, on the wall, in your inventory, in your hand and in item
frames. The rear of a canvas is plain linen. Right-click a hanging painting to
view it; move it back to an easel to edit. **Shift-right-click with an empty hand**
or use **Take** to recover it. Breaking an easel returns its canvas as well as the
easel; breaking a wall display preserves the canvas. Removing the supporting
wall drops the artwork normally.

**PNG** exports the full selected resolution to `hobbymod-paintings/<canvas-id>.png`
inside Minecraft's game directory. Round exports have transparent corners.
Export never changes the original painting. There is currently no PNG import.
Physical canvases can be traded, stored and moved with their pixels and signature.

## Crafting

| Item | Materials |
| --- | --- |
| Easel | Three sticks over three planks; see recipe book |
| Painter's palette | One plank and one stick |
| Paintbrush | White wool over a stick |
| Square canvas | Eight sticks around white wool |
| Landscape canvas | Four sticks, white wool and paper |
| Portrait canvas | Four sticks, white wool and a feather |
| Panoramic canvas | Six sticks, white wool and string |
| Round canvas | Four sticks, white wool and a clay ball |

Canvas recipes use raw materials, so crafting a different shape cannot erase a
finished painting. All recipes have recipe-book unlocks.

## Development checks

Shared Java tests cover shape dimensions and masks, continuous strokes,
one-blend-per-batch opacity, soft edges, bounded flood fill, resolution changes,
signatures, compressed raster round trips and malformed data.

Six painting GameTests verify real dye consumption and pigment payment, stale strokes,
missing supplies, malformed coordinates, resizing, signing, taking, block saves,
drops, wall placement/support loss and interaction through the upper easel section. Use `-Pgametest` for the
server and any concurrent client; see README for safe shutdown instructions.
The raster, palette, persistence, networking and editing screen live in `common`.
NeoForge supplies renderer registration and the held/inventory canvas renderer.

Verified in the cloud with Java 21 and a graphical NeoForge 1.21.1 client:
33 shared tests and all 38 GameTests passed, including the six painting tests.
The live painting session covered dye loading, mixing, connected strokes, fills,
soft edges, picking colors, zoom/pan, resolution changes, the reduction warning,
signing/editing, pickup, wall placement and round-canvas clipping. The editor was
inspected at GUI scales 2 and 3. Exported landscape artwork was 192 × 128 pixels;
the round PNG was 64 × 64 with transparent corners.

After integrating the newer aquarium changes for publication, the combined
release build, all 36 shared tests and all 39 GameTests passed.

Actual client captures: [studio](painting-studio.png),
[easel](painting-easel.png),
[wall display](painting-gallery.png), [round studio](painting-round.png),
and [round easel](painting-round-easel.png).
[Example PNG export](painting-export-example.png).
