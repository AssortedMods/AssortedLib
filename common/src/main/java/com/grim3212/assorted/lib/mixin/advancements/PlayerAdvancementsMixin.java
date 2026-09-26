package com.grim3212.assorted.lib.mixin.advancements;

import com.google.gson.JsonElement;
import com.grim3212.assorted.lib.migration.MovedIds;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Progress saved under an advancement's old id is read under its new one, where vanilla would drop it; see {@link MovedIds}. */
@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {

    @Unique
    private ServerAdvancementManager assortedlib$loading;

    @Inject(method = "load", at = @At("HEAD"))
    private void assortedlib_rememberManager(ServerAdvancementManager manager, CallbackInfo ci) {
        this.assortedlib$loading = manager;
    }

    // The file as read, handed to vanilla's codec, which parses it into a record nothing outside can build.
    @ModifyArg(method = "load", at = @At(value = "INVOKE",
            target = "Lcom/mojang/serialization/Codec;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"), index = 1)
    private Object assortedlib_carryOverMovedAdvancements(Object saved) {
        ServerAdvancementManager manager = this.assortedlib$loading;
        if (manager == null || !MovedIds.enabled() || !(saved instanceof JsonElement json)) {
            return saved;
        }
        return MovedIds.advancements(json, id -> manager.get(id) != null);
    }
}
