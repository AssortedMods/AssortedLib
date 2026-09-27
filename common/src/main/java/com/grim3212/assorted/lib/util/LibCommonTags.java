package com.grim3212.assorted.lib.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public class LibCommonTags {

    /** The namespace of cross-loader convention tags; both loaders use {@code c}. */
    public static final String COMMON_NAMESPACE = "c";

    public static class Blocks {
        public static final TagKey<Block> STONE = commonTag("stones");
        public static final TagKey<Block> BARRELS = commonTag("barrels");
        public static final TagKey<Block> BARRELS_WOODEN = commonTag("barrels/wooden");
        public static final TagKey<Block> BOOKSHELVES = commonTag("bookshelves");
        public static final TagKey<Block> CHESTS = commonTag("chests");
        public static final TagKey<Block> CHESTS_ENDER = commonTag("chests/ender");
        public static final TagKey<Block> CHESTS_TRAPPED = commonTag("chests/trapped");
        public static final TagKey<Block> CHESTS_WOODEN = commonTag("chests/wooden");
        public static final TagKey<Block> COBBLESTONE = commonTag("cobblestones");
        public static final TagKey<Block> END_STONES = commonTag("end_stones");
        public static final TagKey<Block> FENCE_GATES = commonTag("fence_gates");
        public static final TagKey<Block> FENCE_GATES_WOODEN = commonTag("fence_gates/wooden");
        public static final TagKey<Block> FENCES = commonTag("fences");
        public static final TagKey<Block> FENCES_NETHER_BRICK = commonTag("fences/nether_brick");
        public static final TagKey<Block> FENCES_WOODEN = commonTag("fences/wooden");
        public static final TagKey<Block> GRAVEL = commonTag("gravels");
        public static final TagKey<Block> NETHERRACK = commonTag("netherracks");
        public static final TagKey<Block> OBSIDIAN = commonTag("obsidians");
        public static final TagKey<Block> ORES = commonTag("ores");
        public static final TagKey<Block> ORES_COAL = commonTag("ores/coal");
        public static final TagKey<Block> ORES_COPPER = commonTag("ores/copper");
        public static final TagKey<Block> ORES_DIAMOND = commonTag("ores/diamond");
        public static final TagKey<Block> ORES_EMERALD = commonTag("ores/emerald");
        public static final TagKey<Block> ORES_GOLD = commonTag("ores/gold");
        public static final TagKey<Block> ORES_IRON = commonTag("ores/iron");
        public static final TagKey<Block> ORES_LAPIS = commonTag("ores/lapis");
        public static final TagKey<Block> ORES_NETHERITE_SCRAP = commonTag("ores/netherite_scrap");
        public static final TagKey<Block> ORES_QUARTZ = commonTag("ores/quartz");
        public static final TagKey<Block> ORES_REDSTONE = commonTag("ores/redstone");
        public static final TagKey<Block> STORAGE_BLOCKS = commonTag("storage_blocks");
        public static final TagKey<Block> STORAGE_BLOCKS_AMETHYST = commonTag("storage_blocks/amethyst");
        public static final TagKey<Block> STORAGE_BLOCKS_COAL = commonTag("storage_blocks/coal");
        public static final TagKey<Block> STORAGE_BLOCKS_COPPER = commonTag("storage_blocks/copper");
        public static final TagKey<Block> STORAGE_BLOCKS_DIAMOND = commonTag("storage_blocks/diamond");
        public static final TagKey<Block> STORAGE_BLOCKS_EMERALD = commonTag("storage_blocks/emerald");
        public static final TagKey<Block> STORAGE_BLOCKS_GOLD = commonTag("storage_blocks/gold");
        public static final TagKey<Block> STORAGE_BLOCKS_IRON = commonTag("storage_blocks/iron");
        public static final TagKey<Block> STORAGE_BLOCKS_LAPIS = commonTag("storage_blocks/lapis");
        public static final TagKey<Block> STORAGE_BLOCKS_NETHERITE = commonTag("storage_blocks/netherite");
        public static final TagKey<Block> STORAGE_BLOCKS_QUARTZ = commonTag("storage_blocks/quartz");
        public static final TagKey<Block> STORAGE_BLOCKS_RAW_COPPER = commonTag("storage_blocks/raw_copper");
        public static final TagKey<Block> STORAGE_BLOCKS_RAW_GOLD = commonTag("storage_blocks/raw_gold");
        public static final TagKey<Block> STORAGE_BLOCKS_RAW_IRON = commonTag("storage_blocks/raw_iron");
        public static final TagKey<Block> STORAGE_BLOCKS_REDSTONE = commonTag("storage_blocks/redstone");
        public static final TagKey<Block> GLASS = commonTag("glass_blocks");
        public static final TagKey<Block> GLASS_BLACK = commonTag("glass_blocks/black");
        public static final TagKey<Block> GLASS_BLUE = commonTag("glass_blocks/blue");
        public static final TagKey<Block> GLASS_BROWN = commonTag("glass_blocks/brown");
        public static final TagKey<Block> GLASS_COLORLESS = commonTag("glass_blocks/colorless");
        public static final TagKey<Block> GLASS_CYAN = commonTag("glass_blocks/cyan");
        public static final TagKey<Block> GLASS_GRAY = commonTag("glass_blocks/gray");
        public static final TagKey<Block> GLASS_GREEN = commonTag("glass_blocks/green");
        public static final TagKey<Block> GLASS_LIGHT_BLUE = commonTag("glass_blocks/light_blue");
        public static final TagKey<Block> GLASS_LIGHT_GRAY = commonTag("glass_blocks/light_gray");
        public static final TagKey<Block> GLASS_LIME = commonTag("glass_blocks/lime");
        public static final TagKey<Block> GLASS_MAGENTA = commonTag("glass_blocks/magenta");
        public static final TagKey<Block> GLASS_ORANGE = commonTag("glass_blocks/orange");
        public static final TagKey<Block> GLASS_PINK = commonTag("glass_blocks/pink");
        public static final TagKey<Block> GLASS_PURPLE = commonTag("glass_blocks/purple");
        public static final TagKey<Block> GLASS_RED = commonTag("glass_blocks/red");
        public static final TagKey<Block> GLASS_TINTED = commonTag("glass_blocks/tinted");
        public static final TagKey<Block> GLASS_WHITE = commonTag("glass_blocks/white");
        public static final TagKey<Block> GLASS_YELLOW = commonTag("glass_blocks/yellow");
        public static final TagKey<Block> GLASS_PANES = commonTag("glass_panes");
        public static final TagKey<Block> GLASS_PANES_BLACK = commonTag("glass_panes/black");
        public static final TagKey<Block> GLASS_PANES_BLUE = commonTag("glass_panes/blue");
        public static final TagKey<Block> GLASS_PANES_BROWN = commonTag("glass_panes/brown");
        public static final TagKey<Block> GLASS_PANES_COLORLESS = commonTag("glass_panes/colorless");
        public static final TagKey<Block> GLASS_PANES_CYAN = commonTag("glass_panes/cyan");
        public static final TagKey<Block> GLASS_PANES_GRAY = commonTag("glass_panes/gray");
        public static final TagKey<Block> GLASS_PANES_GREEN = commonTag("glass_panes/green");
        public static final TagKey<Block> GLASS_PANES_LIGHT_BLUE = commonTag("glass_panes/light_blue");
        public static final TagKey<Block> GLASS_PANES_LIGHT_GRAY = commonTag("glass_panes/light_gray");
        public static final TagKey<Block> GLASS_PANES_LIME = commonTag("glass_panes/lime");
        public static final TagKey<Block> GLASS_PANES_MAGENTA = commonTag("glass_panes/magenta");
        public static final TagKey<Block> GLASS_PANES_ORANGE = commonTag("glass_panes/orange");
        public static final TagKey<Block> GLASS_PANES_PINK = commonTag("glass_panes/pink");
        public static final TagKey<Block> GLASS_PANES_PURPLE = commonTag("glass_panes/purple");
        public static final TagKey<Block> GLASS_PANES_RED = commonTag("glass_panes/red");
        public static final TagKey<Block> GLASS_PANES_WHITE = commonTag("glass_panes/white");
        public static final TagKey<Block> GLASS_PANES_YELLOW = commonTag("glass_panes/yellow");
        public static final TagKey<Block> STAINED_GLASS = commonTag("stained_glass");
        public static final TagKey<Block> STAINED_GLASS_PANES = commonTag("stained_glass_panes");

        /// ====================================================================
        /// Below are tags that need to be generated on both Fabric and Forge
        /// ====================================================================
        public static final TagKey<Block> CONCRETE = commonTag("concretes");
        public static final TagKey<Block> CONCRETE_POWDER = commonTag("concrete_powders");

        private static TagKey<Block> commonTag(String name) {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(COMMON_NAMESPACE, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> STONE = commonTag("stones");
        public static final TagKey<Item> DYES = commonTag("dyes");
        public static final TagKey<Item> DYES_BLACK = commonTag("dyes/black");
        public static final TagKey<Item> DYES_RED = commonTag("dyes/red");
        public static final TagKey<Item> DYES_GREEN = commonTag("dyes/green");
        public static final TagKey<Item> DYES_BROWN = commonTag("dyes/brown");
        public static final TagKey<Item> DYES_BLUE = commonTag("dyes/blue");
        public static final TagKey<Item> DYES_PURPLE = commonTag("dyes/purple");
        public static final TagKey<Item> DYES_CYAN = commonTag("dyes/cyan");
        public static final TagKey<Item> DYES_LIGHT_GRAY = commonTag("dyes/light_gray");
        public static final TagKey<Item> DYES_GRAY = commonTag("dyes/gray");
        public static final TagKey<Item> DYES_PINK = commonTag("dyes/pink");
        public static final TagKey<Item> DYES_LIME = commonTag("dyes/lime");
        public static final TagKey<Item> DYES_YELLOW = commonTag("dyes/yellow");
        public static final TagKey<Item> DYES_LIGHT_BLUE = commonTag("dyes/light_blue");
        public static final TagKey<Item> DYES_MAGENTA = commonTag("dyes/magenta");
        public static final TagKey<Item> DYES_ORANGE = commonTag("dyes/orange");
        public static final TagKey<Item> DYES_WHITE = commonTag("dyes/white");
        public static final TagKey<Item> STRING = commonTag("strings");
        // The five tool types and the four armour slots are vanilla tags in 26.2: c:tools is
        // defined as #minecraft:swords, #minecraft:axes ... and c:armors/humanoid as
        // #minecraft:head_armor ... Use ItemTags.SWORDS / ItemTags.HEAD_ARMOR directly.
        public static final TagKey<Item> TOOLS = commonTag("tools");
        public static final TagKey<Item> TOOLS_SHIELDS = commonTag("tools/shield");
        public static final TagKey<Item> TOOLS_BOWS = commonTag("tools/bow");
        public static final TagKey<Item> TOOLS_CROSSBOWS = commonTag("tools/crossbow");
        public static final TagKey<Item> TOOLS_FISHING_RODS = commonTag("tools/fishing_rod");
        public static final TagKey<Item> TOOLS_TRIDENTS = commonTag("tools/trident");
        public static final TagKey<Item> TOOLS_MELEE_WEAPONS = commonTag("tools/melee_weapon");
        public static final TagKey<Item> TOOLS_MINING_TOOLS = commonTag("tools/mining_tool");
        public static final TagKey<Item> ARMORS = commonTag("armors");
        public static final TagKey<Item> ORES = commonTag("ores");
        public static final TagKey<Item> ORES_COAL = commonTag("ores/coal");
        public static final TagKey<Item> ORES_COPPER = commonTag("ores/copper");
        public static final TagKey<Item> ORES_DIAMOND = commonTag("ores/diamond");
        public static final TagKey<Item> ORES_EMERALD = commonTag("ores/emerald");
        public static final TagKey<Item> ORES_GOLD = commonTag("ores/gold");
        public static final TagKey<Item> ORES_IRON = commonTag("ores/iron");
        public static final TagKey<Item> ORES_LAPIS = commonTag("ores/lapis");
        public static final TagKey<Item> ORES_NETHERITE_SCRAP = commonTag("ores/netherite_scrap");
        public static final TagKey<Item> ORES_QUARTZ = commonTag("ores/quartz");
        public static final TagKey<Item> ORES_REDSTONE = commonTag("ores/redstone");
        public static final TagKey<Item> RAW_MATERIALS = commonTag("raw_materials");
        public static final TagKey<Item> RAW_MATERIALS_COPPER = commonTag("raw_materials/copper");
        public static final TagKey<Item> RAW_MATERIALS_GOLD = commonTag("raw_materials/gold");
        public static final TagKey<Item> RAW_MATERIALS_IRON = commonTag("raw_materials/iron");
        public static final TagKey<Item> STORAGE_BLOCKS = commonTag("storage_blocks");
        public static final TagKey<Item> STORAGE_BLOCKS_AMETHYST = commonTag("storage_blocks/amethyst");
        public static final TagKey<Item> STORAGE_BLOCKS_COAL = commonTag("storage_blocks/coal");
        public static final TagKey<Item> STORAGE_BLOCKS_COPPER = commonTag("storage_blocks/copper");
        public static final TagKey<Item> STORAGE_BLOCKS_DIAMOND = commonTag("storage_blocks/diamond");
        public static final TagKey<Item> STORAGE_BLOCKS_EMERALD = commonTag("storage_blocks/emerald");
        public static final TagKey<Item> STORAGE_BLOCKS_GOLD = commonTag("storage_blocks/gold");
        public static final TagKey<Item> STORAGE_BLOCKS_IRON = commonTag("storage_blocks/iron");
        public static final TagKey<Item> STORAGE_BLOCKS_LAPIS = commonTag("storage_blocks/lapis");
        public static final TagKey<Item> STORAGE_BLOCKS_NETHERITE = commonTag("storage_blocks/netherite");
        public static final TagKey<Item> STORAGE_BLOCKS_QUARTZ = commonTag("storage_blocks/quartz");
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_COPPER = commonTag("storage_blocks/raw_copper");
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_GOLD = commonTag("storage_blocks/raw_gold");
        public static final TagKey<Item> STORAGE_BLOCKS_RAW_IRON = commonTag("storage_blocks/raw_iron");
        public static final TagKey<Item> STORAGE_BLOCKS_REDSTONE = commonTag("storage_blocks/redstone");
        public static final TagKey<Item> NUGGETS = commonTag("nuggets");
        public static final TagKey<Item> NUGGETS_GOLD = commonTag("nuggets/gold");
        public static final TagKey<Item> NUGGETS_IRON = commonTag("nuggets/iron");
        public static final TagKey<Item> INGOTS = commonTag("ingots");
        public static final TagKey<Item> INGOTS_BRICK = commonTag("bricks/normal");
        public static final TagKey<Item> INGOTS_COPPER = commonTag("ingots/copper");
        public static final TagKey<Item> INGOTS_GOLD = commonTag("ingots/gold");
        public static final TagKey<Item> INGOTS_IRON = commonTag("ingots/iron");
        public static final TagKey<Item> INGOTS_NETHERITE = commonTag("ingots/netherite");
        public static final TagKey<Item> INGOTS_NETHER_BRICK = commonTag("bricks/nether");
        public static final TagKey<Item> INGOTS_ALUMINUM = commonTag("ingots/aluminum");
        public static final TagKey<Item> INGOTS_BRONZE = commonTag("ingots/bronze");
        public static final TagKey<Item> INGOTS_ELECTRUM = commonTag("ingots/electrum");
        public static final TagKey<Item> INGOTS_INVAR = commonTag("ingots/invar");
        public static final TagKey<Item> INGOTS_LEAD = commonTag("ingots/lead");
        public static final TagKey<Item> INGOTS_NICKEL = commonTag("ingots/nickel");
        public static final TagKey<Item> INGOTS_PLATINUM = commonTag("ingots/platinum");
        public static final TagKey<Item> INGOTS_SILVER = commonTag("ingots/silver");
        public static final TagKey<Item> INGOTS_STEEL = commonTag("ingots/steel");
        public static final TagKey<Item> INGOTS_TIN = commonTag("ingots/tin");
        public static final TagKey<Item> LEATHER = commonTag("leathers");
        public static final TagKey<Item> NETHER_STARS = commonTag("nether_stars");
        public static final TagKey<Item> NETHERRACK = commonTag("netherracks");
        public static final TagKey<Item> RODS = commonTag("rods");
        public static final TagKey<Item> RODS_BLAZE = commonTag("rods/blaze");
        public static final TagKey<Item> RODS_WOODEN = commonTag("rods/wooden");
        public static final TagKey<Item> SHEARS = commonTag("tools/shear");
        public static final TagKey<Item> SLIMEBALLS = commonTag("slime_balls");
        public static final TagKey<Item> OBSIDIAN = commonTag("obsidians");
        public static final TagKey<Item> GRAVEL = commonTag("gravels");
        public static final TagKey<Item> GUNPOWDER = commonTag("gunpowders");
        public static final TagKey<Item> END_STONES = commonTag("end_stones");
        public static final TagKey<Item> ENDER_PEARLS = commonTag("ender_pearls");
        public static final TagKey<Item> FEATHERS = commonTag("feathers");
        public static final TagKey<Item> FENCE_GATES = commonTag("fence_gates");
        public static final TagKey<Item> FENCE_GATES_WOODEN = commonTag("fence_gates/wooden");
        public static final TagKey<Item> FENCES = commonTag("fences");
        public static final TagKey<Item> FENCES_NETHER_BRICK = commonTag("fences/nether_brick");
        public static final TagKey<Item> FENCES_WOODEN = commonTag("fences/wooden");
        public static final TagKey<Item> GEMS = commonTag("gems");
        public static final TagKey<Item> GEMS_DIAMOND = commonTag("gems/diamond");
        public static final TagKey<Item> GEMS_EMERALD = commonTag("gems/emerald");
        public static final TagKey<Item> GEMS_AMETHYST = commonTag("gems/amethyst");
        public static final TagKey<Item> GEMS_LAPIS = commonTag("gems/lapis");
        public static final TagKey<Item> GEMS_PRISMARINE = commonTag("gems/prismarine");
        public static final TagKey<Item> GEMS_QUARTZ = commonTag("gems/quartz");
        public static final TagKey<Item> GEMS_PERIDOT = commonTag("gems/peridot");
        public static final TagKey<Item> GEMS_RUBY = commonTag("gems/ruby");
        public static final TagKey<Item> GEMS_SAPPHIRE = commonTag("gems/sapphire");
        public static final TagKey<Item> GEMS_TOPAZ = commonTag("gems/topaz");
        public static final TagKey<Item> EGGS = commonTag("eggs");
        public static final TagKey<Item> DUSTS = commonTag("dusts");
        public static final TagKey<Item> DUSTS_PRISMARINE = commonTag("dusts/prismarine");
        public static final TagKey<Item> DUSTS_REDSTONE = commonTag("dusts/redstone");
        public static final TagKey<Item> DUSTS_GLOWSTONE = commonTag("dusts/glowstone");
        public static final TagKey<Item> BARRELS = commonTag("barrels");
        public static final TagKey<Item> BARRELS_WOODEN = commonTag("barrels/wooden");
        public static final TagKey<Item> BONES = commonTag("bones");
        public static final TagKey<Item> BOOKSHELVES = commonTag("bookshelves");
        public static final TagKey<Item> CHESTS = commonTag("chests");
        public static final TagKey<Item> CHESTS_ENDER = commonTag("chests/ender");
        public static final TagKey<Item> CHESTS_TRAPPED = commonTag("chests/trapped");
        public static final TagKey<Item> CHESTS_WOODEN = commonTag("chests/wooden");
        // Filled by the mod that adds locks and keys, so containers never need to know its items
        public static final TagKey<Item> LOCKS = commonTag("locks");
        public static final TagKey<Item> KEYS = commonTag("keys");
        public static final TagKey<Item> COBBLESTONE = commonTag("cobblestones");
        public static final TagKey<Item> GLASS = commonTag("glass_blocks");
        public static final TagKey<Item> GLASS_BLACK = commonTag("glass_blocks/black");
        public static final TagKey<Item> GLASS_BLUE = commonTag("glass_blocks/blue");
        public static final TagKey<Item> GLASS_BROWN = commonTag("glass_blocks/brown");
        public static final TagKey<Item> GLASS_COLORLESS = commonTag("glass_blocks/colorless");
        public static final TagKey<Item> GLASS_CYAN = commonTag("glass_blocks/cyan");
        public static final TagKey<Item> GLASS_GRAY = commonTag("glass_blocks/gray");
        public static final TagKey<Item> GLASS_GREEN = commonTag("glass_blocks/green");
        public static final TagKey<Item> GLASS_LIGHT_BLUE = commonTag("glass_blocks/light_blue");
        public static final TagKey<Item> GLASS_LIGHT_GRAY = commonTag("glass_blocks/light_gray");
        public static final TagKey<Item> GLASS_LIME = commonTag("glass_blocks/lime");
        public static final TagKey<Item> GLASS_MAGENTA = commonTag("glass_blocks/magenta");
        public static final TagKey<Item> GLASS_ORANGE = commonTag("glass_blocks/orange");
        public static final TagKey<Item> GLASS_PINK = commonTag("glass_blocks/pink");
        public static final TagKey<Item> GLASS_PURPLE = commonTag("glass_blocks/purple");
        public static final TagKey<Item> GLASS_RED = commonTag("glass_blocks/red");
        public static final TagKey<Item> GLASS_SILICA = commonTag("glass_blocks/cheap");
        public static final TagKey<Item> GLASS_TINTED = commonTag("glass_blocks/tinted");
        public static final TagKey<Item> GLASS_WHITE = commonTag("glass_blocks/white");
        public static final TagKey<Item> GLASS_YELLOW = commonTag("glass_blocks/yellow");
        public static final TagKey<Item> GLASS_PANES = commonTag("glass_panes");
        public static final TagKey<Item> GLASS_PANES_BLACK = commonTag("glass_panes/black");
        public static final TagKey<Item> GLASS_PANES_BLUE = commonTag("glass_panes/blue");
        public static final TagKey<Item> GLASS_PANES_BROWN = commonTag("glass_panes/brown");
        public static final TagKey<Item> GLASS_PANES_COLORLESS = commonTag("glass_panes/colorless");
        public static final TagKey<Item> GLASS_PANES_CYAN = commonTag("glass_panes/cyan");
        public static final TagKey<Item> GLASS_PANES_GRAY = commonTag("glass_panes/gray");
        public static final TagKey<Item> GLASS_PANES_GREEN = commonTag("glass_panes/green");
        public static final TagKey<Item> GLASS_PANES_LIGHT_BLUE = commonTag("glass_panes/light_blue");
        public static final TagKey<Item> GLASS_PANES_LIGHT_GRAY = commonTag("glass_panes/light_gray");
        public static final TagKey<Item> GLASS_PANES_LIME = commonTag("glass_panes/lime");
        public static final TagKey<Item> GLASS_PANES_MAGENTA = commonTag("glass_panes/magenta");
        public static final TagKey<Item> GLASS_PANES_ORANGE = commonTag("glass_panes/orange");
        public static final TagKey<Item> GLASS_PANES_PINK = commonTag("glass_panes/pink");
        public static final TagKey<Item> GLASS_PANES_PURPLE = commonTag("glass_panes/purple");
        public static final TagKey<Item> GLASS_PANES_RED = commonTag("glass_panes/red");
        public static final TagKey<Item> GLASS_PANES_WHITE = commonTag("glass_panes/white");
        public static final TagKey<Item> GLASS_PANES_YELLOW = commonTag("glass_panes/yellow");
        public static final TagKey<Item> CROPS = commonTag("crops");
        public static final TagKey<Item> CROPS_BEETROOT = commonTag("crops/beetroot");
        public static final TagKey<Item> CROPS_CARROT = commonTag("crops/carrot");
        public static final TagKey<Item> CROPS_NETHER_WART = commonTag("crops/nether_wart");
        public static final TagKey<Item> CROPS_POTATO = commonTag("crops/potato");
        public static final TagKey<Item> CROPS_WHEAT = commonTag("crops/wheat");

        /// ====================================================================
        /// Below are tags that need to be generated on both Fabric and Forge
        /// ====================================================================
        public static final TagKey<Item> CONCRETE = commonTag("concretes");
        public static final TagKey<Item> CONCRETE_POWDER = commonTag("concrete_powders");
        public static final TagKey<Item> FLUID_CONTAINERS = commonTag("containers/fluid");
        public static final TagKey<Item> BUCKETS_MILK = commonTag("buckets/milk");

        private static TagKey<Item> commonTag(String name) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(COMMON_NAMESPACE, name));
        }
    }

    public static class Biomes {
        public static final TagKey<Biome> IS_HOT = commonTag("is_hot");
        public static final TagKey<Biome> IS_HOT_OVERWORLD = commonTag("is_hot/overworld");
        public static final TagKey<Biome> IS_HOT_NETHER = commonTag("is_hot/nether");
        public static final TagKey<Biome> IS_HOT_END = commonTag("is_hot/end");

        public static final TagKey<Biome> IS_COLD = commonTag("is_cold");
        public static final TagKey<Biome> IS_COLD_OVERWORLD = commonTag("is_cold/overworld");
        public static final TagKey<Biome> IS_COLD_NETHER = commonTag("is_cold/nether");
        public static final TagKey<Biome> IS_COLD_END = commonTag("is_cold/end");

        public static final TagKey<Biome> IS_SPARSE = commonTag("is_sparse_vegetation");
        public static final TagKey<Biome> IS_SPARSE_OVERWORLD = commonTag("is_sparse_vegetation/overworld");
        public static final TagKey<Biome> IS_SPARSE_NETHER = commonTag("is_sparse_vegetation/nether");
        public static final TagKey<Biome> IS_SPARSE_END = commonTag("is_sparse_vegetation/end");
        public static final TagKey<Biome> IS_DENSE = commonTag("is_dense_vegetation");
        public static final TagKey<Biome> IS_DENSE_OVERWORLD = commonTag("is_dense_vegetation/overworld");
        public static final TagKey<Biome> IS_DENSE_NETHER = commonTag("is_dense_vegetation/nether");
        public static final TagKey<Biome> IS_DENSE_END = commonTag("is_dense_vegetation/end");

        public static final TagKey<Biome> IS_WET = commonTag("is_wet");
        public static final TagKey<Biome> IS_WET_OVERWORLD = commonTag("is_wet/overworld");
        public static final TagKey<Biome> IS_WET_NETHER = commonTag("is_wet/nether");
        public static final TagKey<Biome> IS_WET_END = commonTag("is_wet/end");
        public static final TagKey<Biome> IS_DRY = commonTag("is_dry");
        public static final TagKey<Biome> IS_DRY_OVERWORLD = commonTag("is_dry/overworld");
        public static final TagKey<Biome> IS_DRY_NETHER = commonTag("is_dry/nether");
        public static final TagKey<Biome> IS_DRY_END = commonTag("is_dry/end");

        public static final TagKey<Biome> IS_CONIFEROUS = commonTag("is_tree/coniferous");

        public static final TagKey<Biome> IS_SPOOKY = commonTag("is_spooky");
        public static final TagKey<Biome> IS_DEAD = commonTag("is_dead");
        public static final TagKey<Biome> IS_LUSH = commonTag("is_lush");
        public static final TagKey<Biome> IS_MUSHROOM = commonTag("is_mushroom");
        public static final TagKey<Biome> IS_MAGICAL = commonTag("is_magical");
        public static final TagKey<Biome> IS_RARE = commonTag("is_rare");
        public static final TagKey<Biome> IS_PLATEAU = commonTag("is_plateau");

        public static final TagKey<Biome> IS_WATER = commonTag("is_aquatic");
        public static final TagKey<Biome> IS_DESERT = commonTag("is_desert");
        public static final TagKey<Biome> IS_PLAINS = commonTag("is_plains");
        public static final TagKey<Biome> IS_SWAMP = commonTag("is_swamp");
        public static final TagKey<Biome> IS_SANDY = commonTag("is_sandy");
        public static final TagKey<Biome> IS_SNOWY = commonTag("is_snowy");
        public static final TagKey<Biome> IS_WASTELAND = commonTag("is_wasteland");
        public static final TagKey<Biome> IS_VOID = commonTag("is_void");
        public static final TagKey<Biome> IS_UNDERGROUND = commonTag("is_underground");

        public static final TagKey<Biome> IS_CAVE = commonTag("is_cave");
        public static final TagKey<Biome> IS_PEAK = commonTag("is_mountain/peak");
        public static final TagKey<Biome> IS_SLOPE = commonTag("is_mountain/slope");
        public static final TagKey<Biome> IS_MOUNTAIN = commonTag("is_mountain");

        private static TagKey<Biome> commonTag(String name) {
            return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(COMMON_NAMESPACE, name));
        }
    }
}
