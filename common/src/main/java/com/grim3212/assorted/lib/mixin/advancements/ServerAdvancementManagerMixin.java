package com.grim3212.assorted.lib.mixin.advancements;

import com.grim3212.assorted.lib.migration.AdvancementIcons;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


/** Only swaps icons: see {@link AdvancementIcons}. */
@Mixin(ServerAdvancementManager.class)
public class ServerAdvancementManagerMixin {

    // The lambda apply() runs for each advancement json wraps it in its holder. Mojang's mappings do not
    // name lambdas, so it is lambda$apply$0 on NeoForge and keeps its intermediary name on Fabric; both
    // are listed, unremapped, and each loader finds its own.
    @ModifyArg(method = {"lambda$apply$0", "method_20723"}, remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementHolder;<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/advancements/Advancement;)V", remap = true), index = 1)
    private Advancement assortedlib_pickIcon(ResourceLocation id, Advancement advancement) {
        return AdvancementIcons.apply(id, advancement);
    }
}
