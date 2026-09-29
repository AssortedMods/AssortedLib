# Changelog

## 4.3.0

- `IPlatformHelper#registerFlammable` lets fire spread to and burn away a mod's block, with the
  same odds on both loaders.
- `PlayerDeathDropsEvent` fires once a player's death is final, after any totem, and before their
  inventory and experience drop. Fabric's own death events come either before the totem or after
  the drops.
- `IClientHelper#registerLevelSubmit` adds geometry to every level frame, after entities and
  block entities, on both loaders.
- Registry aliases, for a mod that moves its content to a new id: `IRegistryFactory#alias`, or
  `RegistryProvider#aliasFrom(oldNamespace)` to alias everything a provider registers. A world
  saved under the old ids loads the same entities, items and blocks.
- `SharedCreativeTabs` gives a family of mods one creative tab between them, registered by
  whichever is installed first and filled in a fixed order.
- A mod's manual chapters can join another namespace's section: `LibManualProvider` takes a
  manual namespace, and a section's `icon` can list several items, the first registered drawn.
- `MovedIds` keeps a player's recipe book and advancement progress when a mod moves its recipes and
  advancements to new ids: `inherit(oldNamespace, newNamespace)`, and `renameCriteria` for criteria
  that were renamed. It also carries over structures already in a world and the loot of chests
  that were never opened. `migration.carryOverMovedIds` in the new `assortedlib-common.toml` (`.json` on Fabric) turns it off.
- `ICanColor` marks a block a paint roller can recolor, so blocks from one mod take another mod's paint.
- `AdvancementIcons` draws an advancement with the first of several items that is installed, for a
  root advancement a family of mods ships between them.
- Mods can join a family to share a creative tab and manual section, and each one can be turned off in the family's parts config.
- `creative.hideUncraftableItems` in `assortedlib-common.toml` (`.json` on Fabric) hides items from the creative menu when nothing installed
  provides their material. Mods add those items with `CreativeTabItems#addIfObtainable`, and `ItemUtil.isTagEmpty` does the check.
- The storage pieces Assorted Storage's parts share now live here, so they can become separate mods.
  `core.storage` has `StorageMaterial`, the base storage block, block entity and menus, and
  `core.storage.ender` the locked ender inventories. Their screens are in `client.screen.storage`.
- The chest, barrel, hopper and shulker box blocks are here too, with their block entities, menus,
  renderers and models, so Assorted Locks can own the locked vanilla ones while each part keeps its
  materials. Nothing is registered by Lib. A mod hands its blocks the block entity and menu types it
  registered through `StorageTypes`, and a block entity takes its type from its block.
- `LockConversions` says what a lock turns a block into, so one padlock locks the containers of
  whichever mods are installed. `LockItems` and the new `c:locks` and `c:keys` tags let a container
  take a lock or check a key without knowing the mod that adds them.
- `LevelUpgrades` says how a mod's containers upgrade from one material to the next, so a level
  upgrade works on all of them.
- `StorageAccessUtil` moved here too, and `registerKeySource` adds somewhere else a key can be, such
  as an accessory slot.
- `SharedDataComponents` gives a family of mods one data component type between them, registered
  under its id by whichever asks first. `StorageInfo`, the lock and level lines on storage items, uses
  it, and the shared container backgrounds are Lib's own textures now.
- `core.tool` holds the tool materials Assorted Tools' parts share. `ToolTiers.get()` gives wood through
  netherite and 17 extra materials such as tin and ruby, from `assortedlib-tool-tiers.toml`, which only
  exists once a mod asks for it. `ArmorMaterialConfig`, `ConfigurableArmorItem`, `ConfigurableTieredItem`
  and `HarvestTiers` came with them.
- `ISwitchModes` items share one switch-modes key, Z by default. A mod turns it on with
  `ModeSwitching.enable()` and, on the client, `ModeSwitchKey.enable()`; without one, there is no key
  and no packet.
- `MovedIds` also carries over data-driven entries saved under an old id, such as an enchantment on an
  item or a book, which vanilla would drop.
- `IConditionHelper#conditionalOutput` takes a function from a recipe id to its conditions instead of a map. A mod that calls it directly can pass `map::get`.
- Fixed recipe conditions, tooltips and biome changes that could go missing on NeoForge when several mods added them at the same time while loading.

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
