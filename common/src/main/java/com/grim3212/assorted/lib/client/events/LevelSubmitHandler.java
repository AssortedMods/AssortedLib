package com.grim3212.assorted.lib.client.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Submits extra geometry into the level each frame, after entities and block entities. The pose
 * stack starts at the camera, so world positions are drawn relative to {@code camera.pos}.
 */
@FunctionalInterface
public interface LevelSubmitHandler {
    void submit(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera);
}
