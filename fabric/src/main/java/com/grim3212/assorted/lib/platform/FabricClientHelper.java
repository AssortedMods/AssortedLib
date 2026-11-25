package com.grim3212.assorted.lib.platform;

import com.google.common.base.Suppliers;
import com.grim3212.assorted.lib.client.events.ClientTickHandler;
import com.grim3212.assorted.lib.client.events.HudElementHandler;
import com.grim3212.assorted.lib.client.events.LevelSubmitHandler;
import com.grim3212.assorted.lib.client.model.loader.FabricModelLoaders;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecificationLoader;
import com.grim3212.assorted.lib.client.render.ISpecialModelRendererRegistry;
import com.grim3212.assorted.lib.client.screen.LibScreenFactory;
import com.grim3212.assorted.lib.platform.services.IClientHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
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
    public void addReloadListener(ResourceLocation identifier, PreparableReloadListener reloadListener) {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new FabricIdentifiedReloadListener(identifier, reloadListener));
    }

    /**
     * Fabric's builtin item renderer registry is the hook for a {@code builtin/entity} item. The
     * renderer is made on the first resource reload, once the entity models it bakes from exist, and
     * is reloaded with every later one, as vanilla reloads its own.
     */
    @Override
    public void registerSpecialModelRenderers(final Consumer<ISpecialModelRendererRegistry> register) {
        register.accept(FabricClientHelper::registerItemRenderer);
    }

    private static void registerItemRenderer(final Supplier<? extends Item> item, final Supplier<? extends BlockEntityWithoutLevelRenderer> renderer) {
        final Item target = item.get();
        final Supplier<BlockEntityWithoutLevelRenderer> made = Suppliers.memoize(renderer::get);
        BuiltinItemRendererRegistry.INSTANCE.register(target, (stack, context, poseStack, bufferSource, light, overlay) ->
                made.get().renderByItem(stack, context, poseStack, bufferSource, light, overlay));

        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(target).withSuffix("_item_renderer");
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return id;
            }

            @Override
            public void onResourceManagerReload(final ResourceManager resourceManager) {
                made.get().onResourceManagerReload(resourceManager);
            }
        });
    }

    @Override
    public <E extends BlockEntity> void registerBlockEntityRenderer(Supplier<? extends BlockEntityType<? extends E>> entityType, BlockEntityRendererProvider<E> entityRendererFactory) {
        BlockEntityRenderers.register(entityType.get(), entityRendererFactory);
    }

    @Override
    public <E extends Entity> void registerEntityRenderer(Supplier<? extends EntityType<? extends E>> entityType, EntityRendererProvider<E> entityRendererFactory) {
        EntityRendererRegistry.register(entityType.get(), entityRendererFactory);
    }

    @Override
    public void registerEntityLayer(ModelLayerLocation modelLayerLocation, Supplier<LayerDefinition> layerDefinition) {
        EntityModelLayerRegistry.registerModelLayer(modelLayerLocation, layerDefinition::get);
    }

    @Override
    public void registerBlockColor(BlockColor color, Supplier<List<Block>> blocks) {
        ColorProviderRegistry.BLOCK.register(color, blocks.get().toArray(new Block[0]));
    }

    @Override
    public void registerBlockRenderType(RenderType renderType, Supplier<List<Block>> blocks) {
        BlockRenderLayerMap.INSTANCE.putBlocks(renderType, blocks.get().toArray(new Block[0]));
    }

    @Override
    public void registerItemColor(ItemColor color, Supplier<List<? extends ItemLike>> items) {
        ColorProviderRegistry.ITEM.register(color, items.get().toArray(new ItemLike[0]));
    }

    @Override
    public BlockColors getBlockColors() {
        return Minecraft.getInstance().getBlockColors();
    }

    @Override
    public void registerModelLoader(ResourceLocation name, IModelSpecificationLoader<?> modelLoader) {
        FabricModelLoaders.register(name, modelLoader);
    }

    /** {@code ItemProperties#register} is private in vanilla; Fabric API's transitive access wideners open it. */
    @Override
    public void registerItemProperty(Supplier<? extends Item> item, ResourceLocation id, ClampedItemPropertyFunction property) {
        ItemProperties.register(item.get(), id, property);
    }

    /**
     * Vanilla's bakery hands a loader model to {@code BlockModelSpecificationMixin}, which bakes it
     * through its specification, so the baker's own bake already keeps it dynamic.
     */
    @Override
    public BakedModel bakeSpecificationModel(ModelBaker baker, ResourceLocation modelLocation, ModelState modelState) {
        return baker.bake(modelLocation, modelState);
    }

    @Override
    public void registerKeyMapping(KeyMapping keyMapping) {
        KeyBindingHelper.registerKeyBinding(keyMapping);
    }

    @Override
    public void registerClientTickStart(ClientTickHandler handler) {
        ClientTickEvents.START_CLIENT_TICK.register(handler::handle);
    }

    @Override
    public void registerClientTickEnd(ClientTickHandler handler) {
        ClientTickEvents.END_CLIENT_TICK.register(handler::handle);
    }

    /**
     * Fabric's HUD callback runs once the whole vanilla HUD is drawn, so the element lands above the
     * crosshair rather than straight after it.
     */
    @Override
    public void registerHudElement(ResourceLocation id, HudElementHandler element) {
        HudRenderCallback.EVENT.register(element::render);
    }

    /**
     * Drawn after entities into the entity buffers, which vanilla flushes with its own. The pose
     * stack starts at the camera: 1.21.1 applies the view rotation on the model view matrix.
     */
    @Override
    public void registerLevelSubmit(LevelSubmitHandler handler) {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (context.consumers() == null) {
                return;
            }

            final PoseStack poseStack = context.matrixStack() != null ? context.matrixStack() : new PoseStack();
            handler.render(poseStack, context.consumers(), context.camera(), context.tickCounter().getGameTimeDeltaPartialTick(false));
        });
    }

    @Override
    public <T extends ParticleOptions> void registerParticle(Supplier<ParticleType<T>> type, Function<SpriteSet, ParticleProvider<T>> factory) {
        ParticleFactoryRegistry.getInstance().register(type.get(), factory::apply);
    }

    @Override
    public Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }
}
