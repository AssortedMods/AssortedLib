package com.grim3212.assorted.lib.mixin.advancements;

import com.grim3212.assorted.lib.migration.AdvancementIcons;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

/** Only swaps icons: see {@link AdvancementIcons}. */
@Mixin(ServerAdvancementManager.class)
public class ServerAdvancementManagerMixin {

    // The Map overload by descriptor, not the bridge method the generic reload listener adds.
    @ModifyVariable(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true)
    private Map<Identifier, Advancement> assortedlib_pickIcons(Map<Identifier, Advancement> advancements) {
        return AdvancementIcons.apply(advancements);
    }
}
