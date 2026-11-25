package com.grim3212.assorted.lib.client.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Draws extra geometry into the level each frame, after entities and block entities. The pose stack
 * starts at the camera, so world positions are drawn relative to {@link Camera#getPosition()}.
 * Whatever is drawn into {@code bufferSource} is flushed by the caller.
 */
@FunctionalInterface
public interface LevelSubmitHandler {
    void render(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTick);
}
