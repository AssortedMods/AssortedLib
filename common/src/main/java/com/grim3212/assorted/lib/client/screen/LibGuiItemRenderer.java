package com.grim3212.assorted.lib.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Draws an item into a screen the way {@link GuiGraphics#renderItem(ItemStack, int, int)} does, but
 * with a view of your own. Vanilla's call always applies the transform the model carries for
 * {@link ItemDisplayContext#GUI}, so a screen that wants a block seen head on - a storage crate's
 * face, a machine's front - cannot use it.
 */
public final class LibGuiItemRenderer {

    private LibGuiItemRenderer() {
    }

    /** Head on, filling the 16x16 the GUI gives an item, with the model's given face toward the viewer. */
    public static ItemTransform facingViewer(float yRotation) {
        return new ItemTransform(new Vector3f(0.0F, yRotation, 0.0F), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));
    }

    /**
     * @param displayContext the context whose model transform is applied when {@code transform} is null
     * @param transform      replaces the transform the model carries, or null to keep the one for {@code displayContext}
     */
    public static void item(GuiGraphics graphics, ItemStack stack, ItemDisplayContext displayContext, @Nullable ItemTransform transform, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, minecraft.level, null, 0);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate((float) (x + 8), (float) (y + 8), 150.0F);
        pose.scale(16.0F, -16.0F, 16.0F);

        // ItemRenderer applies the model's own transform for the context it is given. NONE carries
        // none, so a replacement is applied here and the model is drawn with NONE.
        ItemDisplayContext context = displayContext;
        if (transform != null) {
            transform.apply(false, pose);
            context = ItemDisplayContext.NONE;
        }

        boolean flatLighting = !model.usesBlockLight();
        if (flatLighting) {
            Lighting.setupForFlatItems();
        }

        itemRenderer.render(stack, context, false, pose, graphics.bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY, model);
        graphics.flush();

        if (flatLighting) {
            Lighting.setupFor3DItems();
        }

        pose.popPose();
    }
}
