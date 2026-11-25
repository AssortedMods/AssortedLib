package com.grim3212.assorted.lib.client.storage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;

/**
 * The single chest Lib's locked chests are drawn with, a padlock in place of the handle while
 * locked. Pose it with {@link #setupAnim(StorageModelState)} before each draw.
 */
public class ChestModel extends Model {

    private final ModelPart root;
    private final ModelPart lid;
    private final ModelPart lock;
    private final ModelPart unlocked;

    public ChestModel(ModelPart root) {
        super(RenderType::entityCutout);
        this.root = root;
        this.lid = root.getChild("main").getChild("lid");
        this.lock = root.getChild("lock");
        this.unlocked = root.getChild("unlocked");
    }

    public static LayerDefinition createBaseMeshDefinition() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 19).addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F), PartPose.ZERO);
        partdefinition1.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
        partdefinition.addOrReplaceChild("unlocked", CubeListBuilder.create().texOffs(0, 9).addBox(7.0F, -1.0F, 15.0F, 2.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        partdefinition.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(1, 7).addBox(7.0F, 3.0F, 15.0F, 2.0F, 1.0F, 1.0F).texOffs(0, 4).addBox(6.0F, 0.0F, 15.0F, 1.0F, 3.0F, 1.0F).addBox(9.0F, 0.0F, 15.0F, 1.0F, 3.0F, 1.0F).texOffs(0, 0).addBox(6.0F, -3.0F, 15.0F, 4.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 8.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public ModelPart root() {
        return this.root;
    }

    public void setupAnim(StorageModelState state) {
        float xRot = (float) Math.toRadians(state.doorAngle() / -1.0F);
        this.lid.xRot = xRot;
        this.lock.xRot = xRot;
        this.unlocked.xRot = xRot;

        this.unlocked.visible = state.renderHandle();
        this.lock.visible = !state.renderHandle();
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
