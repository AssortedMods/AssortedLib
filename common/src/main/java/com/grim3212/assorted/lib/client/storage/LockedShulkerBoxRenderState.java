package com.grim3212.assorted.lib.client.storage;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;

public class LockedShulkerBoxRenderState extends BlockEntityRenderState {

    /** False when the block entity's block is not a locked shulker box, in which case nothing is drawn. */
    public boolean renderModel;
    public Direction facing = Direction.UP;
    /** The dyed shulker box body. */
    public SpriteId colorSprite = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION;
    /** The storage material overlay, drawn as a second pass over the same model. */
    public SpriteId materialSprite = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION;
    public ShulkerBoxModel.State model = new ShulkerBoxModel.State(0.0F, false);
}
