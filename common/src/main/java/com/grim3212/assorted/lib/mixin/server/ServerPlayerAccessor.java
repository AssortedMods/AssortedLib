package com.grim3212.assorted.lib.mixin.server;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Ends a joined test player's spawn protection, so a test can hurt it on its first tick. */
@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor {
    @Accessor("spawnInvulnerableTime")
    void assortedlib_setSpawnInvulnerableTime(int ticks);
}
