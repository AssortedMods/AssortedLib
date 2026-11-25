package com.grim3212.assorted.lib.core.tool;

import net.minecraft.world.item.ArmorItem;

/** A piece of armour built from a configured armour material, which must already be registered. */
public class ConfigurableArmorItem extends ArmorItem {

    private final ArmorMaterialConfig armorMaterial;

    public ConfigurableArmorItem(ArmorMaterialConfig material, ArmorItem.Type type, Properties properties) {
        super(material.holder(), type, properties.durability(type.getDurability(material.getDurability())));
        this.armorMaterial = material;
    }

    public ArmorMaterialConfig getArmorMaterial() {
        return this.armorMaterial;
    }

    public ArmorItem.Type getArmorType() {
        return this.type;
    }
}
