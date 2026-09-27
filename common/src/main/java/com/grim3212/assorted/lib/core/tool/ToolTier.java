package com.grim3212.assorted.lib.core.tool;

import com.grim3212.assorted.lib.config.IConfigurationBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

import java.util.function.Supplier;

/**
 * A tool material whose numbers come from a config file. {@link #material()} is baked into items as they
 * register, so changes need a restart.
 */
public class ToolTier {

    private final String name;
    private final TagKey<Item> repairItems;
    public final Supplier<Integer> harvestLevel;
    public final Supplier<Integer> maxUses;
    public final Supplier<Integer> enchantability;
    public final Supplier<Double> efficiency;
    public final Supplier<Double> damage;
    public final Supplier<Double> axeDamage;
    public final Supplier<Double> axeSpeed;

    // Cached so every item of one material agrees even if the config reloads underneath.
    private ToolMaterial cachedMaterial;

    /**
     * Defines the tier's options under {@code <path>.<name>}, defaulting to {@code defaults}. The axe values
     * are the damage and speed an axe of this material gets, which vanilla picks per material.
     */
    public ToolTier(IConfigurationBuilder builder, String path, String name, ToolMaterial defaults, float defaultAxeDamage, float defaultAxeSpeed) {
        this.name = name;
        this.repairItems = defaults.repairItems();
        String prefix = path + "." + name + ".";
        this.maxUses = builder.defineInteger(prefix + "maxUses", defaults.durability(), 1, 100000, "The maximum uses for this item tier");
        this.enchantability = builder.defineInteger(prefix + "enchantability", defaults.enchantmentValue(), 0, 100000, "The enchantability for this item tier");
        this.harvestLevel = builder.defineInteger(prefix + "harvestLevel", HarvestTiers.harvestLevelOf(defaults.incorrectBlocksForDrops()), 0, 100, "The harvest level for this item tier. 0 is wood, 1 stone, 2 iron, 3 diamond, 4 and above netherite.");
        this.efficiency = builder.defineDouble(prefix + "efficiency", defaults.speed(), 0F, 100000F, "The efficiency for this item tier");
        this.damage = builder.defineDouble(prefix + "damage", defaults.attackDamageBonus(), 0F, 100000F, "The amount of damage this item tier does");
        this.axeDamage = builder.defineDouble(prefix + "axeDamage", defaultAxeDamage, 0F, 100000F, "The damage modifier for axes as they are different per material. Will not affect vanilla tools.");
        this.axeSpeed = builder.defineDouble(prefix + "axeSpeed", defaultAxeSpeed, -1000F, 100000F, "The speed modifier for axes as they are different per material. Will not affect vanilla tools.");
    }

    public String getName() {
        return this.name;
    }

    public ToolMaterial material() {
        if (this.cachedMaterial == null) {
            this.cachedMaterial = new ToolMaterial(HarvestTiers.incorrectBlocksForDrops(getHarvestLevel()), getMaxUses(), getEfficiency(), getDamage(), getEnchantability(), this.repairItems);
        }
        return this.cachedMaterial;
    }

    /** Durability, repair material and enchantability without any mining or attack behaviour, for tools of no vanilla shape. */
    public Item.Properties tiered(Item.Properties properties) {
        return properties.durability(getMaxUses()).repairable(this.repairItems).enchantable(getEnchantability());
    }

    public TagKey<Item> getRepairItems() {
        return this.repairItems;
    }

    public int getHarvestLevel() {
        return this.harvestLevel.get();
    }

    public int getMaxUses() {
        return this.maxUses.get();
    }

    public float getEfficiency() {
        return this.efficiency.get().floatValue();
    }

    public float getDamage() {
        return this.damage.get().floatValue();
    }

    public int getEnchantability() {
        return this.enchantability.get();
    }

    public float getAxeDamage() {
        return this.axeDamage.get().floatValue();
    }

    public float getAxeSpeed() {
        return this.axeSpeed.get().floatValue();
    }

    @Override
    public String toString() {
        return "[Name:" + getName() + ", HarvestLevel:" + getHarvestLevel() + ", MaxUses:" + getMaxUses() + ", Efficiency:" + getEfficiency() + ", Damage:" + getDamage() + ", Enchantability:" + getEnchantability() + ", AxeDamage:" + getAxeDamage() + ", AxeSpeed:" + getAxeSpeed() + "]";
    }
}
