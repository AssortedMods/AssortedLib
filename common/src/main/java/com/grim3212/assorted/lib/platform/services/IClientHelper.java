package com.grim3212.assorted.lib.platform.services;

import com.grim3212.assorted.lib.client.events.ClientTickHandler;
import com.grim3212.assorted.lib.client.events.HudElementHandler;
import com.grim3212.assorted.lib.client.events.LevelSubmitHandler;
import com.grim3212.assorted.lib.client.model.loaders.IModelSpecificationLoader;
import com.grim3212.assorted.lib.client.render.ISpecialModelRendererRegistry;
import com.grim3212.assorted.lib.client.screen.LibScreenFactory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.resources.model.ModelBaker;
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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IClientHelper {

    <T extends AbstractContainerMenu, S extends Screen & MenuAccess<T>> void registerScreen(Supplier<MenuType<? extends T>> menuType, LibScreenFactory<T, S> factory);

    void addReloadListener(Identifier identifier, PreparableReloadListener reloadListener);

    void registerSpecialModelRenderers(final Consumer<ISpecialModelRendererRegistry> register);

    // BlockEntityRendererProvider gained a render-state type parameter in 26.x; renderers now
    // extract a state object and submit from it rather than rendering inline.
    <E extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(Supplier<? extends BlockEntityType<? extends E>> entityType, BlockEntityRendererProvider<E, S> entityRendererFactory);

    <E extends Entity> void registerEntityRenderer(Supplier<? extends EntityType<? extends E>> entityType, EntityRendererProvider<E> entityRendererFactory);

    void registerEntityLayer(ModelLayerLocation modelLayerLocation, Supplier<LayerDefinition> layerDefinition);

    void registerBlockColor(BlockTintSource color, Supplier<List<Block>> blocks);

    // An item's tints live in its model json as ItemTintSource entries; code only registers the
    // codec for a custom source type. A caller that tinted items from code emits a "tints" entry
    // naming this id in those items' model json instead.
    void registerItemTintSource(Identifier id, MapCodec<? extends ItemTintSource> source);

    BlockColors getBlockColors();

    void registerModelLoader(Identifier name, IModelSpecificationLoader<?> modelLoader);

    /**
     * Registers the codec for a custom {@code ItemModel.Unbaked} type, which an item model json
     * names as {@code "model": {"type": "<id>"}}. This is how an item's model varies per stack.
     */
    void registerItemModelType(Identifier id, MapCodec<? extends ItemModel.Unbaked> codec);

    /**
     * Registers the codec for a custom {@link ConditionalItemModelProperty}, which a
     * {@code minecraft:condition} item model can branch on. Needed to read a mod-owned value, since
     * vanilla's {@code component_matches} only takes an exact {@code NbtPredicate}.
     */
    void registerConditionalItemModelProperty(Identifier id, MapCodec<? extends ConditionalItemModelProperty> codec);

    /**
     * Bakes the model at {@code modelLocation}, keeping it dynamic when it is backed by an
     * {@link com.grim3212.assorted.lib.client.model.loaders.IModelSpecification}, so one that depends
     * on a block entity draws what it holds. Any other model bakes as a plain variant.
     *
     * @param baker         kept past the bake by a data aware model, which bakes states while
     *                      rendering
     * @param modelLocation must have been marked as a dependency during discovery
     */
    BlockStateModel bakeSpecificationModel(ModelBaker baker, Identifier modelLocation, ModelState modelState);

    void registerKeyMapping(KeyMapping keyMapping);

    void registerClientTickStart(ClientTickHandler handler);

    void registerClientTickEnd(ClientTickHandler handler);

    /**
     * Draws {@code element} on the HUD directly after vanilla's crosshair. Registered during mod
     * construction, the same as every other client registration here.
     */
    void registerHudElement(Identifier id, HudElementHandler element);

    /** Adds {@code handler}'s geometry to every level frame. */
    void registerLevelSubmit(LevelSubmitHandler handler);

    <T extends ParticleOptions> void registerParticle(Supplier<ParticleType<T>> type, Function<SpriteSet, ParticleProvider<T>> particleFactory);

    Player getClientPlayer();
}
