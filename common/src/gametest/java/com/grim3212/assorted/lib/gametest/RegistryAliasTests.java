package com.grim3212.assorted.lib.gametest;

import com.google.gson.JsonParser;
import com.grim3212.assorted.lib.manual.LibItems;
import com.grim3212.assorted.lib.platform.Services;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** An id moved to another namespace still reads, as a world saved before a mod split does. */
final class RegistryAliasTests {

    /** Stands for an id the manual used to have. Nothing registers it. */
    static final Identifier OLD_MANUAL = Identifier.fromNamespaceAndPath("assortedlib_old", "instruction_manual");

    private RegistryAliasTests() {
    }

    static void install() {
        Services.REGISTRY_FACTORY.alias(Registries.ITEM, OLD_MANUAL, LibItems.INSTRUCTION_MANUAL.getId());
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("registry_alias_reads_the_old_id", RegistryAliasTests::registryAliasReadsTheOldId);
    }

    private static void registryAliasReadsTheOldId(GameTestHelper helper) {
        helper.assertValueEqual(BuiltInRegistries.ITEM.getValue(OLD_MANUAL), LibItems.INSTRUCTION_MANUAL.get(), "the item under its old id");

        ItemStack saved = ItemStack.CODEC.parse(helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE),
                JsonParser.parseString("{\"id\": \"" + OLD_MANUAL + "\", \"count\": 2}")).getOrThrow();
        helper.assertTrue(saved.is(LibItems.INSTRUCTION_MANUAL.get()), "a stack saved under the old id reads back as " + saved);
        helper.assertValueEqual(saved.getCount(), 2, "the stack's count");
        helper.succeed();
    }
}
