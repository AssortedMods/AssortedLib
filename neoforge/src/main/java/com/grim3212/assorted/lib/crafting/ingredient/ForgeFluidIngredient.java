package com.grim3212.assorted.lib.crafting.ingredient;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.core.crafting.ingredient.LibFluidIngredient;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An {@link ICustomIngredient}, wrapped by a vanilla {@link Ingredient} via
 * {@link ICustomIngredient#toVanilla()} and serialised through its registered
 * {@link IngredientType}.
 */
public class ForgeFluidIngredient implements ICustomIngredient {

    public static final ResourceLocation NAME = ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, "stored_fluid_ingredient");

    public static final MapCodec<ForgeFluidIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(TagKey.codec(Registries.ITEM).optionalFieldOf("item").forGetter(ingredient -> Optional.ofNullable(ingredient.fluidIngredient.getItemTag())),
                    TagKey.codec(Registries.FLUID).fieldOf("fluid").forGetter(ingredient -> ingredient.fluidIngredient.getFluidTag()),
                    Codec.LONG.optionalFieldOf("amount", (long) FluidType.BUCKET_VOLUME).forGetter(ingredient -> ingredient.fluidIngredient.getAmount()))
            .apply(instance, ForgeFluidIngredient::new));

    public static final IngredientType<ForgeFluidIngredient> TYPE = new IngredientType<>(CODEC);

    protected final LibFluidIngredient fluidIngredient;

    protected ForgeFluidIngredient(Optional<TagKey<Item>> itemTag, TagKey<Fluid> fluidTag, long amount) {
        this(itemTag.orElse(null), fluidTag, amount);
    }

    protected ForgeFluidIngredient(@Nullable TagKey<Item> itemTag, TagKey<Fluid> fluidTag) {
        this(itemTag, fluidTag, FluidType.BUCKET_VOLUME);
    }

    protected ForgeFluidIngredient(@Nullable TagKey<Item> itemTag, TagKey<Fluid> fluidTag, long amount) {
        this.fluidIngredient = new LibFluidIngredient(itemTag, fluidTag, amount);
    }

    public static ForgeFluidIngredient of(@Nullable TagKey<Item> itemTag, TagKey<Fluid> fluidTag) {
        return new ForgeFluidIngredient(itemTag, fluidTag);
    }

    public static ForgeFluidIngredient of(@Nullable TagKey<Item> itemTag, TagKey<Fluid> fluidTag, long amount) {
        return new ForgeFluidIngredient(itemTag, fluidTag, amount);
    }

    @Override
    public boolean test(ItemStack input) {
        return this.fluidIngredient.test(input);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE;
    }

    /**
     * The filled containers this ingredient matches, which is what the recipe book and recipe viewers
     * draw. An item alone cannot say what fluid is inside it, so the contents are checked per stack in
     * {@link #test(ItemStack)}.
     */
    @Override
    public Stream<ItemStack> getItems() {
        return this.fluidIngredient.getMatchingStacks().stream();
    }

    public void invalidate() {
        this.fluidIngredient.invalidate();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForgeFluidIngredient that = (ForgeFluidIngredient) o;
        return this.fluidIngredient.getAmount() == that.fluidIngredient.getAmount()
                && Objects.equals(this.fluidIngredient.getItemTag(), that.fluidIngredient.getItemTag())
                && Objects.equals(this.fluidIngredient.getFluidTag(), that.fluidIngredient.getFluidTag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.fluidIngredient.getItemTag(), this.fluidIngredient.getFluidTag(), this.fluidIngredient.getAmount());
    }
}
