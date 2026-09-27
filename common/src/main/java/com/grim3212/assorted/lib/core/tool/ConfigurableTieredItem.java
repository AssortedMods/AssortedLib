package com.grim3212.assorted.lib.core.tool;

import net.minecraft.world.item.Item;

/** An item with a configured tool material's durability and repair, but no tool behaviour. */
public class ConfigurableTieredItem extends Item implements ITiered {

    private final ToolTier tier;

    public ConfigurableTieredItem(ToolTier tier, Properties properties) {
        super(tier.tiered(properties));
        this.tier = tier;
    }

    @Override
    public ToolTier getToolTier() {
        return this.tier;
    }
}
