package com.grim3212.assorted.lib.client.model;

import com.google.common.base.Preconditions;
import com.mojang.math.Transformation;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Arrays;

public final class QuadTransformers {

    private static final IQuadTransformer EMPTY = quad -> {
    };
    private static final IQuadTransformer[] EMISSIVE_TRANSFORMERS = Util.make(new IQuadTransformer[16], array -> {
        Arrays.setAll(array, i -> applyingLightmap(LightTexture.pack(i, i)));
    });

    private QuadTransformers() {
    }

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
        if (transform.equals(Transformation.identity()))
            return empty();

        final Matrix3f normalTransform = getNormalTransform(transform);
        return quad -> {
            var vertices = quad.getVertices();
            for (int i = 0; i < 4; i++) {
                int offset = i * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
                float x = Float.intBitsToFloat(vertices[offset]);
                float y = Float.intBitsToFloat(vertices[offset + 1]);
                float z = Float.intBitsToFloat(vertices[offset + 2]);

                Vector4f pos = new Vector4f(x, y, z, 1);
                pos.mul(transform.getMatrix());
                pos.div(pos.w);

                vertices[offset] = Float.floatToRawIntBits(pos.x());
                vertices[offset + 1] = Float.floatToRawIntBits(pos.y());
                vertices[offset + 2] = Float.floatToRawIntBits(pos.z());
            }

            for (int i = 0; i < 4; i++) {
                int offset = i * IQuadTransformer.STRIDE + IQuadTransformer.NORMAL;
                int normalIn = vertices[offset];
                if ((normalIn & 0x00FFFFFF) != 0) // The ignored byte is padding and may be filled with user data
                {
                    float x = ((byte) (normalIn & 0xFF)) / 127.0f;
                    float y = ((byte) ((normalIn >> 8) & 0xFF)) / 127.0f;
                    float z = ((byte) ((normalIn >> 16) & 0xFF)) / 127.0f;

                    Vector3f pos = new Vector3f(x, y, z);
                    pos.mul(normalTransform);
                    pos.normalize();

                    vertices[offset] = (((byte) (pos.x() * 127.0f)) & 0xFF) |
                            ((((byte) (pos.y() * 127.0f)) & 0xFF) << 8) |
                            ((((byte) (pos.z() * 127.0f)) & 0xFF) << 16) |
                            (normalIn & 0xFF000000); // Restore padding, just in case
                }
            }
        };
    }

    public static Matrix3f getNormalTransform(Transformation transform) {
        Matrix3f normalTransform = new Matrix3f(transform.getMatrix());
        normalTransform.invert();
        normalTransform.transpose();
        return normalTransform;
    }

    /**
     * @return A new {@link BakedQuad} transformer that applies the specified packed light value.
     */
    public static IQuadTransformer applyingLightmap(int packedLight) {
        return quad -> {
            var vertices = quad.getVertices();
            for (int i = 0; i < 4; i++)
                vertices[i * IQuadTransformer.STRIDE + IQuadTransformer.UV2] = packedLight;
        };
    }

    /**
     * @return A new {@link BakedQuad} transformer that applies the specified block and sky light values.
     */
    public static IQuadTransformer applyingLightmap(int blockLight, int skyLight) {
        return applyingLightmap(LightTexture.pack(blockLight, skyLight));
    }

    /**
     * @return A {@link BakedQuad} transformer that sets the lightmap to the given emissivity (0-15)
     */
    public static IQuadTransformer settingEmissivity(int emissivity) {
        Preconditions.checkArgument(emissivity >= 0 && emissivity < 16, "Emissivity must be between 0 and 15.");
        return EMISSIVE_TRANSFORMERS[emissivity];
    }

    /**
     * @return A {@link BakedQuad} transformer that sets the lightmap to its max value
     */
    public static IQuadTransformer settingMaxEmissivity() {
        return EMISSIVE_TRANSFORMERS[15];
    }

    /**
     * @param color The color in ARGB format.
     * @return A {@link BakedQuad} transformer that sets the color to the specified value.
     */
    public static IQuadTransformer applyingColor(int color) {
        final int fixedColor = toABGR(color);
        return quad -> {
            var vertices = quad.getVertices();
            for (int i = 0; i < 4; i++)
                vertices[i * IQuadTransformer.STRIDE + IQuadTransformer.COLOR] = fixedColor;
        };
    }

    /** Sets an opaque colour from its 0-255 channels. */
    public static IQuadTransformer applyingColor(int red, int green, int blue) {
        return applyingColor(255, red, green, blue);
    }

    /** Sets a colour from its 0-255 channels. */
    public static IQuadTransformer applyingColor(int alpha, int red, int green, int blue) {
        return applyingColor(alpha << 24 | red << 16 | green << 8 | blue);
    }

    /** Converts ARGB to the ABGR order colours are packed in. It is its own inverse. */
    public static int toABGR(int argb) {
        return (argb & 0xFF00FF00) // alpha and green same spot
                | ((argb >> 16) & 0x000000FF) // red moves to blue
                | ((argb << 16) & 0x00FF0000); // blue moves to red
    }
}
