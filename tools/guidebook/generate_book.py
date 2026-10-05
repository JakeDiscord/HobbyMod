#!/usr/bin/env python3
"""Author the universal Patchouli book. --check validates generated files without writing."""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'common/src/main/resources'
BOOK = 'hobbymod:hobbies'
ASSETS = RES / 'assets/hobbymod/patchouli_books/hobbies/en_us'
FILES = {}
CATEGORIES = [
    ('getting_started', 'Welcome', 'minecraft:book', 'Choose a hobby, gather its tools, and make something of your own.'),
    ('sculpting', 'Marble Sculpture', 'hobbymod:chisel', 'Carve, smooth, preserve and share your own marble sculptures.'),
    ('pottery', 'Pottery', 'hobbymod:pottery_wheel', 'Throw a pot, dry it, fire it, glaze it, and give it a home.'),
    ('painting', 'Painting', 'hobbymod:paint_palette', 'Mix dyes, paint custom canvases, and hang your work.'),
    ('aquariums', 'Aquarium Keeping', 'hobbymod:small_aquarium', 'Build an underwater habitat and care for its fish.'),
    ('terrariums', 'Terrariums', 'hobbymod:terrarium', 'Shape a living landscape and tend its residents.'),
    ('bonsai', 'Bonsai', 'hobbymod:bonsai_pot', 'Grow, prune and wire an individual miniature tree.'),
    ('dj', 'DJ & Music', 'hobbymod:dj_workstation', 'Compose patterns, arrange songs, and perform with two decks.'),
    ('winery', 'Winery', 'hobbymod:grape_trellis', 'Grow grapes, tread or press, ferment, age and bottle.'),
    ('astronomy', 'Astronomy', 'minecraft:spyglass', ''),
]

def put(path, obj):
    FILES[path] = json.dumps(obj, indent=2, ensure_ascii=False) + '\n'

def text(value, title=None):
    page = {'type': 'patchouli:text', 'text': value}
    if title:
        page['title'] = title
    return page

def entry(category, key, name, icon, paragraphs, order=0):
    # Each paragraph is a short page, rather than an overflowing wall of instructions.
    pages = [text(p) for p in paragraphs]
    put(ASSETS / 'entries' / category / (key + '.json'), {
        'name': name, 'icon': icon, 'category': 'hobbymod:' + category,
        'sortnum': order, 'pages': pages,
    })

put(RES / 'data/hobbymod/patchouli_books/hobbies/book.json', {
    'name': 'The Hobby Handbook', 'subtitle': 'Make it your own',
    'landing_text': 'Your companion for crafting, creating and caring.$(br2)Pick a hobby from the index. Every chapter is available immediately; recipes, controls and practical help are inside.',
    'version': '1', 'use_resource_pack': True, 'use_blocky_font': True,
    'book_texture': 'patchouli:textures/gui/book_green.png',
    'model': 'patchouli:book_green', 'creative_tab': 'hobbymod:hobbies',
    'show_progress': False, 'show_toasts': False, 'pause_game': False,
})
for order, (key, name, icon, description) in enumerate(CATEGORIES):
    put(ASSETS / 'categories' / (key + '.json'), {
        'name': name, 'description': description, 'icon': icon, 'sortnum': order,
    })

entry('getting_started', 'handbook', 'Your Handbook', 'minecraft:book', [
    'Craft this handbook with a book and green dye. Right-click to read it. The same book contains every hobby; you do not need separate manuals.$(br2)It is also in the HobbyMod creative tab.',
    'Use the index to choose a hobby, or search for an entry by name. Recipe pages show the real crafting ingredients. Click linked entries to jump to related help. Bookmark useful entries for your next session.',
    'For a first project, try $(l:pottery/first_pot)a clay pot$(/l), $(l:painting/first_canvas)a small painting$(/l), or $(l:bonsai/planting)a bonsai$(/l). For care-focused hobbies, start with $(l:aquariums/setup)an aquarium$(/l) or $(l:terrariums/setup)a terrarium$(/l).',
    'Project shapes and care belong to the world. Taking or mining a finished project usually preserves it; read its hobby chapter before moving it.$(br2)In-world cameras do not pause danger. Stay close to your work and choose a safe place.',
])
entry('getting_started', 'world_controls', 'Working in the World', 'minecraft:compass', [
    'Marble and pottery use an orbit camera around the actual work. Pick tools from your hotbar, not buttons. Press E for your inventory and Escape to leave the camera.',
    'Right/middle drag or Alt + left drag orbits. Ctrl + wheel zooms. Number keys and ordinary scrolling select your held tool. F reframes the work.$(br2)Marble also uses Shift + wheel to move up or down a pillar.',
    'Read $(l:sculpting/controls)carving controls$(/l) and $(l:pottery/controls)wheel controls$(/l) before shaping. Cuts are permanent: marble and pottery have no undo.$(br2)Painting and music use their own editor controls.',
], 1)
entry('getting_started', 'sharing', 'Keep & Share Your Work', 'minecraft:chest', [
    'Marble blueprints copy a structure and share as files. Paintings export PNG images; their physical canvases can be traded. DJ projects share as editable files or recorded project discs.',
    '$(l:sculpting/blueprints)Marble blueprints$(/l) require enough real marble when applying. $(l:painting/display)Painted canvases$(/l) preserve pixels and signatures. $(l:dj/files)Music files$(/l) stay in the Minecraft instance that exports them.',
], 2)

entry('sculpting', 'start', 'Your First Sculpture', 'hobbymod:marble', [
    'Craft four marble from calcite and quartz. Place a block, then right-click it with a carving tool to enter the world camera. Opening the camera keeps the whole blank; it does not turn into a preset statue.$(br2)A stonecutter also converts marble into a carving blank.',
    'Choose a tool in your hotbar. Hold left-click and drag across the surface to cut. Rotate often to check the shape from several sides.$(br2)Start with broad removal, switch to a smaller chisel, then finish with the polishing rasp.',
    'Stack marble vertically for a taller blank. Opening carving joins the column; placing marble on an existing sculpture adds another section. Shift + wheel moves the camera along the height.',
    'Cuts can cross section boundaries. Only connected marble rooted in the surviving base stays attached. Severed pieces break away; thin unsupported arms and floating pieces cannot remain.$(br2)There is no undo.',
])
entry('sculpting', 'tools', 'Chisels & Smoothing', 'hobbymod:polishing_rasp', [
    '$(bold)Detail chisel$(/bold): small recesses and fine cuts.$(br2)$(bold)Point chisel$(/bold): medium cuts.$(br2)$(bold)Roughing mallet$(/bold): broad removal to establish the silhouette.',
    '$(bold)Polishing rasp$(/bold): hold and drag over high spots to smooth the surface. It removes marble rather than adding it back.$(br2)Successful strokes wear your actual held tool in survival. Creative preserves durability.',
    'Work is saved with the sculpture. Mine it with a suitable pickaxe to move a carved section. Replacing it preserves the shape.$(br2)The overlay shows the held tool and the percentage of marble remaining.',
], 1)
entry('sculpting', 'controls', 'Carving Controls', 'hobbymod:point_chisel', [
    'Left-click/drag: carve or smooth.$(br)Right/middle drag: orbit.$(br)Alt + left drag: orbit.$(br)Ctrl + wheel: zoom.$(br)Shift + wheel: move up/down the pillar.',
    '1-9 or ordinary wheel: hotbar.$(br)E: inventory, then resume.$(br)M: X symmetry on/off.$(br)F: reset camera.$(br)Escape: exit.$(br2)Changing tools, orbiting or releasing left-click ends the current stroke.',
    'You must stay within six blocks of the section being carved. The orbit camera does not move your player or extend reach. Damage, leaving reach or losing the block ends the session. Nearby walls limit the camera.',
], 2)
entry('sculpting', 'blueprints', 'Marble Blueprints', 'hobbymod:marble_blueprint', [
    'Craft a blueprint from paper and lapis. Use a blank on marble to capture the connected structure. The clicked block is its anchor. Sneak-use a filled blueprint to capture a replacement design.',
    'To apply, place real marble at every saved offset, then use the filled blueprint on the anchor. Missing marble or protected blocks reject the whole application.$(br2)Applying replaces existing cuts permanently. Capture them first if you want to keep them.',
    'Blueprints are reusable. They capture up to 64 face-connected blocks within 15 blocks of the anchor in each direction. World-axis orientation stays fixed. Separate a sculpture from neighboring marble before capture.',
    'Use the blueprint in the air for its file screen. Choose a name and Export file or Import file. Open folder shows the location.$(br2)Share .marble.json files from hobbymod/blueprints inside your Minecraft game directory.',
], 3)

entry('pottery', 'first_pot', 'Throw Your First Pot', 'hobbymod:prepared_clay', [
    'Craft prepared clay and a potter\'s wheel. Place the wheel and use prepared clay on it. Select an empty hand and right-click the loaded wheel to start spinning and enter its camera.',
    'With empty hands, left-click the lump\'s top to open its center. Drag the sides left to narrow a ring or right to widen it. Start near the rim and drag up to pull taller, or down to lower the pot.',
    'Your shape goes all the way around the spinning pot. Pulling and widening redistribute its clay; there is no free extra clay. If a move fails, try a gentler change or a thicker wall.',
    'Switch to a rib or sponge and hold left-click over a ring to smooth it. Watch moisture. If too dry to shape, leave the camera and use a water bucket on the wheel, then return.',
])
entry('pottery', 'controls', 'Wheel Tools & Controls', 'hobbymod:pottery_rib', [
    'Empty hands: open, widen/narrow and pull the rim.$(br2)Wooden rib or sponge: smooth wet clay.$(br2)Trimming loop: remove clay from the foot of leather-hard pottery.',
    'Clay cutting wire: use on the wheel outside the camera to lift the pot. Its shape and stage become a carried pot.$(br2)A water bucket rehydrates wet or leather-hard clay, but cannot soften bone-dry or fired pottery.',
    'Left-click/drag: shape with hands.$(br)Left-click/hold: smooth or trim.$(br)Right/middle drag, Alt + left drag: orbit.$(br)Ctrl + wheel: zoom.$(br)1-9/wheel: tools.$(br)E: inventory.$(br)F: frame.$(br)Escape: exit.',
], 1)
entry('pottery', 'dry_trim', 'Drying & Foot Trimming', 'hobbymod:pottery_loop', [
    'Cut the pot off and place it to air-dry, or stop its wheel and leave it there. A spinning wheel pauses drying.$(br2)Wet clay becomes leather-hard after 60 loaded seconds, then bone dry after another 60.',
    'At leather-hard, return the pot to an empty wheel. Use the trimming loop to enter the camera and hold left-click near the bottom quarter. This removes clay from the foot.$(br2)Cut it off again and finish drying.',
    'A moisture value of 0 does not mean the pot is already fired. Check its stage: bone-dry clay belongs in the kiln.$(br2)Inventory storage and unloaded chunks pause drying. Bone-dry clay cannot be thrown again.',
], 2)
entry('pottery', 'firing', 'The Two Firings', 'hobbymod:pottery_kiln', [
    'Open the kiln\'s furnace-style screen. Put a bone-dry pot in the upper-left input and coal or charcoal in the lower-left fuel slot. Wet and leather-hard clay are refused.',
    'Firing takes 30 seconds, followed by 10 seconds cooling. The pot stays locked while hot. Take the bisque pot from the output on the right.$(br2)Shift-click transfers pottery and fuel.',
    'Place the bisque pot and apply a pottery glaze. Pick it up and fire/cool it again to finish. Unglazed bisque does not undergo the second firing.$(br2)Glaze recipes support all sixteen dye colors.',
    'Both firings preserve your design with slight shrinkage. One coal or charcoal can heat both firings of one pot. Firing and cooling require loaded server ticks.$(br2)Breaking a kiln returns its pot and unconsumed fuel.',
], 3)
entry('pottery', 'flowers', 'Display, Water & Flowers', 'minecraft:poppy', [
    'Place a finished pot at the point you click on a supporting block. It does not have to sit in the middle.$(br2)Use a water bucket on its open center before adding a small flower.',
    'Empty-hand right-click or shears removes the flower. Removing it leaves the stored water. An empty bucket then recovers the water; a planted pot refuses draining.',
    'Sneak-right-click with an empty hand picks up the whole pot; mining also preserves it. Shape, glaze, stage, flower and water travel together.$(br2)Flower water is separate from the moisture used when shaping wet clay.',
], 4)

entry('painting', 'first_canvas', 'Start a Canvas', 'hobbymod:painting_easel', [
    'Craft an easel, a canvas, a palette and a paintbrush. Place the easel on a solid floor with two blocks of headroom. Right-click either easel section with the canvas to mount it.',
    'A fresh canvas asks for resolution first. Carry your palette and paintbrush; the palette also works in the offhand.$(br2)Select a dye swatch and paint by holding and dragging the mouse across the canvas.',
    'The canvas and Palette & tools are separate panels. Drag their headers to move them. Palette hides/shows tools without closing your canvas.$(br2)The world remains visible behind the editor.',
])
entry('painting', 'shapes', 'Canvas Shapes & Pixels', 'hobbymod:round_canvas', [
    'Square: 1:1.$(br)Landscape: 3:2.$(br)Portrait: 2:3.$(br)Panoramic: 2:1.$(br)Round: circular 1:1.$(br2)Shape, wall size and pixel resolution are independent choices.',
    'Pixels cycles the short edge through 16, 32, 64 and 128. Landscape at 64 is 96 x 64; panoramic at 128 is 256 x 128.$(br2)Round canvases clip to a circle, including fills and PNG exports.',
    'Changing resolution resamples existing pixels instead of clearing them. Reducing resolution loses detail; a marked canvas asks for confirmation before shrinking.$(br2)Choose higher resolution before adding fine detail.',
], 1)
entry('painting', 'dyes', 'Palette & Dye Mixing', 'hobbymod:paint_palette', [
    'Click an empty dye swatch to load matching dye from your inventory and select it. Right-click loads another dye.$(br2)One dye provides 512 paint; each color holds up to 4,096.',
    'Shift-click a swatch to select a second mixing color. Mix cycles 0%, 25%, 50%, 75% and 100%. The large swatch previews the result.$(br2)Mix with white or black for lighter or darker shades.',
    'Mixed strokes use both pigments. Pick selects a color already on the artwork, but still needs loaded paint. One paint unit covers up to 64 changed pixels in a submitted stroke batch. Identical repainting is free.',
], 2)
entry('painting', 'brushes', 'Brushes & Studio Controls', 'hobbymod:paint_brush', [
    'Brush: round; sizes 1, 2, 4, 8, 16.$(br)Box: square brush.$(br)Pen: one-pixel precision.$(br)Fill: connected color region.$(br)Soft: graded brush edges.$(br)Pick: sample an existing color.',
    'Paint percentage cycles opacity: 25%, 50%, 75%, 100%.$(br2)Hover previews the actual brush shape, size, opacity and softness. Continuous strokes connect fast mouse movements without gaps.',
    'Wheel over the canvas: zoom 1x-8x around the pointer.$(br2)Middle drag: pan.$(br2)Returning to 1x recenters the canvas. Changes save to the easel; closing waits for queued strokes. Painting has no automatic undo.',
], 3)
entry('painting', 'display', 'Sign, Hang & Export', 'hobbymod:landscape_canvas', [
    'Enter a title and choose Sign to record your name and lock the painting. Edit on the easel reopens it. Signing is optional for display.$(br2)Take returns the exact canvas and frees the easel for another.',
    'Use a canvas against a solid wall. Required area: square/round 1 x 1; landscape 3 x 2; portrait 2 x 3; panoramic 4 x 2.$(br2)The area must be fully supported and free of other paintings.',
    'Right-click a hanging painting for its title and artist. Shift-right-click with an empty hand recovers it. Punching follows normal painting drops; creative punching removes it without a drop.',
    'PNG exports the chosen pixel resolution to hobbymod-paintings inside the Minecraft game directory. Round corners are transparent. There is no PNG import.$(br2)Physical canvases keep their pixels and signature when traded or stored.',
], 4)

entry('aquariums', 'setup', 'Build a Healthy Tank', 'hobbymod:small_aquarium', [
    'Craft a tank, then place it with enough room for its footprint. Small is 1 x 1 x 1; medium 2 x 1 x 1; large 4 x 2 x 2.$(br2)Placement follows your facing. Right-click any part to open the aquarium.',
    'The care inventory shows your real supplies. Clicking a supply uses it only if the action succeeds.$(br2)Fill with water, lay sand or gravel, arrange plants, and install a filter before adding fish.',
    'Cycle a filled, installed filter using Filter Bacteria Starter: moss, bone meal and a glass bottle. It returns the bottle. Without starter, natural cycling takes five loaded minutes.',
    'Choose compatible fish for the water temperature. New fish need two minutes to acclimate. Feed, name, watch and rearrange your tank.$(br2)The Fish tab explains refusals and shows each resident\'s health, sex and age.',
])
entry('aquariums', 'species', 'Fish & Compatibility', 'hobbymod:guppy_fish', [
    'Warm water: guppy (load 1), neon tetra (1), corydoras (2), betta (2), angelfish (3), cherry barb (1).$(br2)Cool water: zebra danio (1), goldfish (3).',
    'Small tank capacity: 12 load.$(br)Medium: 30 load.$(br)Large: 84 load, at most 32 residents.$(br2)Each fish addition checks capacity, quality, cycling, temperature and compatibility.',
    'Avoid two male bettas together. Bettas cannot share with guppies; angelfish cannot share with neon tetras.$(br2)Use magma cream for warm water or a snowball for cool water.',
    'Starter bags are crafted from paper, raw fish and dye. See the recipe pages for each species.$(br2)You can also capture wild fish with a habitat net while carrying a water bucket. Captured fish keep their identity.',
], 1)
entry('aquariums', 'care', 'Feeding & Water Care', 'hobbymod:fish_food', [
    'Fish food adds 20 reserve. Feeding at 80% reserve or above lowers water quality, so do not keep topping up a full tank.$(br2)The Fish tab estimates how long its reserve will last.',
    'A water bucket fills or changes water: quality +25, algae -15. Shears clean glass: algae -30, quality +10, with tool wear.$(br2)Filters and plants reduce waste. Bright tanks grow more algae.',
    'Select a resident, then use an empty bucket to move it or a renamed name tag to name it. Drain in remove mode only after moving all residents out. Water changes are safe with fish inside.',
    'Poor water, hunger, incompatible fish or wrong temperature damages health. Healthy care restores it. At zero, fish die and remain as bodies. Remove bodies clears them; dead fish cannot be revived or captured as living fish.',
], 2)
entry('aquariums', 'aquascaping', 'Aquascaping & 3D Editing', 'hobbymod:aquarium_filter', [
    'In Aquascape, select a supply and click the top view to place it. Sand or gravel makes the bed. Seagrass, kelp, cobblestone, sticks and other blocks become decorations.$(br2)Scroll selects quarter-turn rotation.',
    'Add mode places items; Remove mode recovers their exact material. Edit in 3D opens the world camera.$(br2)Select a piece. Red, green and blue arrows move along X, Y and Z; cube handles stretch those axes.',
    'Right-drag orbits; wheel zooms. Drag a piece directly to move it across the floor; Shift-drag lifts it. Rotate turns it 90 degrees. Reset size and Reset rotation restore proportions or angles.',
    'Moving and resizing stay inside the glass. Release a handle to save the edit. Layout and material types survive reloads and carrying.$(br2)Pieces have their own identities, so another player removing one cannot redirect your edit.',
], 3)
entry('aquariums', 'breeding', 'Breeding & Moving Tanks', 'hobbymod:neon_tetra_fish', [
    'Healthy, fed, acclimated adult pairs of the same species can breed at water quality 75% or higher. There is a six-minute cooldown and at most four fry per update. Capacity and compatibility still apply.',
    'Fry inherit traits and parent identity, and mature over eight minutes. Fish care advances once per loaded minute; unloaded chunks pause it.$(br2)Keep room for young and watch food use as your community grows.',
    'Mine any aquarium part to dismantle the tank and receive one kit carrying its water, residents, care and layout. Place it again to restore them in your new facing.$(br2)Do not break neighboring blocks to move a tank.',
], 4)

entry('terrariums', 'setup', 'Build a Terrarium', 'hobbymod:terrarium', [
    'Craft eight glass around a copper ingot. The starter enclosure is 2 x 1 x 1. Place it on a solid surface and open Ecosystem or Layout.$(br2)It contains land rather than aquarium water.',
    'Add gravel drainage and choose dirt, sand or moss as the substrate. Arrange plants and ornaments. Use a mister to raise moisture and humidity; feed residents with leaf litter.',
    'Opening the lid improves ventilation and speeds drying. Closing it conserves moisture. Mist only as needed; the care screen shows moisture, humidity, light, food and comfort.',
])
entry('terrariums', 'supplies', 'Light, Heat & Supplies', 'hobbymod:terrarium_mister', [
    'Terrarium mister: moisture and humidity.$(br)Glowstone dust: light strip.$(br)Heat lamp: warmth and real block light.$(br)Magma cream: warm conditions.$(br)Snowball: mild conditions.$(br)Leaf litter: colony food.',
    'Light strips provide ecosystem light level 12. A heat lamp supplies level 14 and lights the room. Warm closed enclosures dry slightly faster.$(br2)Shears trim the most recently placed plant with height left to trim.',
    'Remove / collect returns installed supplies. Substrate cannot be removed while inhabited. Swapping substrate returns the old material.$(br2)Held supplies also work on the enclosure; sneak chooses removal.',
], 1)
entry('terrariums', 'residents', 'Residents & Wild Collection', 'hobbymod:habitat_net', [
    'Springtail and isopod colony items each introduce three tiny residents. Feed with leaf litter.$(br2)Snails, tree frogs and geckos are collected in the wild using a habitat net.',
    'Right-click a wild animal with the net. Fish additionally need a water bucket. Introduce captured residents into the appropriate enclosure.$(br2)Collection preserves individual identities instead of creating duplicate animals.',
    'Remove / collect returns up to three selected colony residents. Comfortable closed planted colonies with two mature residents can produce young every eight loaded minutes, up to 48 residents.',
    'Young grow and inherit color variants and parent identity. Captured colony items retain ages, traits and lineage. Poor conditions cost health; good care restores it.$(br2)Dead residents remain as bodies. Remove bodies cannot create living carriers.',
], 2)
entry('terrariums', 'layout', 'Layout & Rotation', 'minecraft:fern', [
    'In Layout, choose a block and click the top view. Starter plants include moss, fern, azalea, poppy and oak sapling. Other permitted blocks make rocks, branches and ornaments. At most 28 pieces fit in an enclosure.',
    'Edit in 3D: select a piece. Arrows move it and cubes resize it. Red is X, green Y, blue Z. Right-drag orbits and wheel zooms.$(br2)Use green movement to lift or bury decorations.',
    'Curved rings rotate a selected piece: red pitch, green yaw, blue roll. Shift while dragging snaps changes to 15 degrees.$(br2)Reset size restores proportions. Reset rotation clears angles while keeping position and size.',
], 3)
entry('terrariums', 'terrain', 'Shape the Landscape', 'minecraft:moss_block', [
    'In the 3D editor choose Edit terrain, then Raise, Lower or Smooth. Hold or drag on the substrate to form hills and valleys. Shift temporarily lowers; Shift + wheel changes brush size.',
    'Choose Edit objects to return to decoration handles. Dirt, sand and moss all support terrain shaping. Valleys keep a minimum layer above drainage.$(br2)Decorations and crawling residents stay grounded on the shaped terrain.',
    'Mining the enclosure returns one kit with care, inhabitants, angles and terrain saved. Carrying or unloading pauses the simulation; it does not punish the colony with offline neglect.',
], 4)

entry('bonsai', 'planting', 'Plant a Miniature Tree', 'hobbymod:bonsai_pot', [
    'Craft a bonsai pot from five bricks and dirt. Place it and use an oak, birch or cherry sapling to plant a tree.$(br2)Each tree has its own age, health, branches and wiring.',
    'Choose a spot with light level 9 or greater above the pot. Growth and care update each loaded minute. Healthy trees bud about every two intervals, up to 28 branch segments.',
    'New branches unfold over one loaded minute rather than appearing at full size. Carrying or unloading pauses growth.$(br2)Mine the pot to carry its whole tree; replacing it preserves age, health and shape.',
])
entry('bonsai', 'care', 'Water, Soil & Roots', 'minecraft:water_bucket', [
    'Aim at the pot to see water, health and age. Sneak for soil and root ages. Empty-hand right-click inspects care.$(br2)Water with a water bucket; survival returns its empty bucket.',
    'Water from 15% to 85% is healthy. Watering a tree already above 75% causes stress. Fresh soil lasts about 40 care intervals; roots crowd after 60.$(br2)Light, moderate water and fresh soil support recovery.',
    'Right-click with dirt to repot; it refreshes soil and sets water to 55%. Sneak-use shears to prune roots. These are different actions, each with a 12-interval cooldown.$(br2)Pruning roots does not replace soil.',
    'Neglect, overwatering, pruning and excessive bending cost health. Good care can restore even a tree at zero health. New branches need health 35 or higher.$(br2)Avoid repeated intervention while a tree is stressed.',
], 1)
entry('bonsai', 'pruning', 'Leaves, Branches & Wiring', 'minecraft:shears', [
    'Aim shears at a branch. First click removes its foliage and its twigs\' foliage; the second cuts the branch and its descendants. The base trunk cannot be cut, although its leaves can be removed.',
    'Aim at a branch and use a copper ingot to wire/bend it. Each operation uses one ingot and adds 12 degrees of bend. Sneak reverses both the turn and outward bend.$(br2)A seventh bend breaks that subtree.',
    'Branch pruning costs health for each removed segment. Shears wear in survival. Leaf removal is gentler than cutting; healthy new buds can restore foliage.$(br2)Watch the actual branch you target before clicking again.',
], 2)

entry('dj', 'first_track', 'Build Your First Groove', 'hobbymod:dj_workstation', [
    'Craft a DJ workstation and right-click its desk or monitor. Start in Patterns. Click or drag steps to add beats; right-click erases. Select a channel by clicking its name.',
    'Each of eight patterns A-H holds all eight instrument channels. Assign sounds, add drums, and build melodies in Piano.$(br2)Files > Demo gives an editable starter groove after a confirming click.',
    'Select Pat A-H to preview a pattern, or Song to play the arrangement. Play or Space starts/stops. An unarranged project automatically previews its selected pattern.$(br2)Tempo ranges from 60 to 200 BPM; Swing delays alternate steps.',
])
entry('dj', 'patterns', 'Patterns & Piano Roll', 'minecraft:note_block', [
    'Each pattern is one bar. Click the channel\'s instrument button for the paged sound picker; choosing previews it. Copy > copies all channels into the next pattern. Clear confirms before clearing the whole pattern.',
    'In Piano, draw a note and drag to choose duration. Drag a note to move it; Shift-drag resizes it. Right-click deletes. Scroll a note for velocity, or the keyboard for pitch range. Oct shifts by an octave.',
    'Chords are supported. Notes span C2-C6, with at most 48 notes per channel per pattern. A note can last the full bar.$(br2)The channel and pattern buttons cycle what you edit.',
], 1)
entry('dj', 'song', 'Layer Complete Patterns', 'hobbymod:blank_music_disc', [
    'Song places full pattern clips into lane/bar cells. A clip plays every instrument in that pattern.$(br2)Put A and B on different lanes at the same bar to layer both complete patterns. Lanes are not instrument channels.',
    'Click/drag places clips; right-click erases. Each song has eight lanes and up to 64 bars. Page switches four pages of 16 bars; Bars sets playback length.$(br2)Shift-click a bar to cue it.',
    'Click a lane name to select it. Repeat L1-L8 fills only that lane with the chosen pattern. Clear song asks for confirmation.$(br2)Clip colors identify patterns, so layered sections are easy to follow.',
], 2)
entry('dj', 'instruments', 'The Sound Library', 'hobbymod:dj_headphones', [
    'Drums: kick, snare, hi-hat, clap, tom, rim, shaker, cowbell, crash and ride.$(br2)Bass/synth: bass, sub bass, acid bass, lead, pluck and pad.',
    'Keys/tuned percussion: keys, piano, electric piano, organ, bell, vibraphone, marimba and glockenspiel.$(br2)Acoustic-style: guitar, strings, choir, flute and brass.',
    'These 29 instruments are synthesized sounds. Choose from the native paged picker in Patterns. Each channel gets one instrument, but a whole pattern can use all eight channels.',
], 3)
entry('dj', 'mixer', 'Mixer & Speakers', 'hobbymod:dj_speaker', [
    'Mixer has eight channel strips. Drag volume for gain. M mutes; S solos. Multiple solos can play together.$(br2)P sets pan, F filter, D timed delay and R reverb send. Scroll parameters for finer changes.',
    'Files > Master changes overall output. Minecraft\'s Jukebox/Note Blocks sound slider controls this music.$(br2)A desk reaches 24 blocks. A speaker within two blocks horizontally and one vertically extends it to 48.',
    'More speakers do not multiply volume. Closing the editor does not stop public playback. Breaking or unloading the desk stops sound; loading it preserves songs but starts with playback stopped.',
], 4)
entry('dj', 'performance', 'Perform a Two-Deck Set', 'hobbymod:dj_headphones', [
    'Deck A and Deck B keep independent projects. The top A/B buttons choose the deck to edit. DJ offers play/stop, cue-to-start, sync, loop, disc loading and headphone monitoring for each.',
    'Sync matches the other deck\'s tempo and beat clock. If it is playing, the synced deck starts too. Drag the crossfader to blend both, or choose A, Center or B.',
    'Carry Studio Headphones for Monitor: a private stream for cueing while the public deck is faded out. Monitor again or close the studio to stop it.$(br2)Hearing a deck publicly and privately adds both signals.',
], 5)
entry('dj', 'files', 'Save, Discs & Files', 'hobbymod:blank_music_disc', [
    'Edits save automatically. Ctrl+S flushes changes. Ctrl+Z undoes music edits; Ctrl+Shift+Z or Ctrl+Y redoes them. This editor history is separate from permanent carving.',
    'Carry a blank music disc and choose Disc or Files > Write music disc. Survival uses one blank and returns an editable project disc.$(br2)Hold a recorded disc in either hand and Load disc into a deck. It is not a vanilla jukebox disc.',
    'Files > Export project writes .hobbytrack into hobbymod/music in your Minecraft instance. Put received files there, then Import project.$(br2)Export WAV renders the composed arrangement as stereo audio.',
    'WAV exports the song, not live crossfader gestures. Each workstation keeps both deck projects through pickup and replacement.$(br2)One nearby player edits at a time; others hear public playback.',
], 6)

entry('winery', 'vineyard', 'Grow a Vineyard', 'hobbymod:grape_trellis', [
    'Place a trellis on dirt, grass or farmland. Plant a red or white cutting and give the vine light.$(br2)Stack trellises for a taller vineyard. One cutting plants connected empty sections of the column.',
    'Each planted section grows its own fruit. Bone meal ripens the section you click up to the ripe stage.$(br2)Only the bottom of a connected trellis column needs soil; newly stacked sections inherit its cultivar.',
    'Right-click fruit to harvest. Firm fruit gives three grapes; ripe, late and overripe give four. Harvested sections return to an early growth stage and regrow.$(br2)Outdoor sun-grown grapes gain a small quality bonus.',
    'Firm green grapes are tart and lower quality. Ripe red/pale fruit is reliable. Dark red/golden late harvest is sweeter. Brown overripe fruit is sweet but lower quality.$(br2)Harvest timing changes your wine.',
])
entry('winery', 'treading', 'Tread Grapes by Foot', 'hobbymod:grape_treading_tub', [
    'Craft a treading tub from five planks. Right-click it with eight grapes; red and white can be mixed.$(br2)Right-click with an empty bucket before treading. The tub needs a bucket to begin pressing.',
    'Step over the shallow rim and stand inside. Six seconds of treading presses the grapes. Stepping out pauses progress.$(br2)Your feet power the tub; redstone cannot replace a player.',
    'When pressing finishes, empty-hand right-click to take the filled must bucket. Finished output waits until collected.$(br2)Crouch and empty-hand click to retrieve supplies that have not been reserved for pressing.',
    'Hoppers can supply grapes and an empty bucket, and collect the finished must below. A player must still stand inside to tread.$(br2)Breaking unfinished machinery refunds its reserved supplies.',
], 1)
entry('winery', 'pressing', 'Mechanical Press & Blends', 'hobbymod:grape_press', [
    'Open a grape press and add eight grapes plus an empty bucket. Two grape slots let you blend harvests and colors. Click Press, then collect the must after three seconds.',
    'Mostly red makes red wine; mostly white makes white wine. Middle blends make rose. Ripeness and sun exposure carry into the batch.$(br2)The vintage is the world day when pressing starts.',
    'A redstone signal starts a supplied mechanical press automatically. Hoppers supply ingredients from above/sides and collect output below.$(br2)Breaking a press refunds its reserved fruit and bucket.',
], 2)
entry('winery', 'fermentation', 'Ferment & Rack', 'hobbymod:fermentation_barrel', [
    'Put must and one wine yeast in a fermentation barrel. Click Start and take the returned empty bucket. Fermentation takes one minute.$(br2)Keep the barrel away from fire, lit campfires and lava.',
    'When fermented, click Rack to start aging in the same barrel. It represents removing sediment; no second machine is needed.$(br2)There is no destructive fermentation failure.',
    'Rinse an empty used barrel with a water bucket in its must slot, then Rinse. Dirty barrels still ferment, but the next batch is cloudy and loses quality.$(br2)Rinsing returns the empty bucket.',
    'Breaking an active barrel drops a portable wine cask. Put it into another barrel and Resume. Moving a cask does not create another empty bucket.$(br2)The batch keeps its identity.',
], 3)
entry('winery', 'aging', 'Age, Label & Bottle', 'hobbymod:wine_rack', [
    'Age wine in a cool dark cellar. White wine\'s recommended age is one minute; rose 90 seconds; red two minutes. The barrel shows age, quality and conditions.',
    'You can bottle early. Leaving it past three times its recommended age lowers quality.$(br2)Add glass bottles, optionally type a label, then Bottle. A batch fills four bottles, using one glass bottle each.',
    'You can bottle part now and the rest later. The first bottling fixes that vintage\'s label and tasting record. Different batches never stack together.$(br2)Labels do not erase the batch identity.',
    'Fermentation and aging catch up by elapsed world time when chunks load again. A stopped server does not add real-world hours. Unloaded periods use the last sampled cellar conditions.',
], 4)
entry('winery', 'rack_taste', 'Display & Taste', 'hobbymod:wine_rack', [
    'A rack holds four bottles. Use a bottle on a particular slot to display it; empty-hand right-click that slot takes it back.$(br2)Stack racks without gaps. New racks align to the neighboring rack\'s facing.',
    'Hold right-click with a wine bottle to drink. Survival returns its glass. Short tasting traits appear one at a time above the hotbar with a gentle movement, such as Dark berries... then Oaky...',
    'Quality 80 or above grants 30 seconds of Haste. Taste depends on the vintage\'s fruit, aging, oak and barrel cleanliness.$(br2)Wine moved to another world keeps its label/appearance, but original tasting data belongs to its source world.',
], 5)
entry('winery', 'automation', 'Cellar Automation', 'minecraft:hopper', [
    'Hoppers feed machines from above or the sides. Output hoppers take must, wine and returned barrel buckets.$(br2)A powered barrel starts supplied must, racks when fermented, and bottles at recommended age.',
    'Keep returned buckets extracted so the barrel can accept another must. Supply yeast and glass bottles. Rinsing is manual.$(br2)Tubs need grapes, an empty bucket and a player inside to press; hoppers can collect the finished must.',
], 6)

# Recipe pages use live recipe IDs, so ingredients and outputs follow the game.
GROUPS = {
    'sculpting': ['marble', 'chisel', 'point_chisel', 'roughing_mallet', 'polishing_rasp', 'marble_blueprint'],
    'pottery': ['prepared_clay', 'pottery_wheel', 'pottery_rib', 'pottery_sponge', 'pottery_loop', 'pottery_wire', 'pottery_kiln'],
    'painting': ['painting_easel', 'paint_brush', 'paint_palette', 'square_canvas', 'landscape_canvas', 'portrait_canvas', 'panorama_canvas', 'round_canvas'],
    'aquariums': ['small_aquarium', 'medium_aquarium', 'large_aquarium', 'aquarium_filter', 'aquarium_filter_starter', 'fish_food', 'guppy_fish', 'neon_tetra_fish', 'zebra_danio_fish', 'goldfish_fish', 'corydoras_fish', 'betta_fish', 'angelfish_fish', 'cherry_barb_fish'],
    'terrariums': ['terrarium', 'terrarium_mister', 'terrarium_heat_lamp', 'habitat_net', 'leaf_litter', 'springtail_colony', 'isopod_colony'],
    'bonsai': ['bonsai_pot'],
    'dj': ['dj_workstation', 'dj_speaker', 'dj_headphones', 'blank_music_disc'],
    'winery': ['grape_trellis', 'red_grape_cutting', 'white_grape_cutting', 'red_grape_cutting_from_grapes', 'white_grape_cutting_from_grapes', 'grape_press', 'grape_treading_tub', 'fermentation_barrel', 'wine_rack', 'wine_yeast'],
}
GLAZES = [f'{c}_pottery_glaze' for c in ('white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black')]
lang = json.loads((RES / 'assets/hobbymod/lang/en_us.json').read_text())

def recipe_entry(category, key, ids, name='Crafting Recipes', order=90):
    pages = []
    for start in range(0, len(ids), 2):
        pair = ids[start:start+2]
        page = {'type': 'patchouli:crafting', 'recipe': 'hobbymod:' + pair[0]}
        if len(pair) == 2:
            page['recipe2'] = 'hobbymod:' + pair[1]
        pages.append(page)
    # Names on separate entries make supplies searchable from the book index.
    put(ASSETS / 'entries' / category / (key + '.json'), {
        'name': name, 'icon': ('minecraft:book' if ids[0] == 'hobby_handbook' else json.loads((RES / 'data/hobbymod/recipe' / (ids[0] + '.json')).read_text())['result']['id']),
        'category': 'hobbymod:' + category, 'sortnum': order, 'pages': pages,
    })

for cat, ids in GROUPS.items():
    for i, rid in enumerate(ids):
        recipe = json.loads((RES / 'data/hobbymod/recipe' / (rid + '.json')).read_text())
        result = recipe['result']['id']
        default_name = rid.replace('_', ' ').title()
        title = lang.get('item.' + result.replace(':', '.'), lang.get('block.' + result.replace(':', '.'), default_name))
        recipe_entry(cat, 'recipe_' + rid, [rid], title, 90 + i)
recipe_entry('pottery', 'glaze_recipes', GLAZES, 'All Sixteen Glazes', 110)
recipe_entry('getting_started', 'recipe_handbook', ['hobby_handbook'], 'Craft Your Handbook', 90)

put(RES / 'data/hobbymod/recipe/hobby_handbook.json', {
    'type': 'minecraft:crafting_shapeless', 'category': 'misc',
    'ingredients': [{'item': 'minecraft:book'}, {'item': 'minecraft:green_dye'}],
    'result': {'id': 'patchouli:guide_book', 'count': 1, 'components': {'patchouli:book': BOOK}},
})
put(RES / 'data/hobbymod/advancement/recipes/hobby_handbook.json', {
    'parent': 'minecraft:recipes/root',
    'criteria': {'book': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': ['minecraft:book']}]}}},
    'requirements': [['book']], 'rewards': {'recipes': ['hobbymod:hobby_handbook']},
})

# No entries, filler pages, telescope recipes or explanatory placeholder text for Astronomy.
assert not any('/entries/astronomy/' in str(p) for p in FILES)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    for path, value in FILES.items():
        if args.check:
            assert path.exists() and path.read_text() == value, f'Outdated generated book file: {path}'
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(value)
    entries = [json.loads(s) for p, s in FILES.items() if '/entries/' in str(p)]
    print(f'{len(CATEGORIES)} categories, {len(entries)} entries, {sum(len(e["pages"]) for e in entries)} pages; Astronomy empty.')

if __name__ == '__main__':
    main()
