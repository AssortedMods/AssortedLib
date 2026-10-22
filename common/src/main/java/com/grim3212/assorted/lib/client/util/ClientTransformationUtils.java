package com.grim3212.assorted.lib.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import org.joml.Vector3fc;

public final class ClientTransformationUtils {

    private ClientTransformationUtils() {
        throw new IllegalStateException("Tried to initialize TransformationUtils. But this is a utility class!");
    }

    public static void push(PoseStack stack, final Transformation transformation, final boolean requiresStackPush) {
        if (requiresStackPush) {
            stack.pushPose();
        }

        Vector3fc trans = transformation.translation();
        stack.translate(trans.x(), trans.y(), trans.z());

        stack.rotate(transformation.leftRotation());

        Vector3fc scale = transformation.scale();
        stack.scale(scale.x(), scale.y(), scale.z());

        stack.rotate(transformation.rightRotation());
    }

}
