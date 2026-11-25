package com.grim3212.assorted.lib.client.storage;

import com.grim3212.assorted.lib.core.inventory.locking.StorageUtil;
import com.grim3212.assorted.lib.util.NBTHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A shulker box as an item. The color and the lock are read per stack, and the material overlay is
 * a second pass over the same model. The item's model must be special
 * ({@code "parent": "builtin/entity"}), and the renderer registered for it through
 * {@code ISpecialModelRendererRegistry}.
 */
public class LockedShulkerBoxSpecialRenderer extends BlockEntityWithoutLevelRenderer {

    private final ModelLayerLocation layer;
    private final Material materialSprite;
    private ShulkerBoxModel model;

    /**
     * @param texture The material sprite, a path on the shulker atlas.
     * @param layer   The mod's own model layer for the shulker box.
     */
    public LockedShulkerBoxSpecialRenderer(ResourceLocation texture, ModelLayerLocation layer) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.layer = layer;
        this.materialSprite = new Material(Sheets.SHULKER_SHEET, texture);
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        this.model = bake();
    }

    private ShulkerBoxModel bake() {
        return new ShulkerBoxModel(Minecraft.getInstance().getEntityModels().bakeLayer(this.layer));
    }

    /** The dye a stack carries, or null for an undyed box. */
    private static @Nullable DyeColor color(ItemStack stack) {
        if (NBTHelper.hasTag(stack, "Color")) {
            int savedColor = NBTHelper.getInt(stack, "Color");
            if (savedColor != -1) {
                return DyeColor.byId(savedColor);
            }
        }
        return null;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (this.model == null) {
            this.model = bake();
        }

        Material colorSprite = LockedShulkerBoxRenderer.colorMaterial(color(stack));
        this.model.setupAnim(new ShulkerBoxModel.State(0.0F, StorageUtil.hasCode(stack)));

        // Laid out as vanilla's shulker box renderer lays out a placed box facing up, which is what
        // the builtin/entity display transforms expect.
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(0.0D, -1.0D, 0.0D);

        this.model.renderToBuffer(poseStack, colorSprite.buffer(bufferSource, this.model::renderType, stack.hasFoil()), packedLight, packedOverlay);
        this.model.renderToBuffer(poseStack, this.materialSprite.buffer(bufferSource, this.model::renderType, stack.hasFoil()), packedLight, packedOverlay);

        poseStack.popPose();
    }
}
