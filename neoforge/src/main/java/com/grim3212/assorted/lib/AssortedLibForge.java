package com.grim3212.assorted.lib;

import com.grim3212.assorted.lib.data.AssortedLibLanguageProvider;
import com.grim3212.assorted.lib.data.AssortedLibManualProvider;
import com.grim3212.assorted.lib.client.data.LibItemModelProvider;
import com.grim3212.assorted.lib.data.LibRecipes;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.common.tooltip.TooltipAppender;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;
import com.grim3212.assorted.lib.conditions.LibConditions;
import com.grim3212.assorted.lib.core.block.IBlockOnPlayerBreak;
import com.grim3212.assorted.lib.core.item.LibDataComponents;
import com.grim3212.assorted.lib.data.ForgeBiomeTagProvider;
import com.grim3212.assorted.lib.data.ForgeBlockTagProvider;
import com.grim3212.assorted.lib.data.ForgeItemTagProvider;
import com.grim3212.assorted.lib.data.LibCommonTagProvider;
import com.grim3212.assorted.lib.events.*;
import com.grim3212.assorted.lib.platform.ForgePlatformHelper;
import com.grim3212.assorted.lib.platform.ForgeRecipeSyncHelper;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.worldgen.LibForgeWorldGen;
import com.grim3212.assorted.lib.worldgen.CustomSpawners;
import com.grim3212.assorted.lib.worldgen.StructureSpawns;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.ModifyCustomSpawnersEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.concurrent.CompletableFuture;

@Mod(LibConstants.MOD_ID)
public class AssortedLibForge {

    /**
     * {@code FMLJavaModLoadingContext} is gone; the mod event bus and the mod container are injected
     * into the {@code @Mod} constructor instead.
     */
    public AssortedLibForge(IEventBus modBus, ModContainer modContainer) {
        LibConstants.LOG.info(LibConstants.MOD_NAME + " starting up...");

        modBus.addListener(this::gatherData);
        modBus.addListener(this::gatherClientData);
        modBus.addListener(this::registerIngredientTypes);
        modBus.addListener(this::registerConditionCodecs);
        modBus.addListener(this::modifyCreativeTabs);
        modBus.addListener(this::registerComponentTooltips);
        modBus.addListener((final EntityAttributeCreationEvent event) -> ForgePlatformHelper.attributesToRegister.forEach(registration -> registration.register(event)));
        modBus.addListener((final RegisterSpawnPlacementsEvent event) -> ForgePlatformHelper.spawnPlacementsToRegister.forEach(registration -> registration.register(event)));

        // Recipes are not sent to clients by default; anything that opted a type into
        // SyncedRecipes is asked for here, while the datapack is being synced.
        NeoForge.EVENT_BUS.addListener((final OnDatapackSyncEvent event) -> event.sendRecipes(ForgeRecipeSyncHelper.requestedTypes()));
        // Fabric appends the same additions from NaturalSpawnerMixin.
        NeoForge.EVENT_BUS.addListener((final LevelEvent.PotentialSpawns event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                StructureSpawns.at(level.structureManager(), event.getMobCategory(), event.getPos()).forEach(event::addSpawnerData);
            }
        });

        // Fabric appends the same spawners from ServerLevelMixin.
        NeoForge.EVENT_BUS.addListener((final ModifyCustomSpawnersEvent event) -> CustomSpawners.createFor(event.getLevel()).forEach(event::addCustomSpawner));

        Services.EVENTS.registerEventType(UseBlockEvent.class, () -> {
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, (final PlayerInteractEvent.RightClickBlock event) -> {
                final UseBlockEvent newEvent = new UseBlockEvent(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
                Services.EVENTS.handleEvents(newEvent);
                // A handler's result has to end the click here, as returning it from Fabric's
                // UseBlockCallback does; an uncancelled RightClickBlock lets vanilla carry on and
                // use the block too, after the handler has already acted on it.
                newEvent.outcome().ifPresent(result -> {
                    event.setCancellationResult(result);
                    event.setCanceled(true);
                });
            });
        });

        Services.EVENTS.registerEventType(AnvilUpdatedEvent.class, () -> {
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, (final AnvilUpdateEvent event) -> {
                final AnvilUpdatedEvent newEvent = new AnvilUpdatedEvent(event.getLeft(), event.getRight(), event.getName(), event.getXpCost(), event.getPlayer());
                Services.EVENTS.handleEvents(newEvent);
                if (newEvent.isCanceled()) {
                    event.setCanceled(true);
                } else if (!newEvent.getOutput().isEmpty()) {
                    event.setOutput(newEvent.getOutput());
                    event.setXpCost(newEvent.getCost());
                    event.setMaterialCost(newEvent.getMaterialCost());
                }
            });
        });

        Services.EVENTS.registerEventType(EntityInteractEvent.class, () -> {
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, (final PlayerInteractEvent.EntityInteract event) -> {
                final EntityInteractEvent newEvent = new EntityInteractEvent(event.getEntity(), event.getHand(), event.getTarget());
                Services.EVENTS.handleEvents(newEvent);
                event.setCanceled(newEvent.isCanceled());
                event.setCancellationResult(newEvent.getInteractionResult());
            });
        });

        Services.EVENTS.registerEventType(LootTableModifyEvent.class, () -> {
            NeoForge.EVENT_BUS.addListener((final LootTableLoadEvent event) -> {
                final LootTableModifyEvent newEvent = new LootTableModifyEvent(event.getTable(), event.getName(), new ForgeLootTableModificationContext(event.getTable()), true);
                Services.EVENTS.handleEvents(newEvent);
            });
        });

        // IBlockOnPlayerBreak. BreakBlockEvent fires at the top of destroyBlock on both sides,
        // before playerWillDestroy spawns any break particles.
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, (final BreakBlockEvent event) -> {
            if (event.isCanceled() || !(event.getLevel() instanceof Level level)) {
                return;
            }

            if (event.getState().getBlock() instanceof IBlockOnPlayerBreak onPlayerBreak && !onPlayerBreak.canPlayerBreak(event.getState(), level, event.getPos(), event.getPlayer())) {
                event.setCanceled(true);
            }
        });

        // After the event types above: this registers handlers for them, and a handler that arrives
        // first is reported as one nothing raises.
        LibCommonSetup.init();

        Services.CONDITIONS.init();
        LibDataComponents.init();

        LibForgeWorldGen.init(modBus);
    }

    /**
     * Custom ingredients are no longer recipe serializers; they live in their own
     * {@code NeoForgeRegistries.INGREDIENT_TYPES} registry.
     */
    private void registerIngredientTypes(final RegisterEvent event) {
        if (event.getRegistryKey().equals(NeoForgeRegistries.Keys.INGREDIENT_TYPES)) {
            Services.INGREDIENTS.register();
        }
    }

    /**
     * Condition types are {@code MapCodec}s in their own registry now, so they can only be registered
     * from a {@link RegisterEvent} rather than from {@code IConditionHelper#init()}.
     */
    private void registerConditionCodecs(final RegisterEvent event) {
        LibConditions.registerCodecs(event);
    }

    private void modifyCreativeTabs(final BuildCreativeModeTabContentsEvent event) {
        for (var tab : ForgePlatformHelper.tabsToRegister.entrySet()) {
            if (event.getTabKey() == tab.getKey()) {
                tab.getValue().forEach(stacks -> event.acceptAll(stacks.get()));
            }
        }
    }

    /**
     * {@code ExistingFileHelper} was removed from datagen, and the event owns the provider list now
     * ({@code addProvider} instead of {@code DataGenerator#addProvider(boolean, provider)}), so the
     * include flags are gone as well - the server and client halves are separate events.
     */
    private void gatherClientData(final GatherDataEvent.Client event) {
        PackOutput clientOutput = event.getGenerator().getPackOutput();
        event.addProvider(new LibItemModelProvider(clientOutput));
        event.addProvider(new AssortedLibLanguageProvider(clientOutput));
        event.addProvider(new AssortedLibManualProvider(clientOutput));
    }

    private void gatherData(final GatherDataEvent.Server event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        event.addProvider(new LibRecipes.Runner(packOutput, lookupProvider));
        ForgeBlockTagProvider blockTagProvider = event.addProvider(new ForgeBlockTagProvider(packOutput, lookupProvider, LibConstants.MOD_ID, new LibCommonTagProvider.BlockTagProvider(packOutput, lookupProvider)));
        event.addProvider(new ForgeItemTagProvider(packOutput, lookupProvider, blockTagProvider.contentsGetter(), LibConstants.MOD_ID, new LibCommonTagProvider.ItemTagProvider(packOutput, lookupProvider, blockTagProvider.contentsGetter())));
        event.addProvider(new ForgeBiomeTagProvider(packOutput, lookupProvider, LibConstants.MOD_ID, new LibCommonTagProvider.BiomeTagProvider(packOutput, lookupProvider)));
    }

    /**
     * Vanilla only draws the tooltips of its own components; a mod's are added here, ahead of
     * vanilla's lines, which is where an item's own lines have always gone.
     */
    private void registerComponentTooltips(final RegisterTooltipAppendersEvent event) {
        ForgePlatformHelper.componentTooltips.forEach(type -> addComponentTooltip(event, type.get()));
    }

    private static <T extends TooltipProvider> void addComponentTooltip(RegisterTooltipAppendersEvent event, DataComponentType<T> type) {
        event.registerComponentAppenderBeforeAll(type, TooltipAppender.createComponentAppender(type));
    }
}
