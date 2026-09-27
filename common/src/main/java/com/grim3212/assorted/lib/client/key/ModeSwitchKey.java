package com.grim3212.assorted.lib.client.key;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.core.item.ISwitchModes;
import com.grim3212.assorted.lib.core.network.CycleModePacket;
import com.grim3212.assorted.lib.platform.ClientServices;
import com.grim3212.assorted.lib.platform.Services;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/**
 * The switch-modes key, Z by default, which cycles the {@link ISwitchModes} item in either hand. It only
 * appears in Controls once a mod calls {@link #enable()}, and several calling it still make one key.
 */
public final class ModeSwitchKey {

    // Its label is "key.category.assortedlib.general" in the lang file.
    public static final Identifier CATEGORY = Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, "general");

    private static KeyMapping key;
    // Client only, so one cooldown per player.
    private static int cooldown;

    private ModeSwitchKey() {
    }

    /** Call from your mod's client setup, beside {@code ModeSwitching.enable()}. */
    public static synchronized void enable() {
        if (key != null) {
            return;
        }
        key = ClientServices.KEYBINDS.createNew("key.assortedlib.switch_modes", ClientServices.KEYBINDS.getInGameKeyConflictContext(), InputConstants.Type.KEYSYM, InputConstants.KEY_Z, CATEGORY);
        ClientServices.CLIENT.registerKeyMapping(key);
        ClientServices.CLIENT.registerClientTickStart(ModeSwitchKey::tick);
    }

    /** The key, or null while no mod has enabled it. */
    public static synchronized KeyMapping key() {
        return key;
    }

    private static void tick(Minecraft mc) {
        if (cooldown > 0) {
            --cooldown;
        }

        Player player = mc.player;
        if (player == null || !mc.isWindowActive() || !key.consumeClick() || cooldown > 0) {
            return;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof ISwitchModes) {
                cooldown = 10;
                Services.NETWORK.sendToServer(new CycleModePacket(hand));
            }
        }
    }
}
