package com.grim3212.assorted.lib.client.model.vertices;

import com.google.common.base.Preconditions;
import com.grim3212.assorted.lib.client.texture.UnitTextureAtlasSprite;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * A {@link VertexConsumer} that assembles its vertices into {@linkplain BakedQuad baked quads}.
 * Only position and uv are kept; colour, overlay, light and normal are accepted and ignored, as a
 * quad carries none. A vertex ends when the next begins, so the last quad is only emitted on the
 * fifth vertex or {@link #flush()}.
 */
public class QuadBakingVertexConsumer implements VertexConsumer {

    private final Consumer<BakedQuad> quadConsumer;

    protected int vertexIndex = 0;
    private final Vector3fc[] positions = new Vector3fc[4];
    private final long[] packedUVs = new long[4];

    private int tintIndex = -1;
    private Direction direction = Direction.DOWN;
    private TextureAtlasSprite sprite = UnitTextureAtlasSprite.INSTANCE;
    private @Nullable Direction shadeDirectionOverride;
    private int lightEmission;

    public QuadBakingVertexConsumer(Consumer<BakedQuad> quadConsumer) {
        this.quadConsumer = quadConsumer;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        if (vertexIndex == 4)
            flush();

        positions[vertexIndex] = new Vector3f(x, y, z);
        packedUVs[vertexIndex] = UVPair.pack(0.0F, 0.0F);
        vertexIndex++;
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        if (vertexIndex > 0)
            packedUVs[vertexIndex - 1] = UVPair.pack(u, v);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setUv3(float u, float v) {
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        return this;
    }

    /**
     * Emits the quad built from the vertices seen so far, if there are four of them.
     */
    // NeoForge deprecates MaterialInfo#of in favour of an overload that also takes its own ambient
    // occlusion flag; that overload does not exist in vanilla, which this module builds against.
    @SuppressWarnings("deprecation")
    public void flush() {
        if (vertexIndex != 4)
            return;

        Material.Baked material = new Material.Baked(sprite, false);
        BakedQuad.MaterialInfo materialInfo = BakedQuad.MaterialInfo.of(material, sprite.contents().computeTransparency(0.0F, 0.0F, 1.0F, 1.0F), tintIndex, shadeDirectionOverride, lightEmission);
        quadConsumer.accept(new BakedQuad(positions[0], positions[1], positions[2], positions[3], packedUVs[0], packedUVs[1], packedUVs[2], packedUVs[3], direction, materialInfo));
        vertexIndex = 0;
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
     * Whether the renderer applies its diffuse lighting term to this quad. 26.3 dropped the flag, so an
     * unshaded quad is lit as if it faced up, the brightest side.
     */
    public void setShade(boolean shade) {
        this.shadeDirectionOverride = shade ? null : Direction.UP;
    }

    public void setLightEmission(int lightEmission) {
        this.lightEmission = lightEmission;
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
            var quad = Preconditions.checkNotNull(output[0], "No quad has been emitted. Vertices in buffer: " + vertexIndex);
            output[0] = null;
            return quad;
        }
    }
}
