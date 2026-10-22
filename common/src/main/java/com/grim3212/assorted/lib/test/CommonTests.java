package com.grim3212.assorted.lib.test;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Checks every Assorted mod runs on its own content, registered under its namespace; each still needs a test_instance json per test. */
public final class CommonTests {

    /** Where a block is placed to be checked, inside every mod's test box. */
    private static final BlockPos AT = new BlockPos(2, 2, 2);

    private CommonTests() {
    }

    public static void register(BiConsumer<String, Consumer<GameTestHelper>> out, String modId) {
        out.accept("every_recipe_crafts_from_its_own_ingredients", helper -> report(helper, uncraftableRecipes(helper, modId), "recipes that cannot be made"));
        out.accept("every_block_drops_something", helper -> report(helper, blocksThatDropNothing(helper, modId), "blocks that drop nothing"));
        out.accept("every_block_entity_survives_a_reload", helper -> report(helper, blockEntitiesThatChangeOnReload(helper, modId), "block entities that change across a reload"));
        out.accept("every_item_is_in_a_creative_tab", helper -> report(helper, itemsInNoCreativeTab(helper, modId), "items in no creative tab"));
    }

    /** Crafting and cooking recipes that their own first-choice ingredients do not make. */
    public static List<String> uncraftableRecipes(GameTestHelper helper, String modId) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();

        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            String id = holder.id().identifier().toString();
            Recipe<?> recipe = holder.value();
            if (!holder.id().identifier().getNamespace().equals(modId) || recipe.isSpecial()) {
                continue;
            }

            ItemStack made;
            List<ItemStack> tried = List.of();
            if (recipe instanceof ShapedRecipe shaped) {
                List<ItemStack> grid = new ArrayList<>();
                shaped.getIngredients().forEach(ingredient -> grid.add(ingredient.isPresent() ? first(ingredient.get()) : ItemStack.EMPTY));
                tried = grid;
                CraftingInput input = grid.contains(null) ? null : CraftingInput.of(shaped.getWidth(), shaped.getHeight(), grid);
                made = input == null ? null : shaped.matches(input, level) ? shaped.assemble(input) : ItemStack.EMPTY;
            } else if (recipe instanceof ShapelessRecipe shapeless) {
                List<ItemStack> items = new ArrayList<>();
                shapeless.placementInfo().ingredients().forEach(ingredient -> items.add(first(ingredient)));
                tried = items;
                // A recipe with an ingredient nothing matches has no placement at all, so it lists no ingredients.
                CraftingInput input = items.contains(null) || shapeless.placementInfo().isImpossibleToPlace() ? null : CraftingInput.of(items.size(), 1, items);
                made = input == null ? null : shapeless.matches(input, level) ? shapeless.assemble(input) : ItemStack.EMPTY;
            } else if (recipe instanceof AbstractCookingRecipe cooking) {
                ItemStack item = first(cooking.input());
                SingleRecipeInput input = item == null ? null : new SingleRecipeInput(item);
                made = input == null ? null : cooking.matches(input, level) ? cooking.assemble(input) : ItemStack.EMPTY;
            } else {
                continue;
            }

            // Only an empty tag matches nothing, and that is a recipe waiting on another mod to fill it, as with a knife from Assorted Kitchen.
            if (made != null && made.isEmpty()) {
                problems.add(id + " is not made from its own ingredients " + tried);
            }
        }
        return problems;
    }

    /** Blocks whose every state drops nothing for every common tool, so breaking one loses it; a block meant to drop nothing is left out. */
    public static List<String> blocksThatDropNothing(GameTestHelper helper, String modId) {
        ServerLevel level = helper.getLevel();
        ItemStack silkTouch = new ItemStack(Items.NETHERITE_PICKAXE);
        silkTouch.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        List<ItemStack> tools = List.of(ItemStack.EMPTY, new ItemStack(Items.NETHERITE_PICKAXE), new ItemStack(Items.NETHERITE_AXE),
                new ItemStack(Items.NETHERITE_SHOVEL), new ItemStack(Items.NETHERITE_HOE), new ItemStack(Items.SHEARS), silkTouch);
        BlockPos pos = helper.absolutePos(AT);
        List<String> problems = new ArrayList<>();

        for (Block block : blocks(modId)) {
            if (block.getLootTable().isEmpty()) {
                continue;
            }
            LootTable table = level.getServer().reloadableRegistries().getLootTable(block.getLootTable().get());
            if (table == LootTable.EMPTY) {
                problems.add(BuiltInRegistries.BLOCK.getKey(block) + " names loot table " + block.getLootTable().get().identifier() + ", which does not exist");
                continue;
            }
            // A table with no pools at all drops nothing on purpose, as a cake's does.
            boolean hasPools = LootTable.DIRECT_CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, level.registryAccess()), table)
                    .result().map(json -> json.getAsJsonObject().has("pools")).orElse(true);
            if (!hasPools) {
                continue;
            }

            boolean drops = false;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                for (ItemStack tool : tools) {
                    LootParams.Builder params = new LootParams.Builder(level)
                            .withParameter(LootContextParams.BLOCK_STATE, state)
                            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                            .withParameter(LootContextParams.TOOL, tool);
                    if (block instanceof EntityBlock entityBlock) {
                        BlockEntity entity = entityBlock.newBlockEntity(pos, state);
                        if (entity != null) {
                            params.withParameter(LootContextParams.BLOCK_ENTITY, entity);
                        }
                    }
                    // One source rolled several times, as fresh sources from small seeds all roll nearly alike.
                    RandomSource random = RandomSource.create(0L);
                    for (int roll = 0; roll < 8 && !drops; roll++) {
                        drops = !table.getRandomItems(params.create(LootContextParamSets.BLOCK), random).isEmpty();
                    }
                    if (drops) {
                        break;
                    }
                }
                if (drops) {
                    break;
                }
            }
            if (!drops) {
                problems.add(BuiltInRegistries.BLOCK.getKey(block) + " drops nothing for any tool");
            }
        }
        return problems;
    }

    /** Block entities that save with errors or read back to different data, which loses their contents when a chunk unloads. */
    public static List<String> blockEntitiesThatChangeOnReload(GameTestHelper helper, String modId) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        List<String> problems = new ArrayList<>();

        for (Block block : blocks(modId)) {
            if (!(block instanceof EntityBlock)) {
                continue;
            }
            String name = BuiltInRegistries.BLOCK.getKey(block).toString();
            // No neighbor updates, so a block that needs support stays put long enough to be saved.
            level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                continue;
            }
            try {
                CompoundTag saved = TestSupport.saveWithoutProblems(helper, entity, name);
                BlockEntity loaded = BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
                if (loaded == null) {
                    problems.add(name + " did not load back from its save");
                } else if (!saved.equals(loaded.saveWithFullMetadata(level.registryAccess()))) {
                    problems.add(name + " saved " + saved + " but read back as " + loaded.saveWithFullMetadata(level.registryAccess()));
                }
            } catch (RuntimeException e) {
                problems.add(name + " threw " + e);
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return problems;
    }

    /** Items a creative player cannot find in any tab. */
    public static List<String> itemsInNoCreativeTab(GameTestHelper helper, String modId) {
        CreativeModeTabs.tryRebuildTabContents(FeatureFlags.REGISTRY.allFlags(), true, helper.getLevel().registryAccess());
        Set<Item> shown = new HashSet<>();
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            tab.getDisplayItems().forEach(stack -> shown.add(stack.getItem()));
        }

        List<String> problems = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(modId) && !shown.contains(item)) {
                problems.add(BuiltInRegistries.ITEM.getKey(item).toString());
            }
        }
        return problems;
    }

    private static List<Block> blocks(String modId) {
        return BuiltInRegistries.BLOCK.stream().filter(block -> BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(modId)).toList();
    }

    /** Null when the ingredient matches nothing, such as an empty tag. */
    private static ItemStack first(Ingredient ingredient) {
        Optional<Holder<Item>> item = ingredient.items().findFirst();
        return item.map(ItemStack::new).orElse(null);
    }

    private static void report(GameTestHelper helper, List<String> problems, String what) {
        helper.assertTrue(problems.isEmpty(), problems.size() + " " + what + ":\n  " + String.join("\n  ", problems));
        helper.succeed();
    }
}
