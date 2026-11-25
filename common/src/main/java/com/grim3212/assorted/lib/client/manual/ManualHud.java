package com.grim3212.assorted.lib.client.manual;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.manual.InstructionManualItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

/**
 * The check mark beside the crosshair, marking that what is under it has a page. Drawn only while
 * the manual is in hand, so it says "this will open something" rather than labelling the world.
 */
public final class ManualHud {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LibConstants.MOD_ID, "manual_page_available");

    /** Clear of the 15 wide crosshair, on the side the book's own icon sits on. */
    private static final int OFFSET_X = 10;
    private static final int SIZE = 8;

    private ManualHud() {
    }

    static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        if (!minecraft.options.getCameraType().isFirstPerson() || !ManualClient.showPageIndicator()) {
            return;
        }
        if (!holdingManual(player)) {
            return;
        }

        if (!ManualTarget.hasLoadedPage(ManualTarget.lookedAtPage())) {
            return;
        }

        graphics.blitSprite(ManualBook.Sprites.PAGE_AVAILABLE,
                graphics.guiWidth() / 2 + OFFSET_X, (graphics.guiHeight() - SIZE) / 2, SIZE, SIZE);
    }

    private static boolean holdingManual(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof InstructionManualItem) {
                return true;
            }
        }

        return false;
    }
}
