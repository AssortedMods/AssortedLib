package com.grim3212.assorted.lib.client.events;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

/** Something drawn on the HUD, after the crosshair. Both loaders hand a HUD element the same pair. */
@FunctionalInterface
public interface HudElementHandler {
    void render(GuiGraphics graphics, DeltaTracker deltaTracker);
}
