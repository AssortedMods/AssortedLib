package com.grim3212.assorted.lib;

import com.grim3212.assorted.lib.core.item.IItemEnchantmentCondition;
import com.grim3212.assorted.lib.core.item.LibDataComponents;
import com.grim3212.assorted.lib.family.FamilySwitches;
import com.grim3212.assorted.lib.platform.FabricConfigHelper;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.manual.ManualIngredientTags;
import net.fabricmc.api.ModInitializer;
import com.grim3212.assorted.lib.core.item.SpawnEggDispensing;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.util.TriState;

public class AssortedLibFabric implements ModInitializer {


    @Override
    public void onInitialize() {
        LibConstants.LOG.info(LibConstants.MOD_NAME + " starting up...");

        Services.CONDITIONS.init();
        LibDataComponents.init();
        Services.INGREDIENTS.register();

        FabricConfigHelper.init();
        FamilySwitches.makeOnFirstRead();

        LibCommonSetup.init();

        // Every mod's items exist by the time a server starts.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> SpawnEggDispensing.fillIn());

        // On join and after every /reload: the manual's tag notes, which 1.21.1's recipe sync drops.
        // Only to a client that registered the channel, as on NeoForge.
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            if (ServerPlayNetworking.canSend(player, ManualIngredientTags.PACKET_ID)) {
                ManualIngredientTags.sendTo(player);
            }
        });

        // On NeoForge an IItemEnchantmentCondition's methods are IItemExtension overrides already;
        // Fabric asks through this event at the same four places - table, anvil, loot, /enchant.
        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, stack, context) -> {
            if (stack.getItem() instanceof IItemEnchantmentCondition condition) {
                return TriState.of(context == EnchantingContext.PRIMARY ? condition.isPrimaryItemFor(stack, enchantment) : condition.supportsEnchantment(stack, enchantment));
            }

            return TriState.DEFAULT;
        });
    }

}
