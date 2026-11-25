package com.grim3212.assorted.lib;

import com.grim3212.assorted.lib.data.AssortedLibLanguageProvider;
import com.grim3212.assorted.lib.data.AssortedLibManualProvider;
import com.grim3212.assorted.lib.client.data.LibItemModelProvider;
import com.grim3212.assorted.lib.data.LibRecipes;
import net.minecraft.core.registries.Registries;
import com.grim3212.assorted.lib.core.item.IStackTooltipProvider;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import com.grim3212.assorted.lib.conditions.LibConditions;
import com.grim3212.assorted.lib.conditions.PartToggles;
import com.grim3212.assorted.lib.family.FamilySwitches;
import com.grim3212.assorted.lib.core.block.IBlockOnPlayerBreak;
import com.grim3212.assorted.lib.core.item.LibDataComponents;
import com.grim3212.assorted.lib.data.ForgeBiomeTagProvider;
import com.grim3212.assorted.lib.data.ForgeBlockTagProvider;
import com.grim3212.assorted.lib.data.ForgeItemTagProvider;
import com.grim3212.assorted.lib.data.ForgeRegistryEntries;
import com.grim3212.assorted.lib.data.LibCommonTagProvider;
import com.grim3212.assorted.lib.events.*;
import com.grim3212.assorted.lib.platform.ForgePlatformHelper;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.worldgen.LibForgeWorldGen;
import com.grim3212.assorted.lib.worldgen.CustomSpawners;
import com.grim3212.assorted.lib.worldgen.StructureSpawns;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import com.grim3212.assorted.lib.core.item.SpawnEggDispensing;
import com.grim3212.assorted.lib.manual.ManualIngredientTags;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.ModifyCustomSpawnersEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Mod(LibConstants.MOD_ID)
public class AssortedLibForge {

    /**
     * {@code FMLJavaModLoadingContext} is gone; the mod event bus and the mod container are injected
     * into the {@code @Mod} constructor instead.
     */
    public AssortedLibForge(IEventBus modBus, ModContainer modContainer) {
        LibConstants.LOG.info(LibConstants.MOD_NAME + " starting up...");

        modBus.addListener(this::gatherData);
        modBus.addListener(this::registerIngredientTypes);
        modBus.addListener(this::registerConditionCodecs);
        // Registry events follow the construction of every mod and precede config loading, so every member has joined.
        modBus.addListener((final RegisterEvent event) -> FamilySwitches.makeAll());
        modBus.addListener(this::modifyCreativeTabs);
        modBus.addListener((final RegisterEvent event) -> event.register(Registries.CREATIVE_MODE_TAB,
                helper -> ForgePlatformHelper.creativeTabsToRegister.forEach((id, tab) -> helper.register(id, tab.get()))));
        modBus.addListener((final RegisterEvent event) -> event.register(Registries.DATA_COMPONENT_TYPE,
                helper -> ForgePlatformHelper.componentTypesToRegister.forEach((id, type) -> helper.register(id, type.get()))));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, AssortedLibForge::addComponentTooltips);
        // On join and after every /reload: the manual's tag notes, which 1.21.1's recipe sync drops.
        // Only to a client that registered the channel; NeoForge throws on a send to one that did not.
        NeoForge.EVENT_BUS.addListener((final OnDatapackSyncEvent event) -> event.getRelevantPlayers()
                .filter(player -> player.connection.hasChannel(ManualIngredientTags.PACKET_ID))
                .forEach(ManualIngredientTags::sendTo));
        modBus.addListener((final ModifyDefaultComponentsEvent event) -> ForgePlatformHelper.defaultComponents
                .forEach(entry -> event.modify(entry.getFirst().get(), patch -> applyDefaults(entry.getSecond(), patch))));
        // NeoForge 21.1 still reads fire odds from FireBlock's own tables, so they go in once every block exists.
        modBus.addListener((final FMLCommonSetupEvent event) -> event.enqueueWork(() -> ForgePlatformHelper.flammablesToRegister
                .forEach(flammable -> ((FireBlock) Blocks.FIRE).setFlammable(flammable.block().get(), flammable.igniteOdds(), flammable.burnOdds()))));
        modBus.addListener((final EntityAttributeCreationEvent event) -> ForgePlatformHelper.attributesToRegister.forEach(registration -> registration.register(event)));
        modBus.addListener((final RegisterSpawnPlacementsEvent event) -> ForgePlatformHelper.spawnPlacementsToRegister.forEach(registration -> registration.register(event)));

        // Every mod's items exist by the time a server starts.
        NeoForge.EVENT_BUS.addListener((final ServerStartingEvent event) -> SpawnEggDispensing.fillIn());

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
                final AnvilUpdatedEvent newEvent = new AnvilUpdatedEvent(event.getLeft(), event.getRight(), event.getName(), (int) Math.min(event.getCost(), Integer.MAX_VALUE), event.getPlayer());
                Services.EVENTS.handleEvents(newEvent);
                if (newEvent.isCanceled()) {
                    event.setCanceled(true);
                } else if (!newEvent.getOutput().isEmpty()) {
                    event.setOutput(newEvent.getOutput());
                    event.setCost(newEvent.getCost());
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

        // LivingDeathEvent fires at the top of die(), after any totem has had its chance.
        Services.EVENTS.registerEventType(PlayerDeathDropsEvent.class, () -> {
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (final LivingDeathEvent event) -> {
                if (event.getEntity() instanceof ServerPlayer player) {
                    Services.EVENTS.handleEvents(new PlayerDeathDropsEvent(player, event.getSource()));
                }
            });
        });

        Services.EVENTS.registerEventType(LootTableModifyEvent.class, () -> {
            NeoForge.EVENT_BUS.addListener((final LootTableLoadEvent event) -> {
                final LootTableModifyEvent newEvent = new LootTableModifyEvent(event.getTable(), event.getName(), new ForgeLootTableModificationContext(event.getTable(), event.getRegistries()), true);
                Services.EVENTS.handleEvents(newEvent);
            });
        });

        // IBlockOnPlayerBreak. BreakEvent fires at the top of the server's destroyBlock, before
        // playerWillDestroy; the client's own prediction is vetoed by MultiPlayerGameModeMixin.
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, (final BlockEvent.BreakEvent event) -> {
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
                tab.getValue().forEach(stacks -> event.acceptAll(PartToggles.visible(stacks.get())));
            }
        }
    }

    private void gatherData(final GatherDataEvent event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        event.getGenerator().addProvider(event.includeClient(), new LibItemModelProvider(packOutput, event.getExistingFileHelper()));
        event.getGenerator().addProvider(event.includeClient(), new AssortedLibLanguageProvider(packOutput));
        event.getGenerator().addProvider(event.includeClient(), new AssortedLibManualProvider(packOutput));

        ForgeRegistryEntries.of(LibConstants.MOD_ID).recipes(LibRecipes::new).addTo(event);
        ForgeBlockTagProvider blockTagProvider = event.getGenerator().addProvider(event.includeServer(), new ForgeBlockTagProvider(packOutput, lookupProvider, LibConstants.MOD_ID, event.getExistingFileHelper(), new LibCommonTagProvider.BlockTagProvider(packOutput, lookupProvider)));
        event.getGenerator().addProvider(event.includeServer(), new ForgeItemTagProvider(packOutput, lookupProvider, blockTagProvider.contentsGetter(), LibConstants.MOD_ID, event.getExistingFileHelper(), new LibCommonTagProvider.ItemTagProvider(packOutput, lookupProvider, blockTagProvider.contentsGetter())));
        event.getGenerator().addProvider(event.includeServer(), new ForgeBiomeTagProvider(packOutput, lookupProvider, LibConstants.MOD_ID, event.getExistingFileHelper(), new LibCommonTagProvider.BiomeTagProvider(packOutput, lookupProvider)));
    }

    /**
     * Vanilla only draws the tooltips of its own components; a mod's are added here, directly under
     * the name, which is where an item's own lines have always gone. Run on both sides, wherever the
     * lines are built.
     */
    private static void addComponentTooltips(final ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> lines = new ArrayList<>();
        for (var type : ForgePlatformHelper.componentTooltips) {
            IStackTooltipProvider provider = stack.get(type.get());
            if (provider != null) {
                provider.addToTooltip(event.getContext(), lines::add, event.getFlags(), stack);
            }
        }

        if (!lines.isEmpty()) {
            List<Component> tooltip = event.getToolTip();
            tooltip.addAll(Math.min(1, tooltip.size()), lines);
        }
    }

    /** Hands NeoForge's patch the components a mod set on a plain map builder, which is what the common side writes. */
    private static void applyDefaults(Consumer<DataComponentMap.Builder> defaults, DataComponentPatch.Builder patch) {
        DataComponentMap.Builder components = DataComponentMap.builder();
        defaults.accept(components);
        for (TypedDataComponent<?> component : components.build()) {
            setComponent(patch, component);
        }
    }

    private static <T> void setComponent(DataComponentPatch.Builder patch, TypedDataComponent<T> component) {
        patch.set(component.type(), component.value());
    }
}
