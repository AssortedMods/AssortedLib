package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.manual.ManualIngredientTags;
import com.grim3212.assorted.lib.manual.ManualSlot;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The manual's tag notes on a dedicated server. 1.21.1 sends recipe ingredients to clients as plain item
 * lists, so a client has to be told by the server which tags an ingredient was written with; without that
 * the gold slot and its "accepts any" line only ever showed in single player.
 */
final class ManualTagSyncTests {

    private ManualTagSyncTests() {
    }

    static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
        out.accept("manual_tags_reach_a_client_of_a_dedicated_server", ManualTagSyncTests::tagsReachAClientOfADedicatedServer);
    }

    private static void tagsReachAClientOfADedicatedServer(GameTestHelper helper) {
        Map<List<ResourceLocation>, List<TagKey<Item>>> table = ManualIngredientTags.table(helper.getLevel().getServer());

        // The table survives the wire format the packet uses.
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        new ManualIngredientTags.Packet(table).encode(buf);
        Map<List<ResourceLocation>, List<TagKey<Item>>> received = ManualIngredientTags.Packet.decode(buf).tags();
        helper.assertValueEqual(received, table, "the tag table after a trip through the packet");

        // A chest's planks, as a client of a dedicated server gets them: every plank, listed one by one.
        Ingredient asWritten = Ingredient.of(ItemTags.PLANKS);
        Ingredient asSent = Ingredient.of(Arrays.stream(asWritten.getItems()).map(ItemStack::getItem).toArray(ItemLike[]::new));
        ManualSlot slot = ManualSlot.of(asSent);

        ManualIngredientTags.clear();
        try {
            helper.assertTrue(slot.tags().isEmpty(), "a plain item list named tags before the server sent any: " + slot.tags());
            ManualIngredientTags.apply(received);
            helper.assertTrue(slot.tags().contains(ItemTags.PLANKS), "the slot did not name #minecraft:planks once the server's tags arrived: " + slot.tags());
            helper.assertTrue(slot.isChoice(), "the planks slot was not drawn as a choice");
        } finally {
            ManualIngredientTags.clear();
        }
        helper.succeed();
    }
}
