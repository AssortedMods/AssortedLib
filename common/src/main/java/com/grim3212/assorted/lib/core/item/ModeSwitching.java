package com.grim3212.assorted.lib.core.item;

import com.grim3212.assorted.lib.LibConstants;
import com.grim3212.assorted.lib.core.network.CycleModePacket;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.platform.services.INetworkHelper;
import net.minecraft.resources.Identifier;

/**
 * The packet behind the switch-modes key, shared by every mod with an {@link ISwitchModes} item. Nothing is
 * registered until one calls {@link #enable()}, and several calling it still register it once.
 */
public final class ModeSwitching {

    public static final Identifier PACKET_ID = Identifier.fromNamespaceAndPath(LibConstants.MOD_ID, "cycle_mode");

    private static boolean enabled;

    private ModeSwitching() {
    }

    /** Call while your mod is constructed, on both sides; the client also calls {@code ModeSwitchKey.enable()}. */
    public static synchronized void enable() {
        if (enabled) {
            return;
        }
        enabled = true;
        Services.NETWORK.register(new INetworkHelper.MessageHandler<>(PACKET_ID, CycleModePacket.class, CycleModePacket::encode, CycleModePacket::decode, CycleModePacket::handle, INetworkHelper.MessageBoundSide.SERVER));
    }

    public static synchronized boolean enabled() {
        return enabled;
    }
}
