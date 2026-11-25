package com.grim3212.assorted.lib.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/**
 * World-space geometry laid out like a blit, not GUI drawing. As with {@link FluidCuboidUtils}, the
 * {@link VertexConsumer} comes from the caller's {@code MultiBufferSource}.
 */
public class RenderHelper {

    public static void lightedBlit(VertexConsumer vertexConsumer, PoseStack poseStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight, int packedLight) {
        innerBlit(vertexConsumer, poseStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight, packedLight);
    }

    private static void innerBlit(VertexConsumer vertexConsumer, PoseStack poseStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, int packedLight) {
        innerBlit(vertexConsumer, poseStack.last().pose(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth, (vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight, packedLight);
    }

    private static void innerBlit(VertexConsumer vertexConsumer, Matrix4f matrix, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int packedLight) {
        vertexConsumer.addVertex(matrix, (float) x1, (float) y2, (float) blitOffset).setColor(255, 255, 255, 255).setUv(minU, maxV).setLight(packedLight);
        vertexConsumer.addVertex(matrix, (float) x2, (float) y2, (float) blitOffset).setColor(255, 255, 255, 255).setUv(maxU, maxV).setLight(packedLight);
        vertexConsumer.addVertex(matrix, (float) x2, (float) y1, (float) blitOffset).setColor(255, 255, 255, 255).setUv(maxU, minV).setLight(packedLight);
        vertexConsumer.addVertex(matrix, (float) x1, (float) y1, (float) blitOffset).setColor(255, 255, 255, 255).setUv(minU, minV).setLight(packedLight);
    }
}
