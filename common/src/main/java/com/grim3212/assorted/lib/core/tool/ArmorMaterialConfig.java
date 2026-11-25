package com.grim3212.assorted.lib.core.tool;

import com.grim3212.assorted.lib.config.IConfigurationBuilder;
import com.grim3212.assorted.lib.registry.IRegistryObject;
import com.grim3212.assorted.lib.registry.RegistryProvider;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * An armour material whose numbers come from a config file. Armour materials are a registry, so the owning mod
 * calls {@link #register(RegistryProvider)} with its {@code Registries.ARMOR_MATERIAL} provider before making
 * any {@link ConfigurableArmorItem}. The material is read as the items register, so changes need a restart.
 */
public class ArmorMaterialConfig {

    private final String name;
    private final Supplier<Holder<SoundEvent>> equipSound;
    private final TagKey<Item> repairItems;
    private final ResourceLocation assetName;
    public final Supplier<Integer> durability;
    public final Supplier<Integer> enchantability;
    public final Supplier<Double> toughness;
    public final Supplier<Double> knockbackResistance;
    public final Supplier<Integer> bootsReductionAmount;
    public final Supplier<Integer> leggingsReductionAmount;
    public final Supplier<Integer> chestPlateReductionAmount;
    public final Supplier<Integer> helmetReductionAmount;

    // Cached so the four pieces agree.
    private ArmorMaterial cachedMaterial;
    private IRegistryObject<ArmorMaterial> registered;

    /**
     * Defines the material's options under {@code <path>.<name>}. {@code reductionAmounts} is boots, leggings,
     * chestplate, helmet, and {@code assetName} names the armour's
     * {@code assets/<namespace>/textures/models/armor/<path>_layer_1.png} and {@code _layer_2.png}, or the armour renders untextured.
     */
    public ArmorMaterialConfig(IConfigurationBuilder builder, String path, String name, int durability, int enchantability, float toughness, float knockbackResistance, int[] reductionAmounts,
                               Supplier<Holder<SoundEvent>> equipSound, TagKey<Item> repairItems, ResourceLocation assetName) {
        this.name = name;
        this.equipSound = equipSound;
        this.repairItems = repairItems;
        this.assetName = assetName;
        String prefix = path + "." + name + ".";
        this.durability = builder.defineInteger(prefix + "durability", durability, 1, 100000, "The durability multiplier for this armor material");
        this.enchantability = builder.defineInteger(prefix + "enchantability", enchantability, 0, 100000, "The enchantability for this armor material");
        this.toughness = builder.defineDouble(prefix + "toughness", toughness, 0F, 100000F, "The toughness for this armor material");
        this.knockbackResistance = builder.defineDouble(prefix + "knockbackResistance", knockbackResistance, 0F, 100000F, "The knockback resistance for this armor material");
        this.bootsReductionAmount = builder.defineInteger(prefix + "bootsReductionAmount", reductionAmounts[0], 0, 100000, "The reduction amount for the boots of this armor material");
        this.leggingsReductionAmount = builder.defineInteger(prefix + "leggingsReductionAmount", reductionAmounts[1], 0, 100000, "The reduction amount for the leggings of this armor material");
        this.chestPlateReductionAmount = builder.defineInteger(prefix + "chestPlateReductionAmount", reductionAmounts[2], 0, 100000, "The reduction amount for the chestplate of this armor material");
        this.helmetReductionAmount = builder.defineInteger(prefix + "helmetReductionAmount", reductionAmounts[3], 0, 100000, "The reduction amount for the helmet of this armor material");
    }

    public String getName() {
        return this.name;
    }

    /** The material's value. It carries no durability: that goes on each piece's properties, see {@link ConfigurableArmorItem}. */
    public ArmorMaterial material() {
        if (this.cachedMaterial == null) {
            this.cachedMaterial = new ArmorMaterial(getReductionAmounts(), getEnchantability(), this.equipSound.get(), () -> Ingredient.of(this.repairItems), List.of(new ArmorMaterial.Layer(this.assetName)), getToughness(), getKnockbackResistance());
        }
        return this.cachedMaterial;
    }

    /** Registers the material under its name. Call once, before any piece of it is constructed. */
    public IRegistryObject<ArmorMaterial> register(RegistryProvider<ArmorMaterial> armorMaterials) {
        if (this.registered != null) {
            throw new IllegalStateException("Armor material " + this.name + " is already registered");
        }
        this.registered = armorMaterials.register(this.name, this::material);
        return this.registered;
    }

    /** The registered material, which is what an {@link ArmorItem} holds. */
    public Holder<ArmorMaterial> holder() {
        if (this.registered == null) {
            throw new IllegalStateException("Armor material " + this.name + " was used before it was registered");
        }
        return this.registered.asHolder();
    }

    public EnumMap<ArmorItem.Type, Integer> getReductionAmounts() {
        EnumMap<ArmorItem.Type, Integer> amounts = new EnumMap<>(ArmorItem.Type.class);
        amounts.put(ArmorItem.Type.BOOTS, this.bootsReductionAmount.get());
        amounts.put(ArmorItem.Type.LEGGINGS, this.leggingsReductionAmount.get());
        amounts.put(ArmorItem.Type.CHESTPLATE, this.chestPlateReductionAmount.get());
        amounts.put(ArmorItem.Type.HELMET, this.helmetReductionAmount.get());
        return amounts;
    }

    public TagKey<Item> getRepairItems() {
        return this.repairItems;
    }

    public ResourceLocation getAssetName() {
        return this.assetName;
    }

    public int getDurability() {
        return this.durability.get();
    }

    public int getEnchantability() {
        return this.enchantability.get();
    }

    public float getToughness() {
        return this.toughness.get().floatValue();
    }

    public float getKnockbackResistance() {
        return this.knockbackResistance.get().floatValue();
    }

    @Override
    public String toString() {
        return "[Name:" + getName() + ", Durability:" + getDurability() + ", ReductionAmounts:" + getReductionAmounts().values() + ", Enchantability:" + getEnchantability() + ", Toughness:" + getToughness() + ", KnockbackResistance:" + getKnockbackResistance() + "]";
    }
}
