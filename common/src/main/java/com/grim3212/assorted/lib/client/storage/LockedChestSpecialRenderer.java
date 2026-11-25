package com.grim3212.assorted.lib.client.storage;

import com.grim3212.assorted.lib.core.inventory.locking.StorageUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A locked chest as an item, from the chest atlas, drawn with its padlock when the stack carries a
 * lock. The item's model must be special ({@code "parent": "builtin/entity"}), and the renderer
 * registered for it through {@code ISpecialModelRendererRegistry}.
 */
public class LockedChestSpecialRenderer extends BlockEntityWithoutLevelRenderer {

    private final ModelLayerLocation layer;
    private final Material sprite;
    private ChestModel model;

    /**
     * @param texture The sprite, a path on the chest atlas.
     * @param layer   The mod's own model layer for the chest.
     */
    public LockedChestSpecialRenderer(ResourceLocation texture, ModelLayerLocation layer) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.layer = layer;
        this.sprite = new Material(Sheets.CHEST_SHEET, texture);
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        this.model = bake();
    }

    private ChestModel bake() {
        return new ChestModel(Minecraft.getInstance().getEntityModels().bakeLayer(this.layer));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (this.model == null) {
            this.model = bake();
        }

        this.model.setupAnim(StorageUtil.hasCode(stack) ? StorageModelState.CLOSED_LOCKED : StorageModelState.CLOSED_UNLOCKED);
        this.model.renderToBuffer(poseStack, this.sprite.buffer(bufferSource, this.model::renderType, stack.hasFoil()), packedLight, packedOverlay);
    }
}
