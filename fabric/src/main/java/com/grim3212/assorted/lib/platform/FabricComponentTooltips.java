package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.core.item.IStackTooltipProvider;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Draws the lines of every component handed to {@code IPlatformHelper#showComponentTooltip}. Fabric's
 * tooltip event fires once vanilla has written every line, so the lines are put back right under the
 * item's name, ahead of vanilla's own component lines, the way NeoForge places them.
 */
final class FabricComponentTooltips {

    private static final List<DataComponentType<? extends IStackTooltipProvider>> TYPES = new CopyOnWriteArrayList<>();
    private static boolean registered;

    private FabricComponentTooltips() {
    }

    static synchronized void show(DataComponentType<? extends IStackTooltipProvider> type) {
        if (!TYPES.contains(type)) {
            TYPES.add(type);
        }

        if (!registered) {
            registered = true;
            ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
                List<Component> added = new ArrayList<>();
                for (DataComponentType<? extends IStackTooltipProvider> shown : TYPES) {
                    IStackTooltipProvider provider = stack.get(shown);
                    if (provider != null) {
                        provider.addToTooltip(context, added::add, flag, stack);
                    }
                }

                if (!added.isEmpty()) {
                    lines.addAll(Math.min(1, lines.size()), added);
                }
            });
        }
    }
}
