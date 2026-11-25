package com.grim3212.assorted.lib.platform.services;

import com.grim3212.assorted.lib.client.events.ClientTickHandler;
import com.grim3212.assorted.lib.client.events.HudElementHandler;
import com.grim3212.assorted.lib.client.events.LevelSubmitHandler;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecificationLoader;
import com.grim3212.assorted.lib.client.render.ISpecialModelRendererRegistry;
import com.grim3212.assorted.lib.client.screen.LibScreenFactory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
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

public interface IClientHelper {

    <T extends AbstractContainerMenu, S extends Screen & MenuAccess<T>> void registerScreen(Supplier<MenuType<? extends T>> menuType, LibScreenFactory<T, S> factory);

    void addReloadListener(ResourceLocation identifier, PreparableReloadListener reloadListener);

    void registerSpecialModelRenderers(final Consumer<ISpecialModelRendererRegistry> register);

    <E extends BlockEntity> void registerBlockEntityRenderer(Supplier<? extends BlockEntityType<? extends E>> entityType, BlockEntityRendererProvider<E> entityRendererFactory);

    <E extends Entity> void registerEntityRenderer(Supplier<? extends EntityType<? extends E>> entityType, EntityRendererProvider<E> entityRendererFactory);

    void registerEntityLayer(ModelLayerLocation modelLayerLocation, Supplier<LayerDefinition> layerDefinition);

    void registerBlockColor(BlockColor color, Supplier<List<Block>> blocks);

    /**
     * Draws {@code blocks}, and their block items, in the chunk layer {@code renderType}, such as
     * {@code RenderType.cutout()} for a texture with see-through pixels. NeoForge reads the layer from
     * the {@code "render_type"} in each block model json instead, so this does nothing there and the
     * json still has to name it; Fabric on 1.21.1 ignores that key and draws every block solid unless
     * its layer is registered here. Pass the same layer the json names.
     */
    void registerBlockRenderType(RenderType renderType, Supplier<List<Block>> blocks);

    /** Tints the quads of {@code items}' models that carry a tint index. */
    void registerItemColor(ItemColor color, Supplier<List<? extends ItemLike>> items);

    BlockColors getBlockColors();

    void registerModelLoader(ResourceLocation name, IModelSpecificationLoader<?> modelLoader);

    /**
     * Registers an item property, which an item model json's {@code overrides} can branch on with a
     * {@code predicate}. This is how an item's model varies per stack.
     */
    void registerItemProperty(Supplier<? extends Item> item, ResourceLocation id, ClampedItemPropertyFunction property);

    /**
     * Bakes the model at {@code modelLocation}, keeping it dynamic when it is backed by an
     * {@link com.grim3212.assorted.lib.client.model.loaders.IModelSpecification}, so one that depends
     * on a block entity draws what it holds. Any other model bakes as a plain model.
     *
     * @param baker         kept past the bake by a data aware model, which bakes states while
     *                      rendering
     * @param modelLocation a model json, which the 1.21.1 bakery loads on demand while it bakes
     */
    BakedModel bakeSpecificationModel(ModelBaker baker, ResourceLocation modelLocation, ModelState modelState);

    void registerKeyMapping(KeyMapping keyMapping);

    void registerClientTickStart(ClientTickHandler handler);

    void registerClientTickEnd(ClientTickHandler handler);

    /**
     * Draws {@code element} on the HUD directly after vanilla's crosshair. Registered during mod
     * construction, the same as every other client registration here.
     */
    void registerHudElement(ResourceLocation id, HudElementHandler element);

    /** Adds {@code handler}'s geometry to every level frame. */
    void registerLevelSubmit(LevelSubmitHandler handler);

    <T extends ParticleOptions> void registerParticle(Supplier<ParticleType<T>> type, Function<SpriteSet, ParticleProvider<T>> particleFactory);

    Player getClientPlayer();
}
