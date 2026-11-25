package com.grim3212.assorted.lib.manual;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.platform.services.INetworkHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The tags each recipe ingredient was written with, sent from the server so the manual can say which
 * tags a slot accepts. 1.21.1 sends a recipe's ingredients to clients as plain item lists, so a client
 * connected to a dedicated server cannot read a tag off an ingredient itself; the 26.x line got the tag
 * in the recipe's slot display. Keyed by the set of items an ingredient holds, which is what the client
 * still has, so it serves every slot the manual draws: vanilla recipes, a mod's own recipe views and a
 * layout's extras alike. Sent on every datapack sync, so on join and after each {@code /reload}.
 */
public final class ManualIngredientTags {

    public static final ResourceLocation PACKET_ID = ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, "manual_ingredient_tags");

    /** What the server last sent; replaced whole, read from the render thread. */
    private static volatile Map<List<ResourceLocation>, List<TagKey<Item>>> synced = Map.of();

    private ManualIngredientTags() {
    }

    public static void init() {
        Services.NETWORK.register(new INetworkHelper.MessageHandler<>(PACKET_ID, Packet.class, Packet::encode, Packet::decode,
                ManualIngredientTags::receive, INetworkHelper.MessageBoundSide.CLIENT));
    }

    /** From each loader's datapack sync hook, for every player it names. */
    public static void sendTo(ServerPlayer player) {
        Services.NETWORK.sendTo(player, new Packet(table(player.getServer())));
    }

    /** What a client holds once the server's table arrives; also how a test stands in for that client. */
    public static void apply(Map<List<ResourceLocation>, List<TagKey<Item>>> table) {
        synced = Map.copyOf(table);
    }

    /** On disconnect, so another server's tags are never shown. */
    public static void clear() {
        synced = Map.of();
    }

    /** The tags the server wrote an ingredient with that holds exactly these stacks' items, if it sent any. */
    static List<TagKey<Item>> lookup(ItemStack[] stacks) {
        Map<List<ResourceLocation>, List<TagKey<Item>>> table = synced;
        return table.isEmpty() ? List.of() : table.getOrDefault(key(stacks), List.of());
    }

    private static void receive(Packet packet, Player player) {
        apply(packet.tags());
    }

    /** Every recipe ingredient written with a tag, by the items it holds: what {@link #sendTo} sends. */
    public static Map<List<ResourceLocation>, List<TagKey<Item>>> table(MinecraftServer server) {
        Map<List<ResourceLocation>, Set<TagKey<Item>>> table = new HashMap<>();
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            for (Ingredient ingredient : holder.value().getIngredients()) {
                if (ingredient.isEmpty()) {
                    continue;
                }
                List<TagKey<Item>> tags = ManualSlot.tagsWrittenIn(ingredient);
                if (!tags.isEmpty()) {
                    table.computeIfAbsent(key(ingredient.getItems()), key -> new LinkedHashSet<>()).addAll(tags);
                }
            }
        }
        Map<List<ResourceLocation>, List<TagKey<Item>>> out = new HashMap<>();
        table.forEach((key, tags) -> out.put(key, List.copyOf(tags)));
        return out;
    }

    /** The distinct items of an ingredient's stacks, sorted, so the same set gives the same key on both sides. */
    private static List<ResourceLocation> key(ItemStack[] stacks) {
        Set<ResourceLocation> ids = new TreeSet<>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            }
        }
        return List.copyOf(ids);
    }

    public record Packet(Map<List<ResourceLocation>, List<TagKey<Item>>> tags) {

        public static Packet decode(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            Map<List<ResourceLocation>, List<TagKey<Item>>> tags = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                List<ResourceLocation> items = buf.readList(FriendlyByteBuf::readResourceLocation);
                List<TagKey<Item>> keys = new ArrayList<>(buf.readList(b -> TagKey.create(Registries.ITEM, b.readResourceLocation())));
                tags.put(List.copyOf(items), List.copyOf(keys));
            }
            return new Packet(tags);
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.tags.size());
            this.tags.forEach((items, keys) -> {
                buf.writeCollection(items, FriendlyByteBuf::writeResourceLocation);
                buf.writeCollection(keys, (b, key) -> b.writeResourceLocation(key.location()));
            });
        }
    }
}
