package com.grim3212.assorted.lib.manual;

import com.grim3212.assorted.lib.client.manual.ManualClient;
import com.grim3212.assorted.lib.core.item.IStackTooltipProvider;
import com.grim3212.assorted.lib.dist.Dist;
import com.grim3212.assorted.lib.dist.DistExecutor;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

/**
 * The manual's "N sections" line. Carries no data: the count is read from the loaded book each time
 * the tooltip is drawn, and is 0 where there is no client book, such as a dedicated server.
 */
public record ManualSectionsTooltip() implements IStackTooltipProvider {

    public static final ManualSectionsTooltip INSTANCE = new ManualSectionsTooltip();
    public static final Codec<ManualSectionsTooltip> CODEC = Codec.unit(INSTANCE);
    public static final StreamCodec<ByteBuf, ManualSectionsTooltip> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag, ItemStack stack) {
        Integer sections = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> ManualClient::sectionCount);
        tooltip.accept(Component.translatable("gui.assortedlib.manual.sections", sections == null ? 0 : sections).withStyle(ChatFormatting.GRAY));
    }
}
