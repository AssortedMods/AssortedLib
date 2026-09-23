package com.grim3212.assorted.lib.mixin.server.level;

import com.grim3212.assorted.lib.worldgen.CustomSpawners;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CustomSpawner;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Fabric API has no ModifyCustomSpawnersEvent, so {@link CustomSpawners} are appended to the field here; vanilla's stay first. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Shadow
    @Final
    @Mutable
    private List<CustomSpawner> customSpawners;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void assortedlib_addCustomSpawners(CallbackInfo ci) {
        List<CustomSpawner> added = CustomSpawners.createFor((ServerLevel) (Object) this);
        if (!added.isEmpty()) {
            List<CustomSpawner> all = new ArrayList<>(this.customSpawners);
            all.addAll(added);
            this.customSpawners = List.copyOf(all);
        }
    }
}
