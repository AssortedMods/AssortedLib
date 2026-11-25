package com.grim3212.assorted.lib.data;

import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.util.DyeHelper;
import com.grim3212.assorted.lib.util.LibCommonTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class LibCommonTagProvider {

    private static ResourceKey<Block> key(Block block) {
        return BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
    }

    private static ResourceKey<Item> key(Item item) {
        return BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow();
    }

    public static class BlockTagProvider extends LibBlockTagProvider {

        public BlockTagProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookup) {
            super(packOutput, lookup);
        }

        @Override
        public void addCommonTags(Function<TagKey<Block>, TagAppender<Block>> tagger) {
            tagger.apply(LibCommonTags.Blocks.BARRELS).addTag(LibCommonTags.Blocks.BARRELS_WOODEN);
            tagger.apply(LibCommonTags.Blocks.BARRELS_WOODEN).add(key(Blocks.BARREL));
            tagger.apply(LibCommonTags.Blocks.BOOKSHELVES).add(key(Blocks.BOOKSHELF));
            tagger.apply(LibCommonTags.Blocks.CHESTS).addTag(LibCommonTags.Blocks.CHESTS_ENDER);
            tagger.apply(LibCommonTags.Blocks.CHESTS).addTag(LibCommonTags.Blocks.CHESTS_TRAPPED);
            tagger.apply(LibCommonTags.Blocks.CHESTS).addTag(LibCommonTags.Blocks.CHESTS_WOODEN);
            tagger.apply(LibCommonTags.Blocks.CHESTS_ENDER).add(key(Blocks.ENDER_CHEST));
            tagger.apply(LibCommonTags.Blocks.CHESTS_TRAPPED).add(key(Blocks.TRAPPED_CHEST));
            tagger.apply(LibCommonTags.Blocks.CHESTS_WOODEN).add(key(Blocks.CHEST), key(Blocks.TRAPPED_CHEST));
            tagger.apply(LibCommonTags.Blocks.COBBLESTONE).add(key(Blocks.COBBLESTONE), key(Blocks.INFESTED_COBBLESTONE), key(Blocks.MOSSY_COBBLESTONE), key(Blocks.COBBLED_DEEPSLATE));
            tagger.apply(LibCommonTags.Blocks.END_STONES).add(key(Blocks.END_STONE));
            tagger.apply(LibCommonTags.Blocks.FENCE_GATES).addTag(LibCommonTags.Blocks.FENCE_GATES_WOODEN);
            // #minecraft:fence_gates is exactly the wooden fence gates - there is no other kind -
            // so this follows vanilla rather than listing them, which had already fallen three
            // woods behind (cherry, pale oak, bamboo).
            tagger.apply(LibCommonTags.Blocks.FENCE_GATES_WOODEN).addOptionalTag(BlockTags.FENCE_GATES.location());
            tagger.apply(LibCommonTags.Blocks.FENCES).addTag(LibCommonTags.Blocks.FENCES_WOODEN);
            tagger.apply(LibCommonTags.Blocks.FENCES).addTag(LibCommonTags.Blocks.FENCES_NETHER_BRICK);
            tagger.apply(LibCommonTags.Blocks.FENCES_NETHER_BRICK).add(key(Blocks.NETHER_BRICK_FENCE));
            tagger.apply(LibCommonTags.Blocks.FENCES_WOODEN).addOptionalTag(BlockTags.WOODEN_FENCES.location());
            tagger.apply(LibCommonTags.Blocks.GRAVEL).add(key(Blocks.GRAVEL));
            tagger.apply(LibCommonTags.Blocks.NETHERRACK).add(key(Blocks.NETHERRACK));
            tagger.apply(LibCommonTags.Blocks.OBSIDIAN).add(key(Blocks.OBSIDIAN));

            List<TagKey<Block>> oreTags = Arrays.asList(LibCommonTags.Blocks.ORES_COAL, LibCommonTags.Blocks.ORES_COPPER, LibCommonTags.Blocks.ORES_DIAMOND, LibCommonTags.Blocks.ORES_EMERALD, LibCommonTags.Blocks.ORES_GOLD, LibCommonTags.Blocks.ORES_IRON, LibCommonTags.Blocks.ORES_LAPIS, LibCommonTags.Blocks.ORES_REDSTONE, LibCommonTags.Blocks.ORES_QUARTZ, LibCommonTags.Blocks.ORES_NETHERITE_SCRAP);
            for (TagKey<Block> oreTag : oreTags)
                tagger.apply(LibCommonTags.Blocks.ORES).addTag(oreTag);

            tagger.apply(LibCommonTags.Blocks.ORES_COAL).addOptionalTag(BlockTags.COAL_ORES.location());
            tagger.apply(LibCommonTags.Blocks.ORES_COPPER).addOptionalTag(BlockTags.COPPER_ORES.location());
            tagger.apply(LibCommonTags.Blocks.ORES_DIAMOND).addOptionalTag(BlockTags.DIAMOND_ORES.location());
            tagger.apply(LibCommonTags.Blocks.ORES_EMERALD).addOptionalTag(BlockTags.EMERALD_ORES.location());
            tagger.apply(LibCommonTags.Blocks.ORES_GOLD).addOptionalTag(BlockTags.GOLD_ORES.location());
            tagger.apply(LibCommonTags.Blocks.ORES_IRON).addOptionalTag(BlockTags.IRON_ORES.location());
            tagger.apply(LibCommonTags.Blocks.ORES_LAPIS).addOptionalTag(BlockTags.LAPIS_ORES.location());
            tagger.apply(LibCommonTags.Blocks.ORES_QUARTZ).add(key(Blocks.NETHER_QUARTZ_ORE));
            tagger.apply(LibCommonTags.Blocks.ORES_REDSTONE).addOptionalTag(BlockTags.REDSTONE_ORES.location());
            tagger.apply(LibCommonTags.Blocks.ORES_NETHERITE_SCRAP).add(key(Blocks.ANCIENT_DEBRIS));
            tagger.apply(LibCommonTags.Blocks.STONE).add(key(Blocks.ANDESITE), key(Blocks.DIORITE), key(Blocks.GRANITE), key(Blocks.INFESTED_STONE), key(Blocks.STONE), key(Blocks.POLISHED_ANDESITE), key(Blocks.POLISHED_DIORITE), key(Blocks.POLISHED_GRANITE), key(Blocks.DEEPSLATE), key(Blocks.POLISHED_DEEPSLATE), key(Blocks.INFESTED_DEEPSLATE), key(Blocks.TUFF));

            List<TagKey<Block>> storageTags = Arrays.asList(LibCommonTags.Blocks.STORAGE_BLOCKS_AMETHYST, LibCommonTags.Blocks.STORAGE_BLOCKS_COAL, LibCommonTags.Blocks.STORAGE_BLOCKS_COPPER, LibCommonTags.Blocks.STORAGE_BLOCKS_DIAMOND, LibCommonTags.Blocks.STORAGE_BLOCKS_EMERALD, LibCommonTags.Blocks.STORAGE_BLOCKS_GOLD, LibCommonTags.Blocks.STORAGE_BLOCKS_IRON, LibCommonTags.Blocks.STORAGE_BLOCKS_LAPIS, LibCommonTags.Blocks.STORAGE_BLOCKS_QUARTZ, LibCommonTags.Blocks.STORAGE_BLOCKS_RAW_COPPER, LibCommonTags.Blocks.STORAGE_BLOCKS_RAW_GOLD, LibCommonTags.Blocks.STORAGE_BLOCKS_RAW_IRON, LibCommonTags.Blocks.STORAGE_BLOCKS_REDSTONE, LibCommonTags.Blocks.STORAGE_BLOCKS_NETHERITE);
            for (TagKey<Block> storageTag : storageTags)
                tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS).addTag(storageTag);

            tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS_AMETHYST).add(key(Blocks.AMETHYST_BLOCK));
            tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS_COAL).add(key(Blocks.COAL_BLOCK));
            tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS_COPPER).add(key(Blocks.COPPER_BLOCK));
            tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS_DIAMOND).add(key(Blocks.DIAMOND_BLOCK));
            tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS_EMERALD).add(key(Blocks.EMERALD_BLOCK));
            tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS_GOLD).add(key(Blocks.GOLD_BLOCK));
            tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS_IRON).add(key(Blocks.IRON_BLOCK));
            tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS_LAPIS).add(key(Blocks.LAPIS_BLOCK));
            tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS_QUARTZ).add(key(Blocks.QUARTZ_BLOCK));
            tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS_RAW_COPPER).add(key(Blocks.RAW_COPPER_BLOCK));
            tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS_RAW_GOLD).add(key(Blocks.RAW_GOLD_BLOCK));
            tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS_RAW_IRON).add(key(Blocks.RAW_IRON_BLOCK));
            tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS_REDSTONE).add(key(Blocks.REDSTONE_BLOCK));
            tagger.apply(LibCommonTags.Blocks.STORAGE_BLOCKS_NETHERITE).add(key(Blocks.NETHERITE_BLOCK));

            addColored(tagger.apply(LibCommonTags.Blocks.STAINED_GLASS)::add, "GLASS", "{color}_stained_glass", tagger);
            addColored(tagger.apply(LibCommonTags.Blocks.STAINED_GLASS_PANES)::add, "GLASS_PANES", "{color}_stained_glass_pane", tagger);

            tagger.apply(LibCommonTags.Blocks.GLASS).addTag(LibCommonTags.Blocks.GLASS_COLORLESS).addOptionalTag(LibCommonTags.Blocks.STAINED_GLASS.location()).addTag(LibCommonTags.Blocks.GLASS_TINTED);
            tagger.apply(LibCommonTags.Blocks.GLASS_COLORLESS).add(key(Blocks.GLASS));
            tagger.apply(LibCommonTags.Blocks.GLASS_TINTED).add(key(Blocks.TINTED_GLASS));
            tagger.apply(LibCommonTags.Blocks.GLASS_PANES).addTag(LibCommonTags.Blocks.GLASS_PANES_COLORLESS).addOptionalTag(LibCommonTags.Blocks.STAINED_GLASS_PANES.location());
            tagger.apply(LibCommonTags.Blocks.GLASS_PANES_COLORLESS).add(key(Blocks.GLASS_PANE));

            DyeHelper.CONCRETE_BY_DYE.entrySet().stream().forEach((x) -> tagger.apply(LibCommonTags.Blocks.CONCRETE).add(key(x.getValue())));
            DyeHelper.CONCRETE_POWDER_BY_DYE.entrySet().stream().forEach((x) -> tagger.apply(LibCommonTags.Blocks.CONCRETE_POWDER).add(key(x.getValue())));
        }

        /**
         * @param constantPrefix the {@link LibCommonTags.Blocks} constant the per-colour tags are
         *                       named after; the tag path ({@code c:glass_blocks}) does not always
         *                       match it ({@code GLASS}).
         */
        private void addColored(Consumer<ResourceKey<Block>> consumer, String constantPrefix, String pattern, Function<TagKey<Block>, TagAppender<Block>> tagger) {
            String prefix = constantPrefix + '_';
            for (DyeColor color : DyeColor.values()) {
                ResourceLocation key = ResourceLocation.fromNamespaceAndPath("minecraft", pattern.replace("{color}", color.getName()));
                TagKey<Block> tag = getCommonTag(prefix + color.getName());
                Block block = Services.PLATFORM.getRegistry(Registries.BLOCK).getValue(key).get();
                if (block == null || block == Blocks.AIR)
                    throw new IllegalStateException("Unknown block: " + key.toString());
                tagger.apply(tag).add(key(block));
                consumer.accept(key(block));
            }
        }

        private TagKey<Block> getCommonTag(String name) {
            try {
                name = name.toUpperCase(Locale.ENGLISH);
                return (TagKey<Block>) LibCommonTags.Blocks.class.getDeclaredField(name).get(null);
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                throw new IllegalStateException(LibCommonTags.Blocks.class.getName() + " is missing tag name: " + name);
            }
        }
    }

    public static class ItemTagProvider extends LibItemTagProvider {

        public ItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, CompletableFuture<TagLookup<Block>> blockTags) {
            super(output, lookup, blockTags);
        }

        @Override
        public void addCommonTags(Function<TagKey<Item>, TagAppender<Item>> tagger, BiConsumer<TagKey<Block>, TagKey<Item>> copier) {
            copier.accept(LibCommonTags.Blocks.STONE, LibCommonTags.Items.STONE);

            for (DyeColor color : DyeColor.values()) {
                TagKey<Item> dyeTag = DyeHelper.getDyeTag(color);
                tagger.apply(LibCommonTags.Items.DYES).addTag(dyeTag);
                tagger.apply(dyeTag).add(key(DyeItem.byColor(color)));
            }

            copier.accept(LibCommonTags.Blocks.BARRELS, LibCommonTags.Items.BARRELS);
            copier.accept(LibCommonTags.Blocks.BARRELS_WOODEN, LibCommonTags.Items.BARRELS_WOODEN);
            tagger.apply(LibCommonTags.Items.BONES).add(key(Items.BONE));
            copier.accept(LibCommonTags.Blocks.BOOKSHELVES, LibCommonTags.Items.BOOKSHELVES);
            copier.accept(LibCommonTags.Blocks.CHESTS, LibCommonTags.Items.CHESTS);
            copier.accept(LibCommonTags.Blocks.CHESTS_ENDER, LibCommonTags.Items.CHESTS_ENDER);
            copier.accept(LibCommonTags.Blocks.CHESTS_TRAPPED, LibCommonTags.Items.CHESTS_TRAPPED);
            copier.accept(LibCommonTags.Blocks.CHESTS_WOODEN, LibCommonTags.Items.CHESTS_WOODEN);
            copier.accept(LibCommonTags.Blocks.COBBLESTONE, LibCommonTags.Items.COBBLESTONE);
            tagger.apply(LibCommonTags.Items.DUSTS).addTag(LibCommonTags.Items.DUSTS_GLOWSTONE);
            tagger.apply(LibCommonTags.Items.DUSTS).addTag(LibCommonTags.Items.DUSTS_PRISMARINE);
            tagger.apply(LibCommonTags.Items.DUSTS).addTag(LibCommonTags.Items.DUSTS_REDSTONE);
            tagger.apply(LibCommonTags.Items.DUSTS_GLOWSTONE).add(key(Items.GLOWSTONE_DUST));
            tagger.apply(LibCommonTags.Items.DUSTS_PRISMARINE).add(key(Items.PRISMARINE_SHARD));
            tagger.apply(LibCommonTags.Items.DUSTS_REDSTONE).add(key(Items.REDSTONE));
            tagger.apply(LibCommonTags.Items.EGGS).add(key(Items.EGG));
            copier.accept(LibCommonTags.Blocks.END_STONES, LibCommonTags.Items.END_STONES);
            tagger.apply(LibCommonTags.Items.ENDER_PEARLS).add(key(Items.ENDER_PEARL));
            tagger.apply(LibCommonTags.Items.FEATHERS).add(key(Items.FEATHER));
            copier.accept(LibCommonTags.Blocks.FENCE_GATES, LibCommonTags.Items.FENCE_GATES);
            copier.accept(LibCommonTags.Blocks.FENCE_GATES_WOODEN, LibCommonTags.Items.FENCE_GATES_WOODEN);
            copier.accept(LibCommonTags.Blocks.FENCES, LibCommonTags.Items.FENCES);
            copier.accept(LibCommonTags.Blocks.FENCES_NETHER_BRICK, LibCommonTags.Items.FENCES_NETHER_BRICK);
            copier.accept(LibCommonTags.Blocks.FENCES_WOODEN, LibCommonTags.Items.FENCES_WOODEN);

            List<TagKey<Item>> gemTags = Arrays.asList(LibCommonTags.Items.GEMS_AMETHYST, LibCommonTags.Items.GEMS_DIAMOND, LibCommonTags.Items.GEMS_EMERALD, LibCommonTags.Items.GEMS_LAPIS, LibCommonTags.Items.GEMS_PRISMARINE, LibCommonTags.Items.GEMS_QUARTZ);
            for (TagKey<Item> gemTag : gemTags)
                tagger.apply(LibCommonTags.Items.GEMS).addTag(gemTag);

            tagger.apply(LibCommonTags.Items.GEMS_AMETHYST).add(key(Items.AMETHYST_SHARD));
            tagger.apply(LibCommonTags.Items.GEMS_DIAMOND).add(key(Items.DIAMOND));
            tagger.apply(LibCommonTags.Items.GEMS_EMERALD).add(key(Items.EMERALD));
            tagger.apply(LibCommonTags.Items.GEMS_LAPIS).add(key(Items.LAPIS_LAZULI));
            tagger.apply(LibCommonTags.Items.GEMS_PRISMARINE).add(key(Items.PRISMARINE_CRYSTALS));
            tagger.apply(LibCommonTags.Items.GEMS_QUARTZ).add(key(Items.QUARTZ));
            copier.accept(LibCommonTags.Blocks.GRAVEL, LibCommonTags.Items.GRAVEL);
            tagger.apply(LibCommonTags.Items.GUNPOWDER).add(key(Items.GUNPOWDER));

            List<TagKey<Item>> ingotTags = Arrays.asList(LibCommonTags.Items.INGOTS_BRICK, LibCommonTags.Items.INGOTS_COPPER, LibCommonTags.Items.INGOTS_GOLD, LibCommonTags.Items.INGOTS_IRON, LibCommonTags.Items.INGOTS_NETHERITE, LibCommonTags.Items.INGOTS_NETHER_BRICK);
            for (TagKey<Item> ingotTag : ingotTags)
                tagger.apply(LibCommonTags.Items.INGOTS).addTag(ingotTag);

            tagger.apply(LibCommonTags.Items.INGOTS_BRICK).add(key(Items.BRICK));
            tagger.apply(LibCommonTags.Items.INGOTS_COPPER).add(key(Items.COPPER_INGOT));
            tagger.apply(LibCommonTags.Items.INGOTS_GOLD).add(key(Items.GOLD_INGOT));
            tagger.apply(LibCommonTags.Items.INGOTS_IRON).add(key(Items.IRON_INGOT));
            tagger.apply(LibCommonTags.Items.INGOTS_NETHERITE).add(key(Items.NETHERITE_INGOT));
            tagger.apply(LibCommonTags.Items.INGOTS_NETHER_BRICK).add(key(Items.NETHER_BRICK));
            tagger.apply(LibCommonTags.Items.LEATHER).add(key(Items.LEATHER));
            tagger.apply(LibCommonTags.Items.NETHER_STARS).add(key(Items.NETHER_STAR));
            copier.accept(LibCommonTags.Blocks.NETHERRACK, LibCommonTags.Items.NETHERRACK);
            tagger.apply(LibCommonTags.Items.NUGGETS).addTag(LibCommonTags.Items.NUGGETS_GOLD);
            tagger.apply(LibCommonTags.Items.NUGGETS).addTag(LibCommonTags.Items.NUGGETS_IRON);
            tagger.apply(LibCommonTags.Items.NUGGETS_IRON).add(key(Items.IRON_NUGGET));
            tagger.apply(LibCommonTags.Items.NUGGETS_GOLD).add(key(Items.GOLD_NUGGET));
            copier.accept(LibCommonTags.Blocks.OBSIDIAN, LibCommonTags.Items.OBSIDIAN);
            copier.accept(LibCommonTags.Blocks.ORES, LibCommonTags.Items.ORES);
            copier.accept(LibCommonTags.Blocks.ORES_COAL, LibCommonTags.Items.ORES_COAL);
            copier.accept(LibCommonTags.Blocks.ORES_COPPER, LibCommonTags.Items.ORES_COPPER);
            copier.accept(LibCommonTags.Blocks.ORES_DIAMOND, LibCommonTags.Items.ORES_DIAMOND);
            copier.accept(LibCommonTags.Blocks.ORES_EMERALD, LibCommonTags.Items.ORES_EMERALD);
            copier.accept(LibCommonTags.Blocks.ORES_GOLD, LibCommonTags.Items.ORES_GOLD);
            copier.accept(LibCommonTags.Blocks.ORES_IRON, LibCommonTags.Items.ORES_IRON);
            copier.accept(LibCommonTags.Blocks.ORES_LAPIS, LibCommonTags.Items.ORES_LAPIS);
            copier.accept(LibCommonTags.Blocks.ORES_QUARTZ, LibCommonTags.Items.ORES_QUARTZ);
            copier.accept(LibCommonTags.Blocks.ORES_REDSTONE, LibCommonTags.Items.ORES_REDSTONE);
            copier.accept(LibCommonTags.Blocks.ORES_NETHERITE_SCRAP, LibCommonTags.Items.ORES_NETHERITE_SCRAP);
            tagger.apply(LibCommonTags.Items.RAW_MATERIALS).addTag(LibCommonTags.Items.RAW_MATERIALS_COPPER);
            tagger.apply(LibCommonTags.Items.RAW_MATERIALS).addTag(LibCommonTags.Items.RAW_MATERIALS_GOLD);
            tagger.apply(LibCommonTags.Items.RAW_MATERIALS).addTag(LibCommonTags.Items.RAW_MATERIALS_IRON);
            tagger.apply(LibCommonTags.Items.RAW_MATERIALS_COPPER).add(key(Items.RAW_COPPER));
            tagger.apply(LibCommonTags.Items.RAW_MATERIALS_GOLD).add(key(Items.RAW_GOLD));
            tagger.apply(LibCommonTags.Items.RAW_MATERIALS_IRON).add(key(Items.RAW_IRON));
            tagger.apply(LibCommonTags.Items.RODS).addTag(LibCommonTags.Items.RODS_WOODEN);
            tagger.apply(LibCommonTags.Items.RODS).addTag(LibCommonTags.Items.RODS_BLAZE);
            tagger.apply(LibCommonTags.Items.RODS_BLAZE).add(key(Items.BLAZE_ROD));
            tagger.apply(LibCommonTags.Items.RODS_WOODEN).add(key(Items.STICK));
            tagger.apply(LibCommonTags.Items.SLIMEBALLS).add(key(Items.SLIME_BALL));
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS, LibCommonTags.Items.STORAGE_BLOCKS);
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS_AMETHYST, LibCommonTags.Items.STORAGE_BLOCKS_AMETHYST);
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS_COAL, LibCommonTags.Items.STORAGE_BLOCKS_COAL);
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS_COPPER, LibCommonTags.Items.STORAGE_BLOCKS_COPPER);
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS_DIAMOND, LibCommonTags.Items.STORAGE_BLOCKS_DIAMOND);
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS_EMERALD, LibCommonTags.Items.STORAGE_BLOCKS_EMERALD);
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS_GOLD, LibCommonTags.Items.STORAGE_BLOCKS_GOLD);
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS_IRON, LibCommonTags.Items.STORAGE_BLOCKS_IRON);
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS_LAPIS, LibCommonTags.Items.STORAGE_BLOCKS_LAPIS);
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS_QUARTZ, LibCommonTags.Items.STORAGE_BLOCKS_QUARTZ);
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS_REDSTONE, LibCommonTags.Items.STORAGE_BLOCKS_REDSTONE);
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS_RAW_COPPER, LibCommonTags.Items.STORAGE_BLOCKS_RAW_COPPER);
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS_RAW_GOLD, LibCommonTags.Items.STORAGE_BLOCKS_RAW_GOLD);
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS_RAW_IRON, LibCommonTags.Items.STORAGE_BLOCKS_RAW_IRON);
            copier.accept(LibCommonTags.Blocks.STORAGE_BLOCKS_NETHERITE, LibCommonTags.Items.STORAGE_BLOCKS_NETHERITE);
            tagger.apply(LibCommonTags.Items.STRING).add(key(Items.STRING));
            // c:tools, c:armors and every subtag below them are shipped fully populated by
            // both NeoForge and Fabric API, down to the same vanilla items this used to add
            // by hand. Re-declaring them only risked drifting from the loaders.

            copier.accept(LibCommonTags.Blocks.GLASS, LibCommonTags.Items.GLASS);
            copier.accept(LibCommonTags.Blocks.GLASS_TINTED, LibCommonTags.Items.GLASS_TINTED);
            copier.accept(LibCommonTags.Blocks.GLASS_PANES, LibCommonTags.Items.GLASS_PANES);
            copyColored("GLASS", copier);
            copyColored("GLASS_PANES", copier);

            tagger.apply(LibCommonTags.Items.CROPS).addTag(LibCommonTags.Items.CROPS_BEETROOT).addTag(LibCommonTags.Items.CROPS_CARROT).addTag(LibCommonTags.Items.CROPS_NETHER_WART).addTag(LibCommonTags.Items.CROPS_POTATO).addTag(LibCommonTags.Items.CROPS_WHEAT);
            tagger.apply(LibCommonTags.Items.CROPS_BEETROOT).add(key(Items.BEETROOT));
            tagger.apply(LibCommonTags.Items.CROPS_CARROT).add(key(Items.CARROT));
            tagger.apply(LibCommonTags.Items.CROPS_NETHER_WART).add(key(Items.NETHER_WART));
            tagger.apply(LibCommonTags.Items.CROPS_POTATO).add(key(Items.POTATO));
            tagger.apply(LibCommonTags.Items.CROPS_WHEAT).add(key(Items.WHEAT));

            copier.accept(LibCommonTags.Blocks.CONCRETE, LibCommonTags.Items.CONCRETE);
            copier.accept(LibCommonTags.Blocks.CONCRETE_POWDER, LibCommonTags.Items.CONCRETE_POWDER);
            tagger.apply(LibCommonTags.Items.FLUID_CONTAINERS).add(key(Items.BUCKET), key(Items.WATER_BUCKET), key(Items.LAVA_BUCKET));
            tagger.apply(LibCommonTags.Items.BUCKETS_MILK).add(key(Items.MILK_BUCKET));
        }

        /**
         * @param constantPrefix the constant name in {@link LibCommonTags.Blocks} / {@link LibCommonTags.Items}
         *                       the per-colour tags are named after. See
         *                       {@link BlockTagProvider#addColored}.
         */
        private void copyColored(String constantPrefix, BiConsumer<TagKey<Block>, TagKey<Item>> copier) {
            String blockPre = constantPrefix + '_';
            String itemPre = constantPrefix + '_';
            for (DyeColor color : DyeColor.values()) {
                TagKey<Block> from = getCommonBlockTag(blockPre + color.getName());
                TagKey<Item> to = getCommonItemTag(itemPre + color.getName());
                copier.accept(from, to);
            }
            copier.accept(getCommonBlockTag(blockPre + "colorless"), getCommonItemTag(itemPre + "colorless"));
        }

        private TagKey<Block> getCommonBlockTag(String name) {
            try {
                name = name.toUpperCase(Locale.ENGLISH);
                return (TagKey<Block>) LibCommonTags.Blocks.class.getDeclaredField(name).get(null);
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                throw new IllegalStateException(LibCommonTags.Blocks.class.getName() + " is missing tag name: " + name);
            }
        }

        private TagKey<Item> getCommonItemTag(String name) {
            try {
                name = name.toUpperCase(Locale.ENGLISH);
                return (TagKey<Item>) LibCommonTags.Items.class.getDeclaredField(name).get(null);
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                throw new IllegalStateException(LibCommonTags.Items.class.getName() + " is missing tag name: " + name);
            }
        }
    }

    public static class BiomeTagProvider extends LibBiomeTagProvider {

        public BiomeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider);
        }

        /**
         * NeoForge and Fabric API both ship every c: biome tag {@link LibCommonTags} names, and agree
         * on all of them but this one: Fabric API counts the pale garden as dense vegetation and
         * NeoForge does not. Adding it here, on both loaders, keeps a biome check the same whichever
         * loader a mod runs on. Anything added here goes on both loaders, never one - the
         * {@code common_biome_tags_match_on_every_loader} gametest holds each loader to one list.
         */
        public void addCommonTags(Function<TagKey<Biome>, TagAppender<Biome>> tagger) {
        }
    }
}
