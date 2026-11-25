package com.grim3212.assorted.lib.client.storage;

import com.grim3212.assorted.lib.core.storage.shulker.LockedShulkerBoxBlock;
import com.grim3212.assorted.lib.core.storage.shulker.LockedShulkerBoxBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/** Draws a shulker box in its dyed color, then its material over the top from the shulker atlas, padlock and all. */
public class LockedShulkerBoxRenderer implements BlockEntityRenderer<LockedShulkerBoxBlockEntity> {

    private final ShulkerBoxModel model;
    private final Function<LockedShulkerBoxBlock, ResourceLocation> materialSprite;

    public LockedShulkerBoxRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation layer, Function<LockedShulkerBoxBlock, ResourceLocation> materialSprite) {
        this.model = new ShulkerBoxModel(context.bakeLayer(layer));
        this.materialSprite = materialSprite;
    }

    /** The dyed body sprite, or the undyed one for no color. */
    public static Material colorMaterial(@Nullable DyeColor color) {
        return color == null ? Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION : Sheets.SHULKER_TEXTURE_LOCATION.get(color.getId());
    }

    @Override
    public void render(LockedShulkerBoxBlockEntity shulkerBE, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        boolean placedInLevel = shulkerBE.getLevel() != null;
        BlockState blockstate = placedInLevel ? shulkerBE.getBlockState() : shulkerBE.getBlockState().setValue(LockedShulkerBoxBlock.FACING, Direction.UP);
        if (!(blockstate.getBlock() instanceof LockedShulkerBoxBlock shulker)) {
            return;
        }

        Direction facing = blockstate.getValue(LockedShulkerBoxBlock.FACING);
        Material colorSprite = colorMaterial(shulkerBE.getColor());
        Material materialSprite = new Material(Sheets.SHULKER_SHEET, this.materialSprite.apply(shulker));

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        float f = 0.9995F;
        poseStack.scale(f, f, f);
        poseStack.mulPose(facing.getRotation());
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(0.0D, -1.0D, 0.0D);

        this.model.setupAnim(new ShulkerBoxModel.State(shulkerBE.getProgress(partialTicks), shulkerBE.isLocked()));
        this.model.renderToBuffer(poseStack, colorSprite.buffer(bufferSource, this.model::renderType), packedLight, packedOverlay);
        // The material texture is only an overlay, drawn as a second pass over the dyed box.
        this.model.renderToBuffer(poseStack, materialSprite.buffer(bufferSource, this.model::renderType), packedLight, packedOverlay);

        poseStack.popPose();
    }
}
