package com.grim3212.assorted.lib.core.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

/**
 * A data component that adds tooltip lines to the stack carrying it. Vanilla's own
 * {@code TooltipProvider} is only asked for vanilla's components and is not handed the stack, so a
 * modded component implements this instead and is shown through
 * {@code IPlatformHelper#showComponentTooltip}, which each loader runs from its tooltip event.
 */
public interface IStackTooltipProvider {

    /**
     * @param stack the stack the component is on, for lines that read its other components
     */
    void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag, ItemStack stack);
}
