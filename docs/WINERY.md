# Winery

Grow a little vineyard and turn its harvest into your own labeled vintages.
This hobby uses a few meaningful choices rather than a chemistry simulator.
![Red and white vines at different growth stages](winery-vineyard.png)

All items are in the **HobbyMod: Winery** creative tab; wine itself is made in
batches rather than supplied as a blank decorative bottle.

## From vineyard to bottle

1. Place a **grape trellis** on dirt, grass or farmland. Right-click it with a
   red or white grape cutting. The vine needs light to grow. Bone meal brings
   it to ripe grapes; later ripeness happens naturally.
2. Right-click a vine to harvest. Firm grapes give three fruit; ripe and later
   stages give four. The vine stays planted and regrows. Outdoor, sun-grown
   grapes have a small quality bonus.
3. Put **eight grapes and an empty bucket** in a grape press. The two grape
   slots let you blend colors and harvests. Click **Press**. Its wooden plate
   moves while it works; after three seconds, collect the bucket of must.
4. Put must and **one wine yeast** in a fermentation barrel and click **Start**.
   Collect the returned empty bucket. Fermentation takes one minute. Keep the
   barrel away from fire, lit campfires and lava for better quality.
5. Once fermented, click **Rack**. This begins aging in the same barrel, so
   there is no second machine to craft. Racking is a single gameplay step that
   represents transferring wine away from sediment.
6. Let it age in a dark, cool cellar. The screen shows elapsed and recommended
   age, quality, and whether the location is hot, bright or dark. White wine's
   recommended age is one minute, rosé's is 90 seconds, red's is two minutes.
   You can bottle early; leave it beyond three times the recommended age and
   quality starts declining.
7. Add glass bottles, optionally type a label, and click **Bottle**. Each batch
   fills **four bottles**, using one glass bottle each. You can bottle a few
   now and the rest later; the first bottling fixes that vintage's name and
   tasting record. Bottle stacks contain only one batch.
8. Right-click a wine rack with a bottle to display it. Right-click with an
   empty hand to take a bottle back. Each rack holds four. Hold right-click to
   taste a bottle; it returns the glass. Good-quality wine gives 30 seconds of
   Haste, alongside a short tasting note.

Rinse an empty, used barrel by putting a water bucket in its must slot and
clicking **Rinse**. Dirty barrels still work, but the next batch is cloudy and
loses some quality. There is no destructive fermentation failure.

![The grape press uses normal inventory slots](winery-press.png)

![A labeled batch in the fermentation barrel](winery-fermentation.png)

![Wine bottles displayed in a rack](winery-rack.png)

## Harvest choices

| Ripeness | Appearance | Result |
| --- | --- | --- |
| Firm | Green grapes | Tart, lower-quality harvest |
| Ripe | Red or pale grapes | Reliable quality |
| Late harvest | Darker red or golden grapes | Sweeter, slightly higher quality |
| Overripe | Brown fruit | Sweet, lower-quality harvest |

A mostly red pressing produces red wine; mostly white produces white wine.
Middle blends produce rosé. Sugar, acidity, tannin, maturity and oak influence
short tasting notes. They are simple gameplay values, not real-world chemistry.
The vintage is the world's day when pressing starts.

## Crafting

| Item | Recipe |
| --- | --- |
| Two trellises | Six sticks around a plank (`S S / SPS / S S`) |
| Two red cuttings | Sweet berries + stick, or red grapes + stick |
| Two white cuttings | Glow berries + stick, or white grapes + stick |
| Grape press | Iron ingot / three planks / wooden slab, piston, wooden slab |
| Fermentation barrel | Vanilla barrel + copper ingot + glass bottle |
| Wine rack | Three planks, three sticks, three planks in rows |
| Four yeast | Bread + sugar |

Buckets and glass bottles use vanilla recipes. There is no copied Vinery code
or artwork; its vineyard workflow is the inspiration for this original hobby.

## Moving and automation

Breaking a working press refunds the reserved fruit and bucket. Breaking a
barrel drops a portable wine cask with its current batch; place it in another
barrel and click **Resume**. A cask does not create another empty bucket.
Breaking racks returns their bottles.

Supply ingredients through hoppers from above or the sides. A hopper below
extracts finished must or wine and a barrel's returned empty buckets. A redstone
signal enables automatic pressing. Powered barrels automatically start supplied
must, rack after fermentation, and bottle at the recommended age. Keep empty
buckets extracted so the must slot can receive the next batch. Rinsing is manual.
Automation uses ordinary item inventories, which can also be supplied by mods
that interact with Minecraft containers; there is no custom Create fluid pipeline.

The press accepts the shared `c:fruits/grapes` tag. Red/white grape tags include
optional Vinery item IDs, so those grapes can coexist in recipes and pressing
when installed. This release has not been tested with Vinery or Create installed.

## Saves and multiplayer

Each pressing has a unique batch UUID and a compact vector of gameplay values.
Active batches are saved with their machine or portable cask. Bottling puts one
immutable tasting record in the overworld's saved cellar data. Bottles carry that
UUID, label, vintage, color and a quality display snapshot, rather than copying
the full vector. Different batches never stack together.

A central issue count allows at most four bottles from a batch, including stale
or duplicated casks. This is a production safeguard, not protection against
creative commands that directly copy finished items. Finished bottles moved to
another world retain their appearance and label and can be drunk, but the original
tasting record and quality-based buff require that world's cellar data.

Fermentation and aging sample once per second while loaded and catch up by
elapsed game time when reloaded. They progress while the world is running,
including unloaded chunks; a stopped server does not count real-world hours.
Unloaded intervals use the last sampled cellar conditions. Changing the daylight
clock with `/time` changes the vintage but does not speed up fermentation.

## Development checks

Run `./gradlew --no-daemon :common:test` for the shared regression suite. Winery's
10 pure-Java tests cover ripeness/blending, fermentation/racking/aging, unloaded
elapsed time, clock rollback, malformed saves, UUIDs and duplicate issuance.

Run `./gradlew --no-daemon -Pgametest :neoforge:runServer`, then `test runall`.
Ten Winery GameTests cover vine planting/harvest permissions, powered barrel automation, pressing costs and interrupted-work refunds, full batches,
unloaded/restored casks, duplicate casks, rinsing and server reach checks, powered
pressing with a real output hopper, rack saves/stack identity, and tasting/glass
returns. Keep `-Pgametest` on simultaneous client runs. See the root README for
safe server shutdown. Normal release builds exclude the tests and test structure.


The completed Winery was inspected in a running NeoForge 1.21.1 client.
Gameplay checks included bone-meal growth, shift-click pressing, fermentation,
racking, aging, custom label delivery, bottling and rack insert/remove. A saved
vintage was tasted after a server restart; its original notes, quality reward
and glass returns survived. Client review caught and fixed the missing inventory
panel, clipped press hint, shelf intersection and rosé model selection. The
screenshots above are direct game-window captures; rack color variations were
prepared with development commands to check all three models.

Validated after integrating Astronomy: 90 unit tests, all 69 GameTests (10 Winery), and the NeoForge release
build. The production jar contains no GameTests, test structure or JUnit classes.
