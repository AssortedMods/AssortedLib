package com.grim3212.assorted.lib.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/**
 * A player has died for good (no totem saved them) and is about to drop their inventory and
 * experience. Whatever a handler takes out of the inventory is not dropped. Cannot be cancelled.
 */
public class PlayerDeathDropsEvent extends GenericEvent {

    private final ServerPlayer player;
    private final DamageSource source;

    public PlayerDeathDropsEvent(ServerPlayer player, DamageSource source) {
        this.player = player;
        this.source = source;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public DamageSource getSource() {
        return source;
    }
}
