package com.grim3212.assorted.lib.platform;

import com.grim3212.assorted.lib.client.events.ClientTickHandler;
import com.grim3212.assorted.lib.client.events.HudElementHandler;
import com.grim3212.assorted.lib.client.events.LevelSubmitHandler;
import com.grim3212.assorted.lib.client.model.loader.FabricPlatformModelLoaderPlatformDelegate;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecificationLoader;
import com.grim3212.assorted.lib.client.render.ISpecialModelRendererRegistry;
import com.grim3212.assorted.lib.client.screen.LibScreenFactory;
import com.grim3212.assorted.lib.platform.services.IClientHelper;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.grim3212.assorted.lib.client.model.loader.FabricBakedModelDelegate;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecification;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecificationHolder;
import com.grim3212.assorted.lib.client.model.loaders.context.ResolvedModelBakingContext;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedModelDeserializer;
import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FabricClientHelper implements IClientHelper {

    @Override
    public <T extends AbstractContainerMenu, S extends Screen & MenuAccess<T>> void registerScreen(Supplier<MenuType<? extends T>> menuType, LibScreenFactory<T, S> factory) {
        MenuScreens.register(menuType.get(), factory::create);
    }

    @Override
    public void addReloadListener(Identifier identifier, PreparableReloadListener reloadListener) {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(identifier, reloadListener);
    }

    @Override
    public void registerSpecialModelRenderers(final Consumer<ISpecialModelRendererRegistry> register) {
        // BlockEntityWithoutLevelRenderer is gone; a special item renderer is now an id -> unbaked
        // renderer codec that item models refer to by id. Fabric widens the id mapper for us.
        register.accept(SpecialModelRenderers.ID_MAPPER::put);
    }

    @Override
    public <E extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(Supplier<? extends BlockEntityType<? extends E>> entityType, BlockEntityRendererProvider<E, S> entityRendererFactory) {
        BlockEntityRenderers.register(entityType.get(), entityRendererFactory);
    }

    @Override
    public <E extends Entity> void registerEntityRenderer(Supplier<? extends EntityType<? extends E>> entityType, EntityRendererProvider<E> entityRendererFactory) {
        EntityRenderers.register(entityType.get(), entityRendererFactory);
    }

    @Override
    public void registerEntityLayer(ModelLayerLocation modelLayerLocation, Supplier<LayerDefinition> layerDefinition) {
        ModelLayerRegistry.registerModelLayer(modelLayerLocation, layerDefinition::get);
    }

    @Override
    public void registerBlockColor(BlockTintSource color, Supplier<List<Block>> blocks) {
        BlockColorRegistry.register(List.of(color), blocks.get().toArray(new Block[0]));
    }

    @Override
    public void registerItemTintSource(Identifier id, MapCodec<? extends ItemTintSource> source) {
        ItemTintSources.ID_MAPPER.put(id, source);
    }

    @Override
    public BlockColors getBlockColors() {
        return Minecraft.getInstance().getBlockColors();
    }

    @Override
    public void registerModelLoader(Identifier name, IModelSpecificationLoader<?> modelLoader) {
        UnbakedModelDeserializer.register(name, new FabricPlatformModelLoaderPlatformDelegate<>(name, modelLoader));
    }

    /**
     * Fabric API has no item model registry of its own, so this goes at vanilla's - reached through
     * the access widener, since {@code ItemModels.ID_MAPPER} is private. Registering has to happen
     * before the first item model json is read, which client initialisation is.
     */
    @Override
    public void registerItemModelType(Identifier id, MapCodec<? extends ItemModel.Unbaked> codec) {
        ItemModels.ID_MAPPER.put(id, codec);
    }

    @Override
    public void registerConditionalItemModelProperty(Identifier id, MapCodec<? extends ConditionalItemModelProperty> codec) {
        // Widened in assortedlib_fabric.accesswidener; Fabric API has no equivalent of NeoForge's
        // RegisterConditionalItemModelPropertyEvent, so the vanilla mapper is the only registry.
        ConditionalItemModelProperties.ID_MAPPER.put(id, codec);
    }

    @Override
    public BlockStateModel bakeSpecificationModel(ModelBaker baker, Identifier modelLocation, ModelState modelState) {
        ResolvedModel resolved = baker.getModel(modelLocation);
        if (resolved.wrapped() instanceof IModelSpecificationHolder holder) {
            IModelSpecification<?> specification = holder.getModelSpecification();
            TextureSlots slots = resolved.getTopTextureSlots();
            ResolvedModelBakingContext context = new ResolvedModelBakingContext(baker, resolved, slots);

            return new FabricBakedModelDelegate(specification.bake(context, baker, modelState, modelLocation));
        }

        // Not a specification model, so there is nothing dynamic to preserve - bake it the way a
        // vanilla variant would.
        return new SingleVariant(SimpleModelWrapper.bake(baker, modelLocation, modelState));
    }

    @Override
    public void registerKeyMapping(KeyMapping keyMapping) {
        KeyMappingHelper.registerKeyMapping(keyMapping);
    }

    @Override
    public void registerClientTickStart(ClientTickHandler handler) {
        ClientTickEvents.START_CLIENT_TICK.register(handler::handle);
    }

    @Override
    public void registerClientTickEnd(ClientTickHandler handler) {
        ClientTickEvents.END_CLIENT_TICK.register(handler::handle);
    }

    @Override
    public void registerHudElement(Identifier id, HudElementHandler element) {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, id, element::extract);
    }

    @Override
    public void registerLevelSubmit(LevelSubmitHandler handler) {
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> handler.submit(context.poseStack(), context.submitNodeCollector(), context.levelState().cameraRenderState));
    }

    @Override
    public <T extends ParticleOptions> void registerParticle(Supplier<ParticleType<T>> type, Function<SpriteSet, ParticleProvider<T>> factory) {
        ParticleProviderRegistry.getInstance().register(type.get(), (FabricSpriteSet sprites) -> factory.apply(sprites));
    }

    @Override
    public Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }
}
