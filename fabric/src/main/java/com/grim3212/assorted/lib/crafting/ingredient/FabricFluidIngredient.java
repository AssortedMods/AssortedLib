package com.grim3212.assorted.lib.crafting.ingredient;

import com.grim3212.assorted.lib.core.crafting.ingredient.LibFluidIngredient;
import com.grim3212.assorted.lib.platform.Services;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class FabricFluidIngredient extends LibFluidIngredient implements CustomIngredient {

    public static final Serializer SERIALIZER = new Serializer();

    protected FabricFluidIngredient(@Nullable TagKey<Item> itemTag, TagKey<Fluid> fluidTag) {
        this(itemTag, fluidTag, Services.FLUIDS.getBucketAmount());
    }

    protected FabricFluidIngredient(@Nullable TagKey<Item> itemTag, TagKey<Fluid> fluidTag, long amount) {
        super(itemTag, fluidTag, amount);
    }

    public static FabricFluidIngredient of(@Nullable TagKey<Item> itemTag, TagKey<Fluid> fluidTag) {
        return new FabricFluidIngredient(itemTag, fluidTag);
    }

    public static FabricFluidIngredient of(@Nullable TagKey<Item> itemTag, TagKey<Fluid> fluidTag, long amount) {
        return new FabricFluidIngredient(itemTag, fluidTag, amount);
    }

    /**
     * The contained fluid is read off each stack's components, so an item match alone is never
     * enough: {@link #test(ItemStack)} has to run. {@code getMatchingStacks()} is inherited from
     * {@link LibFluidIngredient} and lists the filled containers, which is what recipe viewers draw.
     */
    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    static class Serializer extends LibFluidIngredient.Serializer<FabricFluidIngredient> implements CustomIngredientSerializer<FabricFluidIngredient> {

        // The json/buffer pair the common serializer still carries is not a registration mechanism
        // anymore: Fabric asks for a MapCodec and a StreamCodec, so the same three fields are
        // described once here and both directions fall out of it.
        private static final MapCodec<FabricFluidIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                TagKey.codec(Registries.ITEM).optionalFieldOf("item").forGetter(ingredient -> Optional.ofNullable(ingredient.getItemTag())),
                TagKey.codec(Registries.FLUID).fieldOf("fluid").forGetter(LibFluidIngredient::getFluidTag),
                // Millibuckets in the file, as on NeoForge, so one recipe file reads the same on both.
                Codec.LONG.optionalFieldOf("amount", LibFluidIngredient.MILLIBUCKETS_PER_BUCKET)
                        .xmap(LibFluidIngredient::fromMillibuckets, LibFluidIngredient::toMillibuckets)
                        .forGetter(LibFluidIngredient::getAmount)
        ).apply(instance, Serializer::create));

        private static final StreamCodec<RegistryFriendlyByteBuf, FabricFluidIngredient> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(tagStreamCodec(Registries.ITEM)), ingredient -> Optional.ofNullable(ingredient.getItemTag()),
                tagStreamCodec(Registries.FLUID), LibFluidIngredient::getFluidTag,
                ByteBufCodecs.VAR_LONG, LibFluidIngredient::getAmount,
                (itemTag, fluidTag, amount) -> new FabricFluidIngredient(itemTag.orElse(null), fluidTag, amount)
        );

        /** A tag key on the wire is its id; the registry is implied by the field. */
        private static <T> StreamCodec<RegistryFriendlyByteBuf, TagKey<T>> tagStreamCodec(final ResourceKey<? extends Registry<T>> registry) {
            return ResourceLocation.STREAM_CODEC.<RegistryFriendlyByteBuf>cast().map(id -> TagKey.create(registry, id), TagKey::location);
        }

        private static FabricFluidIngredient create(final Optional<TagKey<Item>> itemTag, final TagKey<Fluid> fluidTag, final long amount) {
            return new FabricFluidIngredient(itemTag.orElse(null), fluidTag, amount);
        }

        @Override
        protected FabricFluidIngredient create(@Nullable TagKey itemTag, TagKey fluidTag, long amount) {
            return new FabricFluidIngredient(itemTag, fluidTag, amount);
        }

        @Override
        public ResourceLocation getIdentifier() {
            return super.getIdentifier();
        }

        @Override
        public MapCodec<FabricFluidIngredient> getCodec(boolean allowEmpty) {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FabricFluidIngredient> getPacketCodec() {
            return STREAM_CODEC;
        }
    }
}
