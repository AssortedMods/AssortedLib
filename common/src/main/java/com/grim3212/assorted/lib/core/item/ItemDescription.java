package com.grim3212.assorted.lib.core.item;

import com.grim3212.assorted.lib.client.util.TooltipHelper;
import com.grim3212.assorted.lib.dist.Dist;
import com.grim3212.assorted.lib.dist.DistExecutor;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.List;
import java.util.function.Consumer;

/**
 * A fixed line under an item's name, for an item whose tooltip does not depend on the stack: set it
 * as a default component ({@link LibDataComponents#DESCRIPTION}) in the item's properties.
 *
 * @param line the line, styled as it is drawn
 */
public record ItemDescription(Component line) implements TooltipProvider {

    public static final Codec<ItemDescription> CODEC = ComponentSerialization.CODEC.xmap(ItemDescription::new, ItemDescription::line);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemDescription> STREAM_CODEC = ComponentSerialization.STREAM_CODEC.map(ItemDescription::new, ItemDescription::line);

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag, DataComponentGetter components) {
        // Wrapped here or not at all: a tooltip draws one line per component, however long it is.
        List<Component> wrapped = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> TooltipHelper.wrap(this.line));
        if (wrapped == null) {
            tooltip.accept(this.line);
            return;
        }

        wrapped.forEach(tooltip);
    }
}
