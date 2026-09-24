# Changelog

## 4.2.0

- `IWorldGenHelper#addCustomSpawner` adds a custom spawner to every server level, beside vanilla's
  own, on both loaders. A factory makes each level its own copy.
- Mods can add creatures now. `IPlatformHelper#registerEntityAttributes` and
  `#registerSpawnPlacement` register a mob's attributes and where it may spawn, and
  `IWorldGenHelper#addSpawnToBiomes` adds it to the natural spawns of the biomes. 
  Spawns are added in the same order on both loaders.
- `IWorldGenHelper#addSpawnToStructures` adds a creature to the natural spawns inside the structures
  in a tag, wherever one of their pieces is, underground included. It is added to what already
  spawns there, and nothing is taken away.
- Long item descriptions in tooltips wrap onto several lines instead of running across the screen.

## 4.1.0

- Features added to biomes now go in the same order on both loaders, so one seed gives one
  world. A feature's place in its generation step decides the seed it is placed from, and the
  NeoForge side added them in registration order while Fabric sorts by the placed feature's id -
  identical terrain, every scattered feature somewhere else. NeoForge now sorts the same way.
- Added the instruction manual: an in-game book that any mod depending on Assorted Lib can add a
  section to. Chapters are read from `assets/<modid>/manual/*.json`, so a resource pack can extend
  or rewrite them. Right clicking a block, item or creature that has a page opens the book there,
  and the book shows two pages at once.
- Recipe pages are drawn on the screen of the container that makes them, taken from that container's own texture. A container's fuel and tool slots are drawn too, cycling through what they accept.
- The whole manual is data: the book's own look, which mods are in the index, their chapters, which
  block, item or creature opens which page, and how each kind of recipe is drawn all come from
  resource packs, so a pack can move a slot, re-point a link, re-skin the book or rewrite a chapter
  without touching a mod.
- Holding the manual puts a green check mark beside the crosshair when whatever it is on has a page,
  so a link is visible before it is clicked. Turn it off with `manual.showPageIndicator`.
- Right clicking an item frame with the manual opens the page of the item on display.

## 4.0.1

- An item transfer that is rolled back no longer rebuilds the slot it touched from a single stack,
  so an inventory holding more than a stack in a slot keeps everything in it.
- On Fabric, a block whose slot count changes while it runs is no longer frozen at the count it had
  when another mod first looked it up.

## 4.0.0

Updated to Minecraft 26.2, for NeoForge and Fabric.

- Rebuilt on the 26.2 registry, model, networking and inventory APIs.
