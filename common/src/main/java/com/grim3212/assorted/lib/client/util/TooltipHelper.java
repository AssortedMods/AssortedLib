package com.grim3212.assorted.lib.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Client side tooltip text. */
public final class TooltipHelper {

    /** What vanilla wraps its own standalone tooltips at, before the screen is taken into account. */
    private static final int MIN_WRAP_WIDTH = 200;

    private TooltipHelper() {
        throw new IllegalStateException("Can not instantiate an instance of: TooltipHelper. This is a utility class");
    }

    /**
     * Splits a tooltip line: a tooltip draws one line per {@link Component}, and NeoForge only
     * splits one that runs off the screen.
     */
    public static List<Component> wrap(Component line) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = Math.max(minecraft.getWindow().getGuiScaledWidth() / 2, MIN_WRAP_WIDTH);
        List<FormattedText> split = minecraft.font.getSplitter().splitLines(line, width, Style.EMPTY);
        if (split.size() <= 1) {
            return List.of(line);
        }

        List<Component> lines = new ArrayList<>(split.size());
        for (FormattedText part : split) {
            lines.add(rebuild(part));
        }
        return lines;
    }

    /** A split line comes back as {@link FormattedText}, which the tooltip cannot take. */
    private static MutableComponent rebuild(FormattedText text) {
        MutableComponent rebuilt = Component.empty();
        text.visit((style, part) -> {
            rebuilt.append(Component.literal(part).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return rebuilt;
    }
}
