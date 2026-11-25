package com.grim3212.assorted.lib.manual;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * One recipe slot, held as the {@link Ingredient} behind it rather than as resolved stacks so the
 * tag behind it survives for the tooltip. A furnace's fuel slot is no ingredient at all, so it is
 * {@link #anyFuel()} instead.
 * <p>
 * A recipe sent to a client over the network arrives with plain ingredients flattened to item
 * lists, so when playing on a server the tags are only known for ingredients the client built
 * itself, such as a layout's.
 */
public final class ManualSlot {

    public static final ManualSlot EMPTY = new ManualSlot(Ingredient.EMPTY, false, 0);

    private final Ingredient ingredient;
    private final boolean anyFuel;
    private final int count;

    private List<TagKey<Item>> tags;
    private List<ItemStack> fuels;

    private ManualSlot(Ingredient ingredient, boolean anyFuel, int count) {
        this.ingredient = ingredient;
        this.anyFuel = anyFuel;
        this.count = count;
    }

    public static ManualSlot of(Ingredient ingredient) {
        return new ManualSlot(ingredient, false, 0);
    }

    /** @param count stack size to override with, or 0 to keep what the ingredient gives */
    public static ManualSlot of(Ingredient ingredient, int count) {
        return new ManualSlot(ingredient, false, count);
    }

    /** A result, or any one fixed stack, drawn with its own count and components. */
    public static ManualSlot of(ItemStack stack) {
        return stack.isEmpty() ? EMPTY : new ManualSlot(Ingredient.of(stack), false, 0);
    }

    /** Any one of {@code items}, cycling. */
    public static ManualSlot of(ItemLike... items) {
        return new ManualSlot(Ingredient.of(items), false, 0);
    }

    /** Everything that burns in a furnace, cycling. */
    public static ManualSlot anyFuel() {
        return anyFuel(0);
    }

    public static ManualSlot anyFuel(int count) {
        return new ManualSlot(Ingredient.EMPTY, true, count);
    }

    public Ingredient ingredient() {
        return this.ingredient;
    }

    public boolean isAnyFuel() {
        return this.anyFuel;
    }

    /** Every stack this slot accepts, in cycling order. */
    public List<ItemStack> stacks() {
        List<ItemStack> stacks = this.anyFuel ? this.fuels() : Arrays.asList(this.ingredient.getItems());
        if (this.count <= 0) {
            return stacks;
        }

        return stacks.stream().map(stack -> stack.copyWithCount(this.count)).toList();
    }

    /**
     * The tags this slot will take anything from, for the note in its tooltip. More than one only
     * happens for an ingredient built out of several. Read off the ingredient where it still has them
     * (single player, a layout's extras); a client of a dedicated server gets its ingredients as plain
     * item lists, so there the server's {@link ManualIngredientTags} answer instead. That answer is not
     * kept here, so a slot made before it arrives still picks it up.
     */
    public List<TagKey<Item>> tags() {
        if (this.tags == null) {
            this.tags = this.anyFuel || this.ingredient.isEmpty() ? List.of() : collectTags(this.ingredient);
        }
        if (this.tags.isEmpty() && !this.anyFuel && !this.ingredient.isEmpty()) {
            return ManualIngredientTags.lookup(this.ingredient.getItems());
        }
        return this.tags;
    }

    /** The tags an ingredient was written with, for the server to send; see {@link ManualIngredientTags}. */
    static List<TagKey<Item>> tagsWrittenIn(Ingredient ingredient) {
        return collectTags(ingredient);
    }

    /** Whether the slot takes any one of several things, which is what the book marks in gold. */
    public boolean isChoice() {
        return this.anyFuel || !this.tags().isEmpty() || this.stacks().size() > 1;
    }

    public boolean isEmpty() {
        return !this.anyFuel && this.ingredient.isEmpty();
    }

    /** Worked out once per slot, as it tests every registered item. */
    private List<ItemStack> fuels() {
        if (this.fuels == null) {
            this.fuels = BuiltInRegistries.ITEM.stream()
                    .map(ItemStack::new)
                    .filter(AbstractFurnaceBlockEntity::isFuel)
                    .toList();
        }
        return this.fuels;
    }

    /**
     * An ingredient keeps its values to itself, so the tags are read off its own json: a tag value
     * is written as {@code "tag"}, alone, in a list, or inside a loader's compound ingredient.
     */
    private static List<TagKey<Item>> collectTags(Ingredient ingredient) {
        List<TagKey<Item>> out = new ArrayList<>();
        Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient).result().ifPresent(json -> collectTags(json, out));
        return List.copyOf(out);
    }

    private static void collectTags(JsonElement json, List<TagKey<Item>> out) {
        if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(element -> collectTags(element, out));
        } else if (json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                JsonElement value = entry.getValue();
                if (entry.getKey().equals("tag") && value.isJsonPrimitive()) {
                    ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
                    if (id != null) {
                        out.add(TagKey.create(Registries.ITEM, id));
                    }
                } else {
                    collectTags(value, out);
                }
            }
        }
    }
}
