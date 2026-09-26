package com.grim3212.assorted.lib.platform;

import com.google.common.collect.Maps;
import com.grim3212.assorted.lib.client.events.ClientTickHandler;
import com.grim3212.assorted.lib.client.events.HudElementHandler;
import com.grim3212.assorted.lib.client.events.LevelSubmitHandler;
import com.grim3212.assorted.lib.client.model.loader.ForgePlatformModelLoaderPlatformDelegate;
import com.grim3212.assorted.lib.client.model.loader.ForgeBakedModelDelegate;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecification;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecificationHolder;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecificationLoader;
import com.grim3212.assorted.lib.client.model.loaders.context.ResolvedModelBakingContext;
import com.grim3212.assorted.lib.client.render.ISpecialModelRendererRegistry;
import com.grim3212.assorted.lib.client.screen.LibScreenFactory;
import com.grim3212.assorted.lib.platform.services.IClientHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class ForgeClientHelper implements IClientHelper {

    private static final Map<String, Registrations> registrationsMap = Maps.newConcurrentMap();

    // Key mapping categories are shared state on one event instance, so a category may only be
    // registered once no matter how many mods asked for it.
    private static final Set<Identifier> registeredCategories = Collections.synchronizedSet(new HashSet<>());

    @Override
    public <T extends AbstractContainerMenu, S extends Screen & MenuAccess<T>> void registerScreen(Supplier<MenuType<? extends T>> menuType, LibScreenFactory<T, S> factory) {
        getRegistration().menuTypes.put(menuType::get, factory);
    }

    @Override
    public void addReloadListener(Identifier identifier, PreparableReloadListener reloadListener) {
        getRegistration().clientReloadListeners.put(identifier, reloadListener);
    }

    @Override
    public void registerSpecialModelRenderers(final Consumer<ISpecialModelRendererRegistry> register) {
        getRegistration().specialModelRendererInitializers.add(register);
    }

    @Override
    public <E extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(Supplier<? extends BlockEntityType<? extends E>> entityType, BlockEntityRendererProvider<E, S> entityRendererFactory) {
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
    public void registerBlockColor(BlockTintSource color, Supplier<List<Block>> blocks) {
        getRegistration().blockColors.put(color, blocks);
    }

    @Override
    public void registerItemTintSource(Identifier id, MapCodec<? extends ItemTintSource> source) {
        getRegistration().itemTintSources.put(id, source);
    }

    @Override
    public BlockColors getBlockColors() {
        return Minecraft.getInstance().getBlockColors();
    }

    @Override
    public void registerModelLoader(Identifier name, IModelSpecificationLoader<?> modelLoader) {
        getRegistration().modelLoaders.put(name, modelLoader);
    }

    @Override
    public void registerItemModelType(Identifier id, MapCodec<? extends ItemModel.Unbaked> codec) {
        getRegistration().itemModelTypes.put(id, codec);
    }

    @Override
    public void registerConditionalItemModelProperty(Identifier id, MapCodec<? extends ConditionalItemModelProperty> codec) {
        getRegistration().conditionalItemModelProperties.put(id, codec);
    }

    @Override
    public BlockStateModel bakeSpecificationModel(ModelBaker baker, Identifier modelLocation, ModelState modelState) {
        ResolvedModel resolved = baker.getModel(modelLocation);
        if (resolved.wrapped() instanceof IModelSpecificationHolder holder) {
            IModelSpecification<?> specification = holder.getModelSpecification();
            TextureSlots slots = resolved.getTopTextureSlots();
            ResolvedModelBakingContext context = new ResolvedModelBakingContext(baker, resolved, slots);

            return new ForgeBakedModelDelegate(specification.bake(context, baker, modelState, modelLocation));
        }

        // Not a specification model, so there is nothing dynamic to preserve - bake it the way a
        // vanilla variant would.
        return new SingleVariant(SimpleModelWrapper.bake(baker, modelLocation, modelState));
    }

    @Override
    public void registerKeyMapping(KeyMapping keyMapping) {
        getRegistration().keyMappings.add(keyMapping);
    }

    @Override
    public void registerClientTickStart(ClientTickHandler handler) {
        // TickEvent.ClientTickEvent with a phase became two separate events.
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> handler.handle(Minecraft.getInstance()));
    }

    @Override
    public void registerClientTickEnd(ClientTickHandler handler) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> handler.handle(Minecraft.getInstance()));
    }

    @Override
    public void registerHudElement(Identifier id, HudElementHandler element) {
        getRegistration().hudElements.put(id, element);
    }

    @Override
    public void registerLevelSubmit(LevelSubmitHandler handler) {
        NeoForge.EVENT_BUS.addListener((SubmitCustomGeometryEvent event) -> handler.submit(event.getPoseStack(), event.getSubmitNodeCollector(), event.getLevelRenderState().cameraRenderState));
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
            // FMLJavaModLoadingContext is gone; every container hands out its own event bus.
            container.getEventBus().register(newRegistration);
            return newRegistration;
        }
    }

    public static class Registrations {
        private final Map<Supplier<BlockEntityType<?>>, BlockEntityRendererProvider<?, ?>> blockEntityRenderers = new HashMap<>();
        private final Map<Supplier<EntityType<?>>, EntityRendererProvider<?>> entityRenderers = new HashMap<>();
        private final Map<ModelLayerLocation, Supplier<LayerDefinition>> entityLayers = new HashMap<>();
        private final Map<BlockTintSource, Supplier<List<Block>>> blockColors = new HashMap<>();
        private final Map<Identifier, MapCodec<? extends ItemTintSource>> itemTintSources = new HashMap<>();
        private final List<KeyMapping> keyMappings = new ArrayList<>();
        private final List<Consumer<ISpecialModelRendererRegistry>> specialModelRendererInitializers = Collections.synchronizedList(new ArrayList<>());
        private final Map<Identifier, PreparableReloadListener> clientReloadListeners = new HashMap<>();
        private final Map<Identifier, IModelSpecificationLoader<?>> modelLoaders = new HashMap<>();
        private final Map<Identifier, MapCodec<? extends ItemModel.Unbaked>> itemModelTypes = new HashMap<>();
        private final Map<Identifier, MapCodec<? extends ConditionalItemModelProperty>> conditionalItemModelProperties = new HashMap<>();
        private final Map<Supplier<ParticleType<?>>, Function<SpriteSet, ParticleProvider<?>>> particleProviders = new HashMap<>();
        private final Map<Supplier<MenuType<?>>, LibScreenFactory<?, ?>> menuTypes = new HashMap<>();
        private final Map<Identifier, HudElementHandler> hudElements = new HashMap<>();

        @SubscribeEvent
        @SuppressWarnings("unchecked")
        public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            for (Map.Entry<Supplier<BlockEntityType<?>>, BlockEntityRendererProvider<?, ?>> entry : blockEntityRenderers.entrySet()) {
                event.registerBlockEntityRenderer(entry.getKey().get(), (BlockEntityRendererProvider<BlockEntity, BlockEntityRenderState>) entry.getValue());
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
        public void registerBlockColors(final RegisterColorHandlersEvent.BlockTintSources event) {
            // A block's tints are an ordered list of sources now, so each source is registered on its
            // own for the blocks it was given.
            for (Map.Entry<BlockTintSource, Supplier<List<Block>>> entry : blockColors.entrySet()) {
                event.register(List.of(entry.getKey()), entry.getValue().get().toArray(Block[]::new));
            }
        }

        @SubscribeEvent
        public void registerItemTintSources(final RegisterColorHandlersEvent.ItemTintSources event) {
            for (Map.Entry<Identifier, MapCodec<? extends ItemTintSource>> entry : itemTintSources.entrySet()) {
                event.register(entry.getKey(), entry.getValue());
            }
        }

        @SubscribeEvent
        @SuppressWarnings("rawtypes")
        public void registerMenuScreens(final RegisterMenuScreensEvent event) {
            // Screens are registered from their own event now rather than from client setup.
            for (Map.Entry<Supplier<MenuType<?>>, LibScreenFactory<?, ?>> entry : menuTypes.entrySet()) {
                this.registerMenu(event, entry.getKey()::get, (LibScreenFactory) entry.getValue());
            }
        }

        private <T extends AbstractContainerMenu, S extends Screen & MenuAccess<T>> void registerMenu(RegisterMenuScreensEvent event, Supplier<MenuType<? extends T>> menuType, LibScreenFactory<T, S> factory) {
            event.register(menuType.get(), factory::create);
        }

        @SubscribeEvent
        public void registerSpecialModelRenderers(final RegisterSpecialModelRendererEvent event) {
            final ISpecialModelRendererRegistry register = event::register;
            specialModelRendererInitializers.forEach(callback -> callback.accept(register));
        }

        @SubscribeEvent
        public void registerKeyMapping(final RegisterKeyMappingsEvent event) {
            for (KeyMapping key : keyMappings) {
                if (registeredCategories.add(key.getCategory().id())) {
                    event.registerCategory(key.getCategory());
                }
                event.register(key);
            }
        }

        @SubscribeEvent
        public void registerClientReloadListeners(final AddClientReloadListenersEvent event) {
            for (Map.Entry<Identifier, PreparableReloadListener> entry : clientReloadListeners.entrySet()) {
                event.addListener(entry.getKey(), entry.getValue());
            }
        }

        @SubscribeEvent
        public void registerItemModelTypes(final RegisterItemModelsEvent event) {
            for (Map.Entry<Identifier, MapCodec<? extends ItemModel.Unbaked>> entry : itemModelTypes.entrySet()) {
                event.register(entry.getKey(), entry.getValue());
            }
        }

        @SubscribeEvent
        public void registerConditionalItemModelProperties(final RegisterConditionalItemModelPropertyEvent event) {
            for (Map.Entry<Identifier, MapCodec<? extends ConditionalItemModelProperty>> entry : conditionalItemModelProperties.entrySet()) {
                event.register(entry.getKey(), entry.getValue());
            }
        }

        @SubscribeEvent
        public void registerModelLoaders(final ModelEvent.RegisterLoaders event) {
            for (Map.Entry<Identifier, IModelSpecificationLoader<?>> entry : modelLoaders.entrySet()) {
                event.register(entry.getKey(), new ForgePlatformModelLoaderPlatformDelegate<>(entry.getValue()));
            }
        }

        @SubscribeEvent
        public void registerHudElements(final RegisterGuiLayersEvent event) {
            for (Map.Entry<Identifier, HudElementHandler> entry : hudElements.entrySet()) {
                event.registerAbove(VanillaGuiLayers.CROSSHAIR, entry.getKey(), entry.getValue()::extract);
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
