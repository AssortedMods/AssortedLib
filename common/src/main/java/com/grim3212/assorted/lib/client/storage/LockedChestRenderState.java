package com.grim3212.assorted.lib.client.storage;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;

public class LockedChestRenderState extends BlockEntityRenderState {

    /** False when the block entity's block is not a storage block, in which case nothing is drawn. */
    public boolean renderModel;
    public Direction facing = Direction.SOUTH;
    public SpriteId sprite = Sheets.ENDER_CHEST_LOCATION;
    public StorageModelState model = StorageModelState.CLOSED_UNLOCKED;
}
