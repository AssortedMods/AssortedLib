package com.grim3212.assorted.lib.core.crafting.ingredient;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.core.fluid.FluidInformation;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.util.LibCommonTags;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LibFluidIngredient {

    /** Recipe files give fluid amounts in millibuckets on every loader. */
    public static final long MILLIBUCKETS_PER_BUCKET = 1000;

    /** A recipe file's amount in this loader's own unit - droplets on Fabric. */
    public static long fromMillibuckets(long millibuckets) {
        return millibuckets * Services.FLUIDS.getBucketAmount() / MILLIBUCKETS_PER_BUCKET;
    }

    public static long toMillibuckets(long amount) {
        return amount * MILLIBUCKETS_PER_BUCKET / Services.FLUIDS.getBucketAmount();
    }

    protected final TagKey<Item> itemTag;
    protected final TagKey<Fluid> fluidTag;
    protected final long amount;

    private List<ItemStack> itemStacks;

    protected LibFluidIngredient(@Nullable TagKey<Item> itemTag, TagKey<Fluid> fluidTag) {
        this(itemTag, fluidTag, Services.FLUIDS.getBucketAmount());
    }

    public LibFluidIngredient(@Nullable TagKey<Item> itemTag, TagKey<Fluid> fluidTag, long amount) {
        this.itemTag = itemTag;
        this.fluidTag = fluidTag;
        this.amount = amount;
    }

    public TagKey<Item> getItemTag() {
        return itemTag;
    }

    public TagKey<Fluid> getFluidTag() {
        return fluidTag;
    }

    public long getAmount() {
        return amount;
    }

    public boolean test(ItemStack stack) {
        if (stack == null)
            return false;
        // If the itemTag is null just check the fluid
        // If the itemTag isn't null check both the tag and the fluid
        return ((itemTag != null && stack.is(itemTag)) || itemTag == null) && doesInputMatchFluid(stack);
    }

    private boolean doesInputMatchFluid(ItemStack stack) {
        // Can we drain at least the amount given from this stack
        Optional<FluidInformation> fluidInformation = Services.FLUIDS.get(stack);
        if (fluidInformation.isPresent()) {
            long extracted = Services.FLUIDS.simulateExtract(stack, this.amount);
            return extracted == this.amount && BuiltInRegistries.FLUID.wrapAsHolder(fluidInformation.get().fluid()).is(this.fluidTag);
        }
        return false;
    }

    /**
     * Every container stack this ingredient accepts: one that already holds the fluid, because a
     * vanilla filled bucket is its own item, and one filled with it, because a container that keeps
     * its contents in components has no filled form until something fills it. Both are needed - the
     * item list and what a recipe viewer draws are built from this, so each loader's ingredient hands
     * these stacks back as its items ({@code ICustomIngredient#getItems} / {@code CustomIngredient#getMatchingStacks})
     * and a container that holds its fluid in components is drawn full.
     */
    public List<ItemStack> getMatchingStacks() {
        if (this.itemStacks == null) {
            final List<ItemStack> stacks = new ArrayList<>();

            for (Holder<Fluid> fluid : BuiltInRegistries.FLUID.getTagOrEmpty(this.fluidTag)) {
                // A fluid tag lists the flowing form beside the source one, and a container filled
                // with the flowing form keeps it: on NeoForge the bucket then reads back as
                // minecraft:flowing_water, where Fabric normalises while building its variant. Both
                // loaders have to end up listing the same stack, so only the source is ever poured.
                final FluidInformation contents = new FluidInformation(fluid.value(), this.amount).withSource();

                for (Holder<Item> container : BuiltInRegistries.ITEM.getTagOrEmpty(LibCommonTags.Items.FLUID_CONTAINERS)) {
                    final ItemStack empty = new ItemStack(container.value());
                    addIfAccepted(stacks, empty);
                    // insertInto works on a copy and hands back an unchanged one when nothing moved,
                    // so a container that cannot take this fluid just fails the test below.
                    addIfAccepted(stacks, Services.FLUIDS.insertInto(empty, contents));
                }
            }

            this.itemStacks = stacks;
        }
        return this.itemStacks;
    }

    /** Keeps a stack this ingredient accepts, unless an equal one is listed already. */
    private void addIfAccepted(final List<ItemStack> stacks, final ItemStack stack) {
        if (test(stack) && stacks.stream().noneMatch(listed -> ItemStack.matches(listed, stack))) {
            stacks.add(stack);
        }
    }

    public void invalidate() {
        this.itemStacks = null;
    }

    public static abstract class Serializer<T extends LibFluidIngredient> {

        public ResourceLocation getIdentifier() {
            return ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, "stored_fluid_ingredient");
        }

        public T read(JsonObject json) {
            TagKey<Item> itemTag = null;
            if (json.has("item")) {
                ResourceLocation itemLocation = ResourceLocation.parse(GsonHelper.getAsString(json, "item"));
                itemTag = TagKey.create(Registries.ITEM, itemLocation);
            }

            ResourceLocation fluidLocation = ResourceLocation.parse(GsonHelper.getAsString(json, "fluid"));
            TagKey<Fluid> fluidTag = TagKey.create(Registries.FLUID, fluidLocation);

            long amount = Services.FLUIDS.getBucketAmount();
            if (json.has("amount")) {
                amount = fromMillibuckets(GsonHelper.getAsLong(json, "amount"));
            }

            if (fluidTag == null) {
                throw new JsonSyntaxException("Must set 'fluid'");
            }

            return create(itemTag, fluidTag, amount);
        }

        public void write(JsonObject output, T ingredient) {
            if (ingredient.itemTag != null)
                output.addProperty("item", ingredient.itemTag.location().toString());
            output.addProperty("fluid", ingredient.fluidTag.location().toString());
            if (ingredient.amount > 0)
                output.addProperty("amount", toMillibuckets(ingredient.amount));
        }

        public T read(FriendlyByteBuf buf) {
            boolean hasItemTag = buf.readBoolean();
            TagKey<Item> itemTag = null;

            if (hasItemTag) {
                itemTag = TagKey.create(Registries.ITEM, buf.readResourceLocation());
            }
            TagKey<Fluid> fluidTag = TagKey.create(Registries.FLUID, buf.readResourceLocation());
            long amount = buf.readLong();
            return create(itemTag, fluidTag, amount);
        }

        public void write(FriendlyByteBuf buf, T ingredient) {
            if (ingredient.itemTag != null) {
                buf.writeBoolean(true);
                buf.writeResourceLocation(ingredient.itemTag.location());
            } else {
                buf.writeBoolean(false);
            }
            buf.writeResourceLocation(ingredient.fluidTag.location());
            buf.writeLong(ingredient.amount);
        }

        protected abstract T create(@Nullable TagKey<Item> itemTag, TagKey<Fluid> fluidTag, long amount);
    }
}
