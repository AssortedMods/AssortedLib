package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.client.util.TooltipHelper;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;
import java.util.Optional;

/**
 * The client half of the library. Run with {@code ./gradlew :fabric:runClientGameTest}; it exits
 * non-zero on a failure.
 */
public class LibClientGameTests implements FabricClientGameTest {

    private static final String LONG_LINE = "A fragment of a powerful tool from an ancient civilization";

    @Override
    public void runTest(ClientGameTestContext context) {
        // No world: the font and the window are all any of this needs.
        context.runOnClient(client -> {
            tooltipLinesWrapToTheScreen();
            shortTooltipLinesAreLeftAlone();
        });
    }

    /** A line too wide to draw is split into several that read as the one line again. */
    private static void tooltipLinesWrapToTheScreen() {
        List<Component> wrapped = TooltipHelper.wrap(Component.literal(LONG_LINE).withStyle(ChatFormatting.GRAY));
        if (wrapped.size() < 2) {
            throw new AssertionError("a tooltip line of " + LONG_LINE.length() + " characters did not wrap: " + wrapped);
        }

        String rejoined = String.join(" ", wrapped.stream().map(Component::getString).toList());
        if (!rejoined.equals(LONG_LINE)) {
            throw new AssertionError("a wrapped tooltip line reads as \"" + rejoined + "\"");
        }

        // The style has to survive the split, or every wrapped line draws white.
        TextColor gray = Style.EMPTY.withColor(ChatFormatting.GRAY).getColor();
        for (Component line : wrapped) {
            line.visit((style, part) -> {
                if (!part.isEmpty() && !gray.equals(style.getColor())) {
                    throw new AssertionError("a wrapped tooltip line lost its style: " + line);
                }
                return Optional.empty();
            }, Style.EMPTY);
        }
    }

    /** A line that fits is passed straight through, keeping its translation key for the tooltip. */
    private static void shortTooltipLinesAreLeftAlone() {
        Component line = Component.translatable("gui.assortedlib.manual.sections", 3);
        List<Component> wrapped = TooltipHelper.wrap(line);
        if (!wrapped.equals(List.of(line))) {
            throw new AssertionError("a short tooltip line was rebuilt: " + wrapped);
        }
    }
}
