package com.grim3212.assorted.lib.client.storage;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * Vanilla's shulker box model plus the padlock overlay; the state carries the lid progress and the
 * lock. It uses {@code entityCutout}, which does not cull, as the old renderer effectively did.
 */
public class ShulkerBoxModel extends Model<ShulkerBoxModel.State> {
    private static final String LID = "lid";
    private static final String BASE = "base";
    private static final String LOCK = "lock";
    private final ModelPart lid;
    private final ModelPart lock;

    /**
     * @param progress How far the lid is open, 0-1.
     * @param locked   Whether to draw the padlock overlay.
     */
    public record State(float progress, boolean locked) {
    }

    public ShulkerBoxModel(ModelPart modelPart) {
        super(modelPart, RenderTypes::entityCutout);
        this.lid = modelPart.getChild(LID);
        this.lock = modelPart.getChild(LOCK);
    }

    public static LayerDefinition createBaseMeshDefinition() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild(LID, CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 12.0F, 16.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
        partdefinition.addOrReplaceChild(BASE, CubeListBuilder.create().texOffs(0, 28).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 8.0F, 16.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
        partdefinition.addOrReplaceChild(LOCK, CubeListBuilder.create().texOffs(0, 24).addBox(-8.2F, 12.0F, -2.0F, 0.2F, 6.0F, 4.0F).addBox(8.2F, 12.0F, -2.0F, 0.2F, 6.0F, 4.0F).texOffs(0, 28).addBox(-2.0F, 12.0F, -8.2F, 4.0F, 6.0F, 0.0F).addBox(-2.0F, 12.0F, 8.2F, 4.0F, 6.0F, 0.0F), PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(ShulkerBoxModel.State state) {
        super.setupAnim(state);

        float y = state.progress() * 0.5F * 16.0F;
        float yRot = 270.0F * state.progress() * ((float) Math.PI / 180F);

        this.lid.setPos(0.0F, 24.0F - y, 0.0F);
        this.lid.yRot = yRot;

        this.lock.setPos(0.0F, -y, 0.0F);
        this.lock.yRot = yRot;
        this.lock.visible = state.locked();
    }
}
