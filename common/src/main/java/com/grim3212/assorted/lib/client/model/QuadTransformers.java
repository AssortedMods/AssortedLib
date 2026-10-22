package com.grim3212.assorted.lib.client.model;

import com.google.common.base.Preconditions;
import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import org.joml.Matrix3f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import java.util.Arrays;

public final class QuadTransformers {

    private static final IQuadTransformer EMPTY = quad -> quad;
    private static final IQuadTransformer[] EMISSIVE_TRANSFORMERS = Util.make(new IQuadTransformer[16], array -> {
        Arrays.setAll(array, QuadTransformers::makeEmissive);
    });

    /**
     * {@return a {@link BakedQuad } transformer that does nothing}
     */
    public static IQuadTransformer empty() {
        return EMPTY;
    }

    /**
     * {@return a new {@link BakedQuad} transformer that applies the specified {@link Transformation }}
     */
    public static IQuadTransformer applying(Transformation transform) {
        if (transform.equals(Transformation.IDENTITY))
            return empty();

        final Matrix4fc matrix = transform.getMatrix();
        return quad -> new BakedQuad(
                transform(matrix, quad.position0()),
                transform(matrix, quad.position1()),
                transform(matrix, quad.position2()),
                transform(matrix, quad.position3()),
                quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                Direction.rotate(matrix, quad.direction()),
                quad.materialInfo());
    }

    private static Vector3fc transform(Matrix4fc matrix, Vector3fc position) {
        Vector4f pos = new Vector4f(position.x(), position.y(), position.z(), 1.0F);
        pos.mul(matrix);
        pos.div(pos.w);
        return new Vector3f(pos.x(), pos.y(), pos.z());
    }

    public static Matrix3f getNormalTransform(Transformation transform) {
        Matrix3f normalTransform = new Matrix3f(transform.getMatrix());
        normalTransform.invert();
        normalTransform.transpose();
        return normalTransform;
    }

    /**
     * @return A {@link BakedQuad} transformer that sets the light emission to the given emissivity (0-15)
     */
    public static IQuadTransformer settingEmissivity(int emissivity) {
        Preconditions.checkArgument(emissivity >= 0 && emissivity < 16, "Emissivity must be between 0 and 15.");
        return EMISSIVE_TRANSFORMERS[emissivity];
    }

    /**
     * @return A {@link BakedQuad} transformer that sets the light emission to its max value
     */
    public static IQuadTransformer settingMaxEmissivity() {
        return EMISSIVE_TRANSFORMERS[15];
    }

    // NeoForge deprecates the canonical MaterialInfo constructor in favour of one that also takes its
    // own ambient occlusion flag; that overload does not exist in vanilla, which this module builds against.
    @SuppressWarnings("deprecation")
    private static IQuadTransformer makeEmissive(int emissivity) {
        return quad -> {
            BakedQuad.MaterialInfo material = quad.materialInfo();
            if (material.lightEmission() == emissivity)
                return quad;

            return new BakedQuad(
                    quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                    quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                    quad.direction(),
                    new BakedQuad.MaterialInfo(material.sprite(), material.layer(), material.itemRenderType(), material.itemGlintRenderType(),
                            material.itemGlintSpecialRenderType(), material.tintIndex(), material.shadeDirectionOverride(), emissivity));
        };
    }

    /** Converts ARGB to the ABGR order colours are packed in. It is its own inverse. */
    public static int toABGR(int argb) {
        return (argb & 0xFF00FF00) // alpha and green same spot
                | ((argb >> 16) & 0x000000FF) // red moves to blue
                | ((argb << 16) & 0x00FF0000); // blue moves to red
    }

    // applyingLightmap and applyingColor are gone: a BakedQuad carries no per-vertex colour or
    // lightmap. Colour and light are passed at submit time (SubmitNodeCollector#submitBlockModel /
    // #submitItem); the only light a quad keeps is MaterialInfo#lightEmission (settingEmissivity).
}
