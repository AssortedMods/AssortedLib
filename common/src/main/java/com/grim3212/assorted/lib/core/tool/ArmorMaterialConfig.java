package com.grim3212.assorted.lib.core.tool;

import com.grim3212.assorted.lib.config.IConfigurationBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;

import java.util.EnumMap;
import java.util.function.Supplier;

/**
 * An armour material whose numbers come from a config file. Like {@link ToolTier#material()}, {@link #material()}
 * is baked into items as they register, so changes need a restart.
 */
public class ArmorMaterialConfig {

    private final String name;
    private final Supplier<Holder<SoundEvent>> equipSound;
    private final TagKey<Item> repairItems;
    private final ResourceKey<EquipmentAsset> assetId;
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

    /**
     * Defines the material's options under {@code <path>.<name>}. {@code reductionAmounts} is boots, leggings,
     * chestplate, helmet, and {@code assetId} needs its {@code assets/<namespace>/equipment/<path>.json} or the armour renders untextured.
     */
    public ArmorMaterialConfig(IConfigurationBuilder builder, String path, String name, int durability, int enchantability, float toughness, float knockbackResistance, int[] reductionAmounts,
                               Supplier<Holder<SoundEvent>> equipSound, TagKey<Item> repairItems, ResourceKey<EquipmentAsset> assetId) {
        this.name = name;
        this.equipSound = equipSound;
        this.repairItems = repairItems;
        this.assetId = assetId;
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

    public ArmorMaterial material() {
        if (this.cachedMaterial == null) {
            this.cachedMaterial = new ArmorMaterial(getDurability(), getReductionAmounts(), getEnchantability(), this.equipSound.get(), getToughness(), getKnockbackResistance(), this.repairItems, this.assetId);
        }
        return this.cachedMaterial;
    }

    public EnumMap<ArmorType, Integer> getReductionAmounts() {
        EnumMap<ArmorType, Integer> amounts = new EnumMap<>(ArmorType.class);
        amounts.put(ArmorType.BOOTS, this.bootsReductionAmount.get());
        amounts.put(ArmorType.LEGGINGS, this.leggingsReductionAmount.get());
        amounts.put(ArmorType.CHESTPLATE, this.chestPlateReductionAmount.get());
        amounts.put(ArmorType.HELMET, this.helmetReductionAmount.get());
        return amounts;
    }

    public TagKey<Item> getRepairItems() {
        return this.repairItems;
    }

    public ResourceKey<EquipmentAsset> getAssetId() {
        return this.assetId;
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
