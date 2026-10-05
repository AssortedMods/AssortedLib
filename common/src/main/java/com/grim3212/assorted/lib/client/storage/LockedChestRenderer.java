package com.grim3212.assorted.lib.client.storage;

import com.grim3212.assorted.lib.core.storage.BaseStorageBlock;
import com.grim3212.assorted.lib.core.storage.BaseStorageBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Draws a storage block as a {@link ChestModel} textured from the chest atlas, with the padlock while it is locked.
 * Each mod passes the model layer it registered and the sprite of each of its blocks.
 */
public class LockedChestRenderer<T extends BaseStorageBlockEntity> implements BlockEntityRenderer<T, LockedChestRenderState> {

    private final ChestModel model;
    private final SpriteGetter sprites;
    private final Function<Block, Identifier> sprite;
    private final boolean alwaysLocked;

    public LockedChestRenderer(BlockEntityRendererProvider.Context context, ModelLayerLocation layer, Function<Block, Identifier> sprite, boolean alwaysLocked) {
        this.model = new ChestModel(context.bakeLayer(layer));
        this.sprites = context.sprites();
        this.sprite = sprite;
        this.alwaysLocked = alwaysLocked;
    }

    @Override
    public LockedChestRenderState createRenderState() {
        return new LockedChestRenderState();
    }

    @Override
    public void extractRenderState(T storage, LockedChestRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(storage, state, partialTicks, cameraPosition, breakProgress);

        BlockState blockstate = storage.getLevel() != null ? storage.getBlockState() : storage.getBlockState().setValue(BaseStorageBlock.FACING, Direction.SOUTH);

        state.renderModel = blockstate.getBlock() instanceof BaseStorageBlock;
        if (!state.renderModel) {
            return;
        }

        state.facing = blockstate.getValue(BaseStorageBlock.FACING);
        state.sprite = new SpriteId(Sheets.CHEST_SHEET, this.sprite.apply(blockstate.getBlock()));
        state.model = new StorageModelState(storage.getRotation(partialTicks) * 90.0F, !this.alwaysLocked && !storage.isLocked());
    }

    @Override
    public void submit(LockedChestRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.renderModel) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        submitNodeCollector.submitModel(this.model, state.model, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, state.sprite, this.sprites, 0, state.breakProgress);

        poseStack.popPose();
    }
}
