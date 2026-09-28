package com.grim3212.assorted.lib.core.tool;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.config.ConfigurationType;
import com.grim3212.assorted.lib.config.IConfigurationBuilder;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.util.LibCommonTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The tool materials the Assorted mods share, so a tin hammer and a tin bucket from two mods agree. The
 * config file is only made the first time a mod calls {@link #get()}, and it changes no vanilla tool.
 */
public final class ToolTiers {

    public static final String CONFIG_NAME = LibConstants.MOD_ID + "-tool-tiers";

    // Concurrent: NeoForge constructs mods in parallel, and the first to ask makes the file.
    private static volatile ToolTiers instance;

    public final ToolTier wood;
    public final ToolTier stone;
    public final ToolTier gold;
    public final ToolTier iron;
    public final ToolTier diamond;
    public final ToolTier netherite;
    private final Map<String, ToolTier> extras = new LinkedHashMap<>();

    private ToolTiers() {
        // Needed at registration: items bake their material as they are constructed.
        IConfigurationBuilder builder = Services.CONFIG.createBuilder(ConfigurationType.NEEDED_AT_REGISTRATION, CONFIG_NAME);

        this.wood = new ToolTier(builder, "vanilla_materials", "wood", ToolMaterial.WOOD, 6.0F, -3.2F);
        this.stone = new ToolTier(builder, "vanilla_materials", "stone", ToolMaterial.STONE, 7.0F, -3.2F);
        this.gold = new ToolTier(builder, "vanilla_materials", "gold", ToolMaterial.GOLD, 6.0F, -3.0F);
        this.iron = new ToolTier(builder, "vanilla_materials", "iron", ToolMaterial.IRON, 6.0F, -3.1F);
        this.diamond = new ToolTier(builder, "vanilla_materials", "diamond", ToolMaterial.DIAMOND, 5.0F, -3.0F);
        this.netherite = new ToolTier(builder, "vanilla_materials", "netherite", ToolMaterial.NETHERITE, 5.0F, -3.0F);

        extra(builder, "tin", 1, 80, 2.5F, 0.4F, 14, LibCommonTags.Items.INGOTS_TIN, 6.0F, -3.2F);
        extra(builder, "copper", 2, 150, 4.3F, 1.0F, 14, LibCommonTags.Items.INGOTS_COPPER, 7.0F, -3.2F);
        extra(builder, "silver", 3, 1456, 8.0F, 2.8F, 14, LibCommonTags.Items.INGOTS_SILVER, 6.5F, -3.2F);
        extra(builder, "aluminum", 1, 215, 5.4F, 1.5F, 10, LibCommonTags.Items.INGOTS_ALUMINUM, 6.2F, -3.1F);
        extra(builder, "nickel", 2, 230, 5.8F, 1.8F, 10, LibCommonTags.Items.INGOTS_NICKEL, 6.0F, -3.2F);
        extra(builder, "platinum", 3, 1865, 7.8F, 3.2F, 18, LibCommonTags.Items.INGOTS_PLATINUM, 6.0F, -3.0F);
        extra(builder, "lead", 2, 212, 4.2F, 1.2F, 4, LibCommonTags.Items.INGOTS_LEAD, 6.0F, -3.2F);
        extra(builder, "bronze", 2, 245, 6.0F, 2.0F, 13, LibCommonTags.Items.INGOTS_BRONZE, 6.0F, -3.1F);
        extra(builder, "electrum", 3, 213, 10.0F, 2.0F, 13, LibCommonTags.Items.INGOTS_ELECTRUM, 6.0F, -3.5F);
        extra(builder, "invar", 2, 264, 6.2F, 2.2F, 11, LibCommonTags.Items.INGOTS_INVAR, 6.0F, -3.1F);
        extra(builder, "steel", 3, 1356, 6.9F, 2.5F, 10, LibCommonTags.Items.INGOTS_STEEL, 7.0F, -3.0F);
        extra(builder, "ruby", 2, 1612, 8.4F, 3.2F, 10, LibCommonTags.Items.GEMS_RUBY, 6.0F, -3.2F);
        extra(builder, "amethyst", 2, 1532, 7.9F, 2.9F, 12, LibCommonTags.Items.GEMS_AMETHYST, 5.2F, -3.1F);
        extra(builder, "sapphire", 2, 1524, 7.8F, 2.8F, 12, LibCommonTags.Items.GEMS_SAPPHIRE, 5.2F, -3.1F);
        extra(builder, "topaz", 2, 1426, 7.6F, 2.6F, 8, LibCommonTags.Items.GEMS_TOPAZ, 5.0F, -3.0F);
        extra(builder, "emerald", 3, 1547, 8.2F, 3.0F, 14, LibCommonTags.Items.GEMS_EMERALD, 5.2F, -3.2F);
        extra(builder, "peridot", 2, 1456, 7.7F, 2.7F, 9, LibCommonTags.Items.GEMS_PERIDOT, 5.0F, -3.0F);

        builder.setup();
    }

    /** The shared tiers, making their config file on the first call. Call it while your mod is constructed. */
    public static ToolTiers get() {
        ToolTiers tiers = instance;
        if (tiers == null) {
            synchronized (ToolTiers.class) {
                tiers = instance;
                if (tiers == null) {
                    tiers = new ToolTiers();
                    instance = tiers;
                }
            }
        }
        return tiers;
    }

    /** Whether any mod has asked for the tiers, and so whether their config file exists. */
    public static boolean created() {
        return instance != null;
    }

    public List<ToolTier> vanilla() {
        return List.of(this.wood, this.stone, this.gold, this.iron, this.diamond, this.netherite);
    }

    /** Materials other mods add, such as tin or ruby, by name, in a fixed order. */
    public Map<String, ToolTier> extras() {
        return Collections.unmodifiableMap(this.extras);
    }

    public ToolTier extra(String name) {
        ToolTier tier = this.extras.get(name);
        if (tier == null) {
            throw new IllegalArgumentException("No extra tool tier named " + name);
        }
        return tier;
    }

    private void extra(IConfigurationBuilder builder, String name, int harvestLevel, int maxUses, float efficiency, float damage, int enchantability, TagKey<Item> repairItems, float axeDamage, float axeSpeed) {
        ToolMaterial defaults = new ToolMaterial(HarvestTiers.incorrectBlocksForDrops(harvestLevel), maxUses, efficiency, damage, enchantability, repairItems);
        this.extras.put(name, new ToolTier(builder, "extra_materials", name, defaults, axeDamage, axeSpeed));
    }
}
