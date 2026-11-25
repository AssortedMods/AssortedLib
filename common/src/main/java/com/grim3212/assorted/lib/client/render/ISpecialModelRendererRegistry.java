package com.grim3212.assorted.lib.client.render;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * Binds an item whose model is special ({@code "parent": "builtin/entity"}) to the
 * {@link BlockEntityWithoutLevelRenderer} that draws it. Vanilla keeps one hard-coded renderer for
 * every such item, so each loader supplies the hook: NeoForge's client item extensions, Fabric's
 * builtin item renderer registry. The loader also registers the renderer as a reload listener, so
 * it can bake its model layers in {@link BlockEntityWithoutLevelRenderer#onResourceManagerReload}.
 */
public interface ISpecialModelRendererRegistry {
    /**
     * Registers {@code renderer} as the one that draws {@code item}. The renderer is created once,
     * when the loader first asks for it.
     */
    void registerItemRenderer(final Supplier<? extends Item> item, final Supplier<? extends BlockEntityWithoutLevelRenderer> renderer);
}
