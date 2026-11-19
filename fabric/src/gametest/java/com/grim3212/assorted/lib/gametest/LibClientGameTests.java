package com.grim3212.assorted.lib.gametest;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.client.util.TooltipHelper;
import com.grim3212.assorted.lib.core.item.ItemDescription;
import com.grim3212.assorted.lib.test.ClientModelCheck;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The client half of the library. Run with {@code ./gradlew :fabric:runClientGameTest}; it exits
 * non-zero on a failure.
 */
public class LibClientGameTests implements FabricClientGameTest {

    private static final String LONG_LINE = "A fragment of a powerful tool from an ancient civilization";
    // Runes nothing else draws, so the font still has to bake their glyphs.
    private static final String UNDRAWN_LINE = "ᚠᚢᚦᚨᚱᚲ ᚷᚹᚺᚾᛁᛃ ᛇᛈᛉᛊᛏᛒ ᛖᛗᛚᛜᛞᛟ";

    @Override
    public void runTest(ClientGameTestContext context) {
        descriptionsBuildOffTheRenderThread();

        // No world: the font and the window are all any of this needs.
        context.runOnClient(client -> {
            tooltipLinesWrapToTheScreen();
            shortTooltipLinesAreLeftAlone();
        });

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            ManualClientTests.run(context);
            List<String> models = context.computeOnClient(client -> ClientModelCheck.problems(LibConstants.MOD_ID));
            if (!models.isEmpty()) {
                throw new AssertionError("Lib's own art does not draw:\n  " + String.join("\n  ", models));
            }
        }
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

    /** The creative search builds every tooltip on a worker thread, where the font can not bake glyphs. */
    private static void descriptionsBuildOffTheRenderThread() {
        ItemDescription description = new ItemDescription(Component.literal(UNDRAWN_LINE));
        List<Component> lines = CompletableFuture.supplyAsync(() -> {
            List<Component> built = new ArrayList<>();
            description.addToTooltip(Item.TooltipContext.EMPTY, built::add, TooltipFlag.NORMAL, DataComponentMap.EMPTY);
            return built;
        }).join();

        String text = String.join(" ", lines.stream().map(Component::getString).toList());
        if (!text.equals(UNDRAWN_LINE)) {
            throw new AssertionError("a description built off the render thread reads as \"" + text + "\"");
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
