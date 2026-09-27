package com.grim3212.assorted.lib.gametest;

import com.google.gson.JsonParser;
import com.grim3212.assorted.lib.migration.AdvancementIcons;
import com.grim3212.assorted.lib.migration.MovedIds;
import com.grim3212.assorted.lib.test.TestSupport;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBookSettings;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Content that moved to another namespace, as a mod split into several moves its own: saved recipe books, progress,
 * structures and chest loot carry over, and a shared advancement's icon is picked from what is installed.
 */
final class MovedIdsTests {

    /** Stands for a namespace whose recipes and advancements vanilla's now has, at the same paths. */
    private static final String OLD = "assortedlib_old";
    private static final Identifier STORY_ROOT = Identifier.withDefaultNamespace("story/root");

    private MovedIdsTests() {
    }

    static void install() {
        MovedIds.inherit(OLD, Identifier.DEFAULT_NAMESPACE);
        MovedIds.renameCriteria(STORY_ROOT, Map.of("old_crafting_table", "crafting_table"));
        AdvancementIcons.register(STORY_ROOT, List.of(Identifier.fromNamespaceAndPath(OLD, "not_installed"), Identifier.withDefaultNamespace("clock")));
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("moved_ids_advancement_icon_is_the_first_installed", MovedIdsTests::advancementIconIsTheFirstInstalled);
        out.accept("moved_ids_recipe_book_carries_over", MovedIdsTests::recipeBookCarriesOver);
        out.accept("moved_ids_advancement_progress_carries_over", MovedIdsTests::advancementProgressCarriesOver);
        out.accept("moved_ids_chunk_structures_carry_over", MovedIdsTests::chunkStructuresCarryOver);
        out.accept("moved_ids_loot_table_carries_over", MovedIdsTests::lootTableCarriesOver);
        out.accept("moved_ids_enchantments_carry_over", MovedIdsTests::enchantmentsCarryOver);
    }

    private static void advancementIconIsTheFirstInstalled(GameTestHelper helper) {
        AdvancementHolder root = helper.getLevel().getServer().getAdvancements().get(STORY_ROOT);
        helper.assertTrue(root.value().display().orElseThrow().getIcon().item().value() == Items.CLOCK,
                "the story root is drawn with " + root.value().display().orElseThrow().getIcon() + ", not the clock, the first of its icons installed");
        helper.succeed();
    }

    private static void recipeBookCarriesOver(GameTestHelper helper) {
        ResourceKey<Recipe<?>> moved = ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("crafting_table"));
        ResourceKey<Recipe<?>> old = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(OLD, "crafting_table"));
        ResourceKey<Recipe<?>> gone = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(OLD, "never_was"));
        ServerRecipeBook book = TestSupport.survivalPlayer(helper).getRecipeBook();

        book.loadUntrusted(new ServerRecipeBook.Packed(new RecipeBookSettings(), List.of(old, gone), List.of(old)),
                key -> helper.getLevel().recipeAccess().byKey(key).isPresent());
        helper.assertTrue(book.contains(moved), "a recipe saved under its old id was not carried over to " + moved.identifier());
        helper.assertFalse(book.contains(old) || book.contains(gone), "an old or unknown recipe id was kept");
        helper.succeed();
    }

    /** A real progress file, read the way a player's is when they join. */
    private static void advancementProgressCarriesOver(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = TestSupport.survivalPlayer(helper);
        Path saved;
        try {
            saved = Files.createTempFile("assortedlib-advancements", ".json");
            Files.writeString(saved, "{\"" + OLD + ":story/root\": {\"criteria\": {\"old_crafting_table\": \"2026-01-01 00:00:00 +0000\"}, \"done\": true}, "
                    + "\"DataVersion\": " + SharedConstants.getCurrentVersion().dataVersion().version() + "}");
        } catch (IOException e) {
            throw helper.assertionException("could not write a progress file: " + e);
        }

        PlayerAdvancements advancements = new PlayerAdvancements(server.getFixerUpper(), server.getPlayerList(), server.getAdvancements(), saved, player);
        helper.assertTrue(advancements.getOrStartProgress(server.getAdvancements().get(STORY_ROOT)).isDone(),
                "progress saved under the old id and criterion did not carry over to " + STORY_ROOT);
        helper.succeed();
    }

    /** Laid out the way a chunk saves its structure starts. */
    private static void chunkStructuresCarryOver(GameTestHelper helper) {
        Registry<Structure> structures = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        CompoundTag start = new CompoundTag();
        start.putString("id", OLD + ":village_plains");
        CompoundTag starts = new CompoundTag();
        starts.put(OLD + ":village_plains", start);
        starts.put(OLD + ":never_was", new CompoundTag());

        MovedIds.structures(starts, structures::containsKey);
        CompoundTag moved = starts.getCompoundOrEmpty("minecraft:village_plains");
        helper.assertTrue("minecraft:village_plains".equals(moved.getStringOr("id", "")),
                "a structure start saved under its old id was not carried over, whole, to minecraft:village_plains: " + starts);
        helper.assertTrue(starts.contains(OLD + ":never_was") && !starts.contains(OLD + ":village_plains"), "an unknown structure id was changed, or the old one kept: " + starts);
        helper.succeed();
    }

    /** Asked for the way an unopened chest asks for its loot. */
    private static void lootTableCarriesOver(GameTestHelper helper) {
        ReloadableServerRegistries.Holder registries = helper.getLevel().getServer().reloadableRegistries();
        LootTable old = registries.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(OLD, "chests/simple_dungeon")));
        helper.assertTrue(old == registries.getLootTable(BuiltInLootTables.SIMPLE_DUNGEON), "a loot table asked for by its old id did not give minecraft:chests/simple_dungeon");
        helper.assertTrue(registries.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(OLD, "never_was"))) == LootTable.EMPTY,
                "an unknown loot table id did not stay empty");
        helper.succeed();
    }

    /** An item and a book saved with enchantments under an old id, read the way a chest reads its items. */
    private static void enchantmentsCarryOver(GameTestHelper helper) {
        RegistryAccess access = helper.getLevel().registryAccess();
        Holder<Enchantment> sharpness = access.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> mending = access.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);

        ItemStack sword = readStack(access, "{\"id\": \"minecraft:diamond_sword\", \"count\": 1, \"components\": {\"minecraft:enchantments\": {\"" + OLD + ":sharpness\": 3, \"" + OLD + ":never_was\": 1}}}");
        helper.assertTrue(sword.is(Items.DIAMOND_SWORD), "the sword itself was lost");
        ItemEnchantments enchantments = sword.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        helper.assertTrue(enchantments.getLevel(sharpness) == 3, "sharpness saved under its old id did not carry over, got " + enchantments);
        helper.assertTrue(enchantments.size() == 1, "an unknown enchantment id was kept, got " + enchantments);

        ItemStack book = readStack(access, "{\"id\": \"minecraft:enchanted_book\", \"count\": 1, \"components\": {\"minecraft:stored_enchantments\": {\"" + OLD + ":mending\": 1}}}");
        helper.assertTrue(book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(mending) == 1, "a book's stored enchantment saved under its old id did not carry over");
        helper.succeed();
    }

    private static ItemStack readStack(RegistryAccess access, String json) {
        // Partial, as a chest keeps what it could read of an item.
        return ItemStack.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, access), JsonParser.parseString(json)).resultOrPartial(error -> {
        }).orElse(ItemStack.EMPTY);
    }
}
