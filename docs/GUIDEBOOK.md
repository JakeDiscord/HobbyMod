# The Hobby Handbook

Install Patchouli **1.21.1-93-NEOFORGE** alongside HobbyMod and Architectury.
Gradle supplies Patchouli for development; release jars require the separate
Patchouli mod. Its Maven download host is `maven.blamejared.com`.

Craft the handbook with one book and one green dye, in any arrangement. It also
appears in the HobbyMod creative tab. Right-click to open it. Search,
bookmarks, clickable cross-references and live crafting recipe pages use
Patchouli's native interface.

The universal book covers marble sculpture, pottery, painting, aquariums,
terrariums, bonsai, DJ/music and Winery. Each chapter explains the workflow,
controls, crafting, care or finishing, and practical problems. Astronomy is
a visible empty chapter with no entries or explanatory filler.

The book contains **103 entries and 213 pages**, with **74 crafting recipe
references** covering every non-Astronomy crafting recipe. The marble
stonecutter conversion is explained in the sculpture introduction. All
sixteen pottery glazes have live recipe pages.

## Maintaining the book

Author text and recipe groupings in `tools/guidebook/generate_book.py`, then run:

```sh
python3 tools/guidebook/generate_book.py
python3 tools/guidebook/generate_book.py --check
python3 tools/guidebook/validate_book.py
```

The validator checks links, categories, recipe coverage, item icon models,
text-page lengths and the empty Astronomy chapter. Keep paragraphs short enough
for Patchouli's ordinary pages and check changes in Minecraft.

The definition lives in `data/hobbymod/patchouli_books/hobbies/book.json`.
English categories and entries live in
`assets/hobbymod/patchouli_books/hobbies/en_us`. Resource packs can translate or
extend them. No Patchouli Java classes enter the shared gameplay module. NeoForge copies
the shared book definition into its development resources because Patchouli
discovers definitions through the owning mod, while Loom loads common as a
separate generated mod. The release jar contains one definition.

To obtain the native guide book in development:

```mcfunction
/give @s patchouli:guide_book[patchouli:book="hobbymod:hobbies"]
```

## Recovery

The authoring generator and validator were recovered from the original
"Set up HobbyMod - Janoc" conversation's command history. This restores its
book structure and text rather than recreating them from memory.

The Winery chapter describes the gameplay in this checkout: load an empty
bucket before treading, then collect the finished must with an empty hand.
Connected trellis sections grow their fruit separately. The original book
also described unpublished Winery changes that are absent from this checkout;
those gameplay changes have not been restored as part of the handbook work.

Restoration validation: the normal Gradle build passed, with 95 JUnit tests
passing and zero skips. The generator check and validator passed. The release
jar contains one book definition, all 103 entries, and no GameTest classes.

A graphical Minecraft 1.21.1 client connected to the loopback development
server with the official Patchouli artifact. Crafting a book plus green dye
produced the handbook with the correct `patchouli:book` component. The book
opened; chapter navigation, live crafting pages and all sixteen glaze recipes
rendered across four spreads. Astronomy displayed no entries. These are
representative visual checks, not a manual review of every page.

Screenshots from this run: [index](guidebook-index.png),
[live recipe](guidebook-crafting.png), [glazes](guidebook-glazes.png),
and [empty Astronomy](guidebook-astronomy.png).
