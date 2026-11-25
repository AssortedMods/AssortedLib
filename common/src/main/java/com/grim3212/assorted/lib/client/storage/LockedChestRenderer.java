package com.grim3212.assorted.lib.client.storage;

import com.grim3212.assorted.lib.core.storage.BaseStorageBlock;
import com.grim3212.assorted.lib.core.storage.BaseStorageBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

/**
 * Draws a storage block as a {@link ChestModel} textured from the chest atlas, with the padlock while it is locked.
 * Each mod passes the model layer it registered and the sprite of each of its blocks. The block
 * breaking overlay needs nothing here: the dispatcher hands a renderer a buffer source that already
 * draws it over whatever is rendered.
 */
public class LockedChestRenderer<T extends BaseStorageBlockEntity> implements BlockEntityRenderer<T> {

    private final ChestModel model;
    private final Function<Block, ResourceLocation> sprite;
    private final boolean alwaysLocked;

    public LockedChestRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation layer, Function<Block, ResourceLocation> sprite, boolean alwaysLocked) {
        this.model = new ChestModel(context.bakeLayer(layer));
        this.sprite = sprite;
        this.alwaysLocked = alwaysLocked;
    }

    @Override
    public void render(T storage, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState blockstate = storage.getLevel() != null ? storage.getBlockState() : storage.getBlockState().setValue(BaseStorageBlock.FACING, Direction.SOUTH);
        if (!(blockstate.getBlock() instanceof BaseStorageBlock)) {
            return;
        }

        Direction facing = blockstate.getValue(BaseStorageBlock.FACING);
        Material material = new Material(Sheets.CHEST_SHEET, this.sprite.apply(blockstate.getBlock()));

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        this.model.setupAnim(new StorageModelState(storage.getRotation(partialTicks) * 90.0F, !this.alwaysLocked && !storage.isLocked()));
        this.model.renderToBuffer(poseStack, material.buffer(bufferSource, this.model::renderType), packedLight, packedOverlay);

        poseStack.popPose();
    }
}
