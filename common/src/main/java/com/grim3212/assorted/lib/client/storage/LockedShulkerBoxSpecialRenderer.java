package com.grim3212.assorted.lib.client.storage;

import com.grim3212.assorted.lib.core.inventory.locking.StorageUtil;
import com.grim3212.assorted.lib.util.NBTHelper;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A shulker box as an item. The color and the lock are read per stack, and the material overlay is a second pass
 * over the same model.
 */
public class LockedShulkerBoxSpecialRenderer implements SpecialModelRenderer<LockedShulkerBoxSpecialRenderer.Data> {

    /**
     * @param colorSprite The dyed body sprite, or null for the undyed one.
     * @param locked      Whether to draw the padlock overlay.
     */
    public record Data(@Nullable SpriteId colorSprite, boolean locked) {
    }

    private final ShulkerBoxModel model;
    private final SpriteGetter sprites;
    private final SpriteId materialSprite;

    public LockedShulkerBoxSpecialRenderer(ShulkerBoxModel model, SpriteGetter sprites, SpriteId materialSprite) {
        this.model = model;
        this.sprites = sprites;
        this.materialSprite = materialSprite;
    }

    @Override
    public void submit(@Nullable Data data, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        SpriteId colorSprite = data != null && data.colorSprite() != null ? data.colorSprite() : Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION;
        ShulkerBoxModel.State state = new ShulkerBoxModel.State(0.0F, data != null && data.locked());

        submitNodeCollector.submitModel(this.model, state, poseStack, lightCoords, overlayCoords, -1, colorSprite, this.sprites, outlineColor, null);
        submitNodeCollector.submitModel(this.model, state, poseStack, lightCoords, overlayCoords, -1, this.materialSprite, this.sprites, outlineColor, null);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.model.setupAnim(new ShulkerBoxModel.State(0.0F, false));
        this.model.root().getExtentsForGui(poseStack, output);
    }

    @Override
    public @Nullable Data extractArgument(ItemStack stack) {
        SpriteId colorSprite = null;
        if (NBTHelper.hasTag(stack, "Color")) {
            int savedColor = NBTHelper.getInt(stack, "Color");
            if (savedColor != -1) {
                colorSprite = Sheets.getShulkerBoxSprite(DyeColor.byId(savedColor));
            }
        }

        return new Data(colorSprite, StorageUtil.hasCode(stack));
    }

    /** The material sprite is a path on the shulker atlas; the layer is the mod's own, which picks the codec it registered. */
    public record Unbaked(Identifier texture, ModelLayerLocation layer) implements SpecialModelRenderer.Unbaked<Data> {

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
        public LockedShulkerBoxSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            ShulkerBoxModel model = new ShulkerBoxModel(context.entityModelSet().bakeLayer(this.layer));
            return new LockedShulkerBoxSpecialRenderer(model, context.sprites(), new SpriteId(Sheets.SHULKER_SHEET, this.texture));
        }
    }
}
