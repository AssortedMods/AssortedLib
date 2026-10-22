package com.grim3212.assorted.lib.client.manual;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.client.manual.screen.ManualScreen;
import com.grim3212.assorted.lib.conditions.DisplayConditions;
import com.grim3212.assorted.lib.crafting.SyncedRecipes;
import com.grim3212.assorted.lib.config.LibClientConfig;
import com.grim3212.assorted.lib.manual.ManualPageRef;
import com.grim3212.assorted.lib.platform.ClientServices;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/** Client side setup for the manual, and the way in from the item. */
public final class ManualClient {

    public static final LibClientConfig CLIENT_CONFIG = new LibClientConfig();

    private ManualClient() {
    }

    public static void init() {
        ManualPageTypes.bootstrap();
        DisplayConditions.bootstrap();

        ClientServices.CLIENT.addReloadListener(ManualLoader.ID, new ManualLoader());
        ClientServices.CLIENT.registerHudElement(ManualHud.ID, ManualHud::extract);
        // Recipes arrive after the join refresh, and a recipe_exists page waits on them.
        SyncedRecipes.addUpdateListener(ManualContent::refresh);
    }

    /** Both loaders call this when the server sends its data: on join, and on every {@code /reload}. */
    public static void refreshConditions() {
        ManualContent.refresh();
    }

    /** Read per frame rather than cached: the config is reloadable. */
    static boolean showPageIndicator() {
        return CLIENT_CONFIG.showManualPageIndicator.get();
    }

    /** For the item's tooltip. */
    public static int sectionCount() {
        return ManualContent.get().sections().size();
    }

    /** Opens at {@code ref} when it resolves, otherwise where the book was last left. */
    public static void open(@Nullable ManualPageRef ref) {
        Minecraft.getInstance().gui.setScreen(ManualScreen.openedAt(ref));
    }

    /** Raytraces here because a non living entity is only known to the client. */
    public static void openAtLookedAt() {
        open(ManualTarget.lookedAtPage());
    }
}
