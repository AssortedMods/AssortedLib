package com.grim3212.assorted.lib.core.tool;

import com.grim3212.assorted.lib.config.IConfigurationBuilder;
import com.google.common.base.Suppliers;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * A tool material whose numbers come from a config file. It is itself the {@link Tier} a tool is made of;
 * items read it as they register, so changes need a restart.
 */
public class ToolTier implements Tier {

    private final String name;
    private final Supplier<Ingredient> repairIngredient;
    private final TagKey<Item> repairItems;
    public final Supplier<Integer> harvestLevel;
    public final Supplier<Integer> maxUses;
    public final Supplier<Integer> enchantability;
    public final Supplier<Double> efficiency;
    public final Supplier<Double> damage;
    public final Supplier<Double> axeDamage;
    public final Supplier<Double> axeSpeed;

    /**
     * Defines the tier's options under {@code <path>.<name>}, defaulting to {@code defaults}. The axe values
     * are the damage and speed an axe of this material gets, which vanilla picks per material. Repairs stay
     * vanilla's; {@code materialTag} is what {@link #getRepairItems()} names for recipes and creative tabs,
     * since 1.21.1's iron, gold, diamond and netherite tiers name a single item rather than a tag.
     */
    public ToolTier(IConfigurationBuilder builder, String path, String name, Tier defaults, TagKey<Item> materialTag, float defaultAxeDamage, float defaultAxeSpeed) {
        this(builder, path, name, HarvestTiers.harvestLevelOf(defaults.getIncorrectBlocksForDrops()), defaults.getUses(), defaults.getSpeed(), defaults.getAttackDamageBonus(), defaults.getEnchantmentValue(), defaults::getRepairIngredient, materialTag, defaultAxeDamage, defaultAxeSpeed);
    }

    /** Defines a tier of no vanilla material, repaired with anything in {@code repairItems}. */
    public ToolTier(IConfigurationBuilder builder, String path, String name, int harvestLevel, int maxUses, float efficiency, float damage, int enchantability, TagKey<Item> repairItems, float defaultAxeDamage, float defaultAxeSpeed) {
        this(builder, path, name, harvestLevel, maxUses, efficiency, damage, enchantability, () -> Ingredient.of(repairItems), repairItems, defaultAxeDamage, defaultAxeSpeed);
    }

    private ToolTier(IConfigurationBuilder builder, String path, String name, int harvestLevel, int maxUses, float efficiency, float damage, int enchantability, Supplier<Ingredient> repairIngredient, TagKey<Item> repairItems, float defaultAxeDamage, float defaultAxeSpeed) {
        this.name = name;
        this.repairItems = repairItems;
        // Memoized: an ingredient resolves its tag lazily, and every item of the tier should share one.
        this.repairIngredient = Suppliers.memoize(repairIngredient::get);
        String prefix = path + "." + name + ".";
        this.maxUses = builder.defineInteger(prefix + "maxUses", maxUses, 1, 100000, "The maximum uses for this item tier");
        this.enchantability = builder.defineInteger(prefix + "enchantability", enchantability, 0, 100000, "The enchantability for this item tier");
        this.harvestLevel = builder.defineInteger(prefix + "harvestLevel", harvestLevel, 0, 100, "The harvest level for this item tier. 0 is wood, 1 stone, 2 iron, 3 diamond, 4 and above netherite.");
        this.efficiency = builder.defineDouble(prefix + "efficiency", efficiency, 0F, 100000F, "The efficiency for this item tier");
        this.damage = builder.defineDouble(prefix + "damage", damage, 0F, 100000F, "The amount of damage this item tier does");
        this.axeDamage = builder.defineDouble(prefix + "axeDamage", defaultAxeDamage, 0F, 100000F, "The damage modifier for axes as they are different per material. Will not affect vanilla tools.");
        this.axeSpeed = builder.defineDouble(prefix + "axeSpeed", defaultAxeSpeed, -1000F, 100000F, "The speed modifier for axes as they are different per material. Will not affect vanilla tools.");
    }

    public String getName() {
        return this.name;
    }

    /** The tier to build a {@code TieredItem} from. The tier is its own material. */
    public Tier material() {
        return this;
    }

    @Override
    public int getUses() {
        return getMaxUses();
    }

    @Override
    public float getSpeed() {
        return getEfficiency();
    }

    @Override
    public float getAttackDamageBonus() {
        return getDamage();
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return HarvestTiers.incorrectBlocksForDrops(getHarvestLevel());
    }

    @Override
    public int getEnchantmentValue() {
        return getEnchantability();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    /** The item tag this tier's tools are made from, as the 26.x line's tier names it, for recipes and creative tabs. */
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
