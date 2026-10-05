package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.events.AnvilUpdatedEvent;
import com.grim3212.assorted.lib.events.EntityInteractEvent;
import com.grim3212.assorted.lib.events.FabricLootTableModificationContext;
import com.grim3212.assorted.lib.events.LootTableModifyEvent;
import com.grim3212.assorted.lib.events.PlayerDeathDropsEvent;
import com.grim3212.assorted.lib.events.UseBlockEvent;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Fabric does not guarantee that AssortedLib initialises before the mods using it, so the hooks are
 * registered here, when the service is first loaded, rather than from the mod initializer. A hook
 * only subscribes to its Fabric event once a handler for it is registered.
 */
public class FabricEventHelper extends EventHelperBase {

    public FabricEventHelper() {
        this.registerEventType(UseBlockEvent.class, () -> UseBlockCallback.EVENT.register((Player player, Level level, InteractionHand hand, BlockHitResult hitResult) -> {
            final UseBlockEvent event = new UseBlockEvent(player, level, hand, hitResult);
            this.handleEvents(event);
            return event.outcome().orElse(InteractionResult.PASS);
        }));

        // Loot API v3 hands over the table's key and its builder rather than the loaded table itself -
        // there is no loot manager to look the table up in any more - so the event's table is built from
        // the builder as it stands before this modification runs.
        this.registerEventType(LootTableModifyEvent.class, () -> LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            final LootTableModifyEvent event = new LootTableModifyEvent(tableBuilder.build(), key.identifier(), new FabricLootTableModificationContext(tableBuilder), source.isBuiltin());
            this.handleEvents(event);
        }));

        // Raised directly by this module's mixins - AnvilMenuMixin, and PlayerMixin plus
        // MultiPlayerGameModeWorldlyBlockMixin for the two sides of an entity interaction - so
        // there is no Fabric event to subscribe to.
        this.registerEventType(AnvilUpdatedEvent.class, () -> {
        });
        // ServerPlayerDeathMixin: Fabric's AFTER_DEATH comes after the drops and ALLOW_DEATH before the totem.
        this.registerEventType(PlayerDeathDropsEvent.class, () -> {
        });
        this.registerEventType(EntityInteractEvent.class, () -> {
        });
    }
}
