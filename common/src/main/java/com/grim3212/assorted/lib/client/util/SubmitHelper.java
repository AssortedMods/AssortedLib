package com.grim3212.assorted.lib.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * The 26.2 {@code submitModel} overloads that took the block breaking overlay. 26.3 draws that overlay
 * as its own submit one order later, which is what each of these does after the model.
 */
public final class SubmitHelper {

    private SubmitHelper() {
    }

    public static <S> void submitModel(SubmitNodeCollector collector, Model<S> model, S state, PoseStack poseStack, int lightCoords, int overlayCoords, int tintedColor,
                                       SpriteId sprite, SpriteGetter sprites, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        collector.submitModel(model, state, poseStack, lightCoords, overlayCoords, tintedColor, sprite, sprites, outlineColor);
        crumble(collector, model, state, poseStack, sprite.renderType(model.renderType()), lightCoords, overlayCoords, crumbling);
    }

    public static <S> void submitModel(SubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack, Identifier texture, int lightCoords,
                                       int overlayCoords, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        collector.submitModel(model, state, poseStack, texture, lightCoords, overlayCoords, outlineColor);
        crumble(collector, model, state, poseStack, model.renderType(texture), lightCoords, overlayCoords, crumbling);
    }

    public static <S> void submitModel(SubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords,
                                       int overlayCoords, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        collector.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, outlineColor);
        crumble(collector, model, state, poseStack, renderType, lightCoords, overlayCoords, crumbling);
    }

    private static <S> void crumble(SubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords,
                                    int overlayCoords, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        if (crumbling != null) {
            collector.order(1).submitCrumblingOverlay(model, state, poseStack, renderType, lightCoords, overlayCoords, -1, crumbling);
        }
    }
}
