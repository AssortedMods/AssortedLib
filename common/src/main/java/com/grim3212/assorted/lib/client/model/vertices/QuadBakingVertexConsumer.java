package com.grim3212.assorted.lib.client.model.vertices;

import com.google.common.base.Preconditions;
import com.grim3212.assorted.lib.client.model.IQuadTransformer;
import com.grim3212.assorted.lib.client.texture.UnitTextureAtlasSprite;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

import java.util.function.Consumer;

/**
 * A {@link VertexConsumer} that packs its vertices into {@linkplain BakedQuad baked quads} in
 * {@link com.mojang.blaze3d.vertex.DefaultVertexFormat#BLOCK}. A vertex ends when the next begins,
 * so the last quad is only emitted on the fifth vertex or {@link #flush()}.
 */
public class QuadBakingVertexConsumer implements VertexConsumer {
    private static final int QUAD_DATA_SIZE = IQuadTransformer.STRIDE * 4;

    private final Consumer<BakedQuad> quadConsumer;

    /** The vertex being written, -1 before the first. */
    protected int vertexIndex = -1;
    private int[] quadData = new int[QUAD_DATA_SIZE];

    private int tintIndex = -1;
    private Direction direction = Direction.DOWN;
    private TextureAtlasSprite sprite = UnitTextureAtlasSprite.INSTANCE;
    private boolean shade = true;

    public QuadBakingVertexConsumer(Consumer<BakedQuad> quadConsumer) {
        this.quadConsumer = quadConsumer;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        if (vertexIndex == 3)
            flush();

        vertexIndex++;
        int offset = vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
        quadData[offset] = Float.floatToRawIntBits(x);
        quadData[offset + 1] = Float.floatToRawIntBits(y);
        quadData[offset + 2] = Float.floatToRawIntBits(z);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        int offset = vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.NORMAL;
        quadData[offset] = ((int) (x * 127.0f) & 0xFF) |
                (((int) (y * 127.0f) & 0xFF) << 8) |
                (((int) (z * 127.0f) & 0xFF) << 16);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        int offset = vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.COLOR;
        quadData[offset] = ((a & 0xFF) << 24) |
                ((b & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                (r & 0xFF);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        int offset = vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
        quadData[offset] = Float.floatToRawIntBits(u);
        quadData[offset + 1] = Float.floatToRawIntBits(v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        if (IQuadTransformer.UV1 >= 0) { // The block format has no overlay, but a third party may add one
            int offset = vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.UV1;
            quadData[offset] = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
        }
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        int offset = vertexIndex * IQuadTransformer.STRIDE + IQuadTransformer.UV2;
        quadData[offset] = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
        return this;
    }

    /**
     * Emits the quad built from the vertices seen so far, if there are four of them.
     */
    public void flush() {
        if (vertexIndex != 3)
            return;

        quadConsumer.accept(new BakedQuad(quadData, tintIndex, direction, sprite, shade));
        vertexIndex = -1;
        quadData = new int[QUAD_DATA_SIZE];
    }

    public void setTintIndex(int tintIndex) {
        this.tintIndex = tintIndex;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setTexture(TextureAtlasSprite sprite) {
        this.sprite = sprite;
    }

    /**
     * Whether the renderer applies its diffuse lighting term to this quad.
     */
    public void setShade(boolean shade) {
        this.shade = shade;
    }

    public static class Buffered extends QuadBakingVertexConsumer {
        private final BakedQuad[] output;

        public Buffered() {
            this(new BakedQuad[1]);
        }

        private Buffered(BakedQuad[] output) {
            super(q -> output[0] = q);
            this.output = output;
        }

        public BakedQuad getQuad() {
            flush();
            var quad = Preconditions.checkNotNull(output[0], "No quad has been emitted. Vertices in buffer: " + (vertexIndex + 1));
            output[0] = null;
            return quad;
        }
    }
}
