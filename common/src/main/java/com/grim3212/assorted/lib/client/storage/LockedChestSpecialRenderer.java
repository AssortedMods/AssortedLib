package com.grim3212.assorted.lib.client.storage;

import com.grim3212.assorted.lib.core.inventory.locking.StorageUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** A locked chest as an item, from the chest atlas, drawn with its padlock when the stack carries a lock. */
public class LockedChestSpecialRenderer implements SpecialModelRenderer<Boolean> {

    private final ChestModel model;
    private final SpriteGetter sprites;
    private final SpriteId sprite;

    public LockedChestSpecialRenderer(ChestModel model, SpriteGetter sprites, SpriteId sprite) {
        this.model = model;
        this.sprites = sprites;
        this.sprite = sprite;
    }

    @Override
    public void submit(@Nullable Boolean locked, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        StorageModelState state = Boolean.TRUE.equals(locked) ? StorageModelState.CLOSED_LOCKED : StorageModelState.CLOSED_UNLOCKED;
        submitNodeCollector.submitModel(this.model, state, poseStack, lightCoords, overlayCoords, -1, this.sprite, this.sprites, outlineColor, null);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.model.setupAnim(StorageModelState.CLOSED_UNLOCKED);
        this.model.root().getExtentsForGui(poseStack, output);
    }

    @Override
    public @Nullable Boolean extractArgument(ItemStack stack) {
        return StorageUtil.hasCode(stack);
    }

    /** The sprite is a path on the chest atlas; the layer is the mod's own, which picks the codec it registered. */
    public record Unbaked(Identifier texture, ModelLayerLocation layer) implements SpecialModelRenderer.Unbaked<Boolean> {

        private static final Map<ModelLayerLocation, MapCodec<Unbaked>> CODECS = new ConcurrentHashMap<>();

        /** The codec to register under a mod's own id; one per layer, as an id can only be bound to one codec. */
        public static MapCodec<Unbaked> codec(ModelLayerLocation layer) {
            return CODECS.computeIfAbsent(layer, l -> RecordCodecBuilder.mapCodec(i -> i.group(
                    Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture)
            ).apply(i, texture -> new Unbaked(texture, l))));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return codec(this.layer);
        }

        @Override
        public LockedChestSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            ChestModel model = new ChestModel(context.entityModelSet().bakeLayer(this.layer));
            return new LockedChestSpecialRenderer(model, context.sprites(), new SpriteId(Sheets.CHEST_SHEET, this.texture));
        }
    }
}
