package com.grim3212.assorted.lib.mixin.server.level;

import com.grim3212.assorted.lib.events.PlayerDeathDropsEvent;
import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Raises {@link PlayerDeathDropsEvent} where NeoForge's LivingDeathEvent fires: past the totem, before the drops. */
@Mixin(ServerPlayer.class)
public class ServerPlayerDeathMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void assortedlib_beforeDeathDrops(DamageSource source, CallbackInfo ci) {
        Services.EVENTS.handleEvents(new PlayerDeathDropsEvent((ServerPlayer) (Object) this, source));
    }
}
