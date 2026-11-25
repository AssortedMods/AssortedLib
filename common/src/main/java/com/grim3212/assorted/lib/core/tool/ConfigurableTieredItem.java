package com.grim3212.assorted.lib.core.tool;

import net.minecraft.world.item.TieredItem;

/** An item with a configured tool material's durability, repair and enchantability, but no tool behaviour. */
public class ConfigurableTieredItem extends TieredItem implements ITiered {

    private final ToolTier tier;

    public ConfigurableTieredItem(ToolTier tier, Properties properties) {
        super(tier, properties);
        this.tier = tier;
    }

    @Override
    public ToolTier getToolTier() {
        return this.tier;
    }
}
