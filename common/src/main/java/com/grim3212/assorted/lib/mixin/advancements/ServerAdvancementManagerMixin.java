package com.grim3212.assorted.lib.mixin.advancements;

import com.grim3212.assorted.lib.migration.AdvancementIcons;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


/** Only swaps icons: see {@link AdvancementIcons}. */
@Mixin(ServerAdvancementManager.class)
public class ServerAdvancementManagerMixin {

    // Advancements are registry entries in 26.3; the constructor's forEach lambda wraps each in a holder.
    @ModifyArg(method = "lambda$new$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementHolder;<init>(Lnet/minecraft/resources/Identifier;Lnet/minecraft/advancements/Advancement;)V"), index = 1)
    private static Advancement assortedlib_pickIcon(Identifier id, Advancement advancement) {
        return AdvancementIcons.apply(id, advancement);
    }
}
