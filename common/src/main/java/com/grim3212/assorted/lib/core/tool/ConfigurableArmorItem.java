package com.grim3212.assorted.lib.core.tool;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;

/** A piece of armour built from a configured armour material. */
public class ConfigurableArmorItem extends Item {

    private final ArmorMaterialConfig material;
    private final ArmorType armorType;

    public ConfigurableArmorItem(ArmorMaterialConfig material, ArmorType type, Properties properties) {
        super(properties.humanoidArmor(material.material(), type));
        this.material = material;
        this.armorType = type;
    }

    public ArmorMaterialConfig getArmorMaterial() {
        return this.material;
    }

    public ArmorType getArmorType() {
        return this.armorType;
    }
}
