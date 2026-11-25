package com.grim3212.assorted.lib.platform;

import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.grim3212.assorted.lib.client.events.ClientTickHandler;
import com.grim3212.assorted.lib.client.events.HudElementHandler;
import com.grim3212.assorted.lib.client.events.LevelSubmitHandler;
import com.grim3212.assorted.lib.client.model.loader.ForgePlatformModelLoaderPlatformDelegate;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecificationLoader;
import com.grim3212.assorted.lib.client.render.ISpecialModelRendererRegistry;
import com.grim3212.assorted.lib.client.screen.LibScreenFactory;
import com.grim3212.assorted.lib.platform.services.IClientHelper;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class ForgeClientHelper implements IClientHelper {

    private static final Map<String, Registrations> registrationsMap = Maps.newConcurrentMap();

    @Override
    public <T extends AbstractContainerMenu, S extends Screen & MenuAccess<T>> void registerScreen(Supplier<MenuType<? extends T>> menuType, LibScreenFactory<T, S> factory) {
        getRegistration().menuTypes.put(menuType::get, factory);
    }

    @Override
    public void addReloadListener(ResourceLocation identifier, PreparableReloadListener reloadListener) {
        getRegistration().clientReloadListeners.put(identifier, reloadListener);
    }

    @Override
    public void registerSpecialModelRenderers(final Consumer<ISpecialModelRendererRegistry> register) {
        getRegistration().specialModelRendererInitializers.add(register);
    }

    @Override
    public <E extends BlockEntity> void registerBlockEntityRenderer(Supplier<? extends BlockEntityType<? extends E>> entityType, BlockEntityRendererProvider<E> entityRendererFactory) {
        getRegistration().blockEntityRenderers.put((Supplier<BlockEntityType<?>>) (Supplier<? extends BlockEntityType<?>>) entityType, entityRendererFactory);
    }

    @Override
    public <E extends Entity> void registerEntityRenderer(Supplier<? extends EntityType<? extends E>> entityType, EntityRendererProvider<E> entityRendererFactory) {
        getRegistration().entityRenderers.put((Supplier<EntityType<?>>) (Supplier<? extends EntityType<?>>) entityType, entityRendererFactory);
    }

    @Override
    public void registerEntityLayer(ModelLayerLocation modelLayerLocation, Supplier<LayerDefinition> layerDefinition) {
        getRegistration().entityLayers.put(modelLayerLocation, layerDefinition);
    }

    @Override
    public void registerBlockColor(BlockColor color, Supplier<List<Block>> blocks) {
        getRegistration().blockColors.put(color, blocks);
    }

    /** NeoForge takes a block's layer from the {@code "render_type"} in its model json. */
    @Override
    public void registerBlockRenderType(RenderType renderType, Supplier<List<Block>> blocks) {
    }

    @Override
    public void registerItemColor(ItemColor color, Supplier<List<? extends ItemLike>> items) {
        getRegistration().itemColors.put(color, items);
    }

    @Override
    public BlockColors getBlockColors() {
        return Minecraft.getInstance().getBlockColors();
    }

    @Override
    public void registerModelLoader(ResourceLocation name, IModelSpecificationLoader<?> modelLoader) {
        getRegistration().modelLoaders.put(name, modelLoader);
    }

    @Override
    public void registerItemProperty(Supplier<? extends Item> item, ResourceLocation id, ClampedItemPropertyFunction property) {
        getRegistration().itemProperties.add(Pair.of(item, Pair.of(id, property)));
    }

    /**
     * A model json backed by a specification bakes through its NeoForge geometry, which already
     * wraps the result in the data aware delegate, so every model bakes the same way. The json may
     * only have been loaded on demand just now, so its parents are resolved first; the bakery only
     * does that for the models it knew of up front.
     */
    @Override
    public BakedModel bakeSpecificationModel(ModelBaker baker, ResourceLocation modelLocation, ModelState modelState) {
        baker.getModel(modelLocation).resolveParents(baker::getModel);
        return ClientServices.MODELS.adaptToPlatform(baker.bake(modelLocation, modelState, baker.getModelTextureGetter()));
    }

    @Override
    public void registerKeyMapping(KeyMapping keyMapping) {
        getRegistration().keyMappings.add(keyMapping);
    }

    @Override
    public void registerClientTickStart(ClientTickHandler handler) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> handler.handle(Minecraft.getInstance()));
    }

    @Override
    public void registerClientTickEnd(ClientTickHandler handler) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> handler.handle(Minecraft.getInstance()));
    }

    @Override
    public void registerHudElement(ResourceLocation id, HudElementHandler element) {
        getRegistration().hudElements.put(id, element);
    }

    /**
     * Drawn after block entities, with the pose stack the level renderer drew them with, which
     * starts at the camera. Vanilla flushes the shared buffer source later in the frame.
     */
    @Override
    public void registerLevelSubmit(LevelSubmitHandler handler) {
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES && event.getPoseStack() != null) {
                handler.render(event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), event.getCamera(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
            }
        });
    }

    @Override
    public <T extends ParticleOptions> void registerParticle(Supplier<ParticleType<T>> type, Function<SpriteSet, ParticleProvider<T>> particleFactory) {
        getRegistration().particleProviders.put((Supplier<ParticleType<?>>) (Supplier<? extends ParticleType<?>>) type, particleFactory::apply);
    }

    @Override
    public Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    public static Registrations getRegistration() {
        final var container = ModLoadingContext.get().getActiveContainer();
        final String modId = container.getModId();
        if (registrationsMap.containsKey(modId)) {
            return registrationsMap.get(modId);
        } else {
            Registrations newRegistration = new Registrations();
            registrationsMap.put(modId, newRegistration);
            container.getEventBus().register(newRegistration);
            return newRegistration;
        }
    }

    public static class Registrations {
        private final Map<Supplier<BlockEntityType<?>>, BlockEntityRendererProvider<?>> blockEntityRenderers = new HashMap<>();
        private final Map<Supplier<EntityType<?>>, EntityRendererProvider<?>> entityRenderers = new HashMap<>();
        private final Map<ModelLayerLocation, Supplier<LayerDefinition>> entityLayers = new HashMap<>();
        private final Map<BlockColor, Supplier<List<Block>>> blockColors = new HashMap<>();
        private final Map<ItemColor, Supplier<List<? extends ItemLike>>> itemColors = new HashMap<>();
        private final List<Pair<Supplier<? extends Item>, Pair<ResourceLocation, ClampedItemPropertyFunction>>> itemProperties = new ArrayList<>();
        private final List<KeyMapping> keyMappings = new ArrayList<>();
        private final List<Consumer<ISpecialModelRendererRegistry>> specialModelRendererInitializers = Collections.synchronizedList(new ArrayList<>());
        private final Map<Supplier<? extends Item>, Supplier<? extends BlockEntityWithoutLevelRenderer>> itemRenderers = new HashMap<>();
        private final Map<ResourceLocation, PreparableReloadListener> clientReloadListeners = new HashMap<>();
        private final Map<ResourceLocation, IModelSpecificationLoader<?>> modelLoaders = new HashMap<>();
        private final Map<Supplier<ParticleType<?>>, Function<SpriteSet, ParticleProvider<?>>> particleProviders = new HashMap<>();
        private final Map<Supplier<MenuType<?>>, LibScreenFactory<?, ?>> menuTypes = new HashMap<>();
        private final Map<ResourceLocation, HudElementHandler> hudElements = new HashMap<>();
        private boolean itemRenderersCollected;

        @SubscribeEvent
        @SuppressWarnings("unchecked")
        public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            for (Map.Entry<Supplier<BlockEntityType<?>>, BlockEntityRendererProvider<?>> entry : blockEntityRenderers.entrySet()) {
                event.registerBlockEntityRenderer(entry.getKey().get(), (BlockEntityRendererProvider<BlockEntity>) entry.getValue());
            }

            for (Map.Entry<Supplier<EntityType<?>>, EntityRendererProvider<?>> entry : entityRenderers.entrySet()) {
                event.registerEntityRenderer(entry.getKey().get(), (EntityRendererProvider<Entity>) entry.getValue());
            }
        }

        @SubscribeEvent
        public void registerEntityLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
            for (Map.Entry<ModelLayerLocation, Supplier<LayerDefinition>> entry : entityLayers.entrySet()) {
                event.registerLayerDefinition(entry.getKey(), entry.getValue());
            }
        }

        @SubscribeEvent
        public void registerBlockColors(final RegisterColorHandlersEvent.Block event) {
            for (Map.Entry<BlockColor, Supplier<List<Block>>> entry : blockColors.entrySet()) {
                event.register(entry.getKey(), entry.getValue().get().toArray(Block[]::new));
            }
        }

        @SubscribeEvent
        public void registerItemColors(final RegisterColorHandlersEvent.Item event) {
            for (Map.Entry<ItemColor, Supplier<List<? extends ItemLike>>> entry : itemColors.entrySet()) {
                event.register(entry.getKey(), entry.getValue().get().toArray(ItemLike[]::new));
            }
        }

        /**
         * Item properties live in a plain static map that is only read while rendering, so they are
         * put in on the main thread once registries are frozen.
         */
        @SubscribeEvent
        public void registerItemProperties(final FMLClientSetupEvent event) {
            event.enqueueWork(() -> itemProperties.forEach(entry -> ItemProperties.register(entry.getFirst().get(), entry.getSecond().getFirst(), entry.getSecond().getSecond())));
        }

        @SubscribeEvent
        @SuppressWarnings("rawtypes")
        public void registerMenuScreens(final RegisterMenuScreensEvent event) {
            for (Map.Entry<Supplier<MenuType<?>>, LibScreenFactory<?, ?>> entry : menuTypes.entrySet()) {
                this.registerMenu(event, entry.getKey()::get, (LibScreenFactory) entry.getValue());
            }
        }

        private <T extends AbstractContainerMenu, S extends Screen & MenuAccess<T>> void registerMenu(RegisterMenuScreensEvent event, Supplier<MenuType<? extends T>> menuType, LibScreenFactory<T, S> factory) {
            event.register(menuType.get(), factory::create);
        }

        /**
         * Each renderer is made the first time NeoForge asks an item for it, and is reloaded with
         * the resources from then on, so it can bake its model layers as vanilla's own one does.
         */
        private synchronized Map<Supplier<? extends Item>, Supplier<? extends BlockEntityWithoutLevelRenderer>> itemRenderers() {
            if (!this.itemRenderersCollected) {
                this.itemRenderersCollected = true;
                final ISpecialModelRendererRegistry register = (item, renderer) -> this.itemRenderers.put(item, Suppliers.memoize(renderer::get));
                specialModelRendererInitializers.forEach(callback -> callback.accept(register));
            }
            return this.itemRenderers;
        }

        @SubscribeEvent
        public void registerClientExtensions(final RegisterClientExtensionsEvent event) {
            for (Map.Entry<Supplier<? extends Item>, Supplier<? extends BlockEntityWithoutLevelRenderer>> entry : itemRenderers().entrySet()) {
                final Supplier<? extends BlockEntityWithoutLevelRenderer> renderer = entry.getValue();
                event.registerItem(new IClientItemExtensions() {
                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return renderer.get();
                    }
                }, entry.getKey().get());
            }
        }

        @SubscribeEvent
        public void registerKeyMapping(final RegisterKeyMappingsEvent event) {
            for (KeyMapping key : keyMappings) {
                event.register(key);
            }
        }

        @SubscribeEvent
        public void registerClientReloadListeners(final RegisterClientReloadListenersEvent event) {
            for (PreparableReloadListener listener : clientReloadListeners.values()) {
                event.registerReloadListener(listener);
            }

            for (IModelSpecificationLoader<?> modelLoader : modelLoaders.values()) {
                event.registerReloadListener(modelLoader);
            }

            for (Supplier<? extends BlockEntityWithoutLevelRenderer> renderer : itemRenderers().values()) {
                event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> renderer.get().onResourceManagerReload(resourceManager));
            }
        }

        @SubscribeEvent
        public void registerModelLoaders(final ModelEvent.RegisterGeometryLoaders event) {
            for (Map.Entry<ResourceLocation, IModelSpecificationLoader<?>> entry : modelLoaders.entrySet()) {
                event.register(entry.getKey(), new ForgePlatformModelLoaderPlatformDelegate<>(entry.getValue()));
            }
        }

        @SubscribeEvent
        public void registerHudElements(final RegisterGuiLayersEvent event) {
            for (Map.Entry<ResourceLocation, HudElementHandler> entry : hudElements.entrySet()) {
                event.registerAbove(VanillaGuiLayers.CROSSHAIR, entry.getKey(), entry.getValue()::render);
            }
        }

        @SubscribeEvent
        @SuppressWarnings("unchecked")
        public void registerParticles(final RegisterParticleProvidersEvent event) {
            for (Map.Entry<Supplier<ParticleType<?>>, Function<SpriteSet, ParticleProvider<?>>> entry : particleProviders.entrySet()) {
                event.registerSpriteSet((ParticleType<ParticleOptions>) entry.getKey().get(), sprites -> (ParticleProvider<ParticleOptions>) entry.getValue().apply(sprites));
            }
        }
    }
}
