package com.grim3212.assorted.lib.core.storage.chest;

import com.grim3212.assorted.lib.core.inventory.IMenuDataProvider;
import com.grim3212.assorted.lib.core.storage.BaseStorageBlockEntity;
import com.grim3212.assorted.lib.core.storage.LockedMaterialContainer;
import com.grim3212.assorted.lib.core.storage.StorageItemStackStorageHandler;
import com.grim3212.assorted.lib.core.storage.StorageMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/**
 * The block entity of a {@link LockedChestBlock}. Its type is the one its block names, so a chest saved under a
 * type another mod registered still loads as the right one.
 */
public class LockedChestBlockEntity extends BaseStorageBlockEntity implements IMenuDataProvider<Optional<StorageMaterial>> {

    private final StorageMaterial storageMaterial;

    public LockedChestBlockEntity(BlockPos pos, BlockState state) {
        super(block(state).types().blockEntity().get(), pos, state);
        this.storageMaterial = block(state).getStorageMaterial();
        this.setStorageHandler(new StorageItemStackStorageHandler(this, storageMaterial != null ? storageMaterial.totalItems() : 27));
    }

    private static LockedChestBlock block(BlockState state) {
        return (LockedChestBlock) state.getBlock();
    }

    public StorageMaterial getStorageMaterial() {
        return this.storageMaterial;
    }

    @Override
    public Optional<StorageMaterial> getMenuData(ServerPlayer player) {
        return Optional.ofNullable(this.storageMaterial);
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory player, Player playerEntity) {
        return new LockedMaterialContainer(block(this.getBlockState()).types().menu().get(), windowId, player, this.getItemStackStorageHandler(), storageMaterial, false);
    }

    @Override
    protected Component getDefaultName() {
        return this.blockContainerName();
    }

    @Override
    public int getNumberOfPlayersUsing(Level world, BaseStorageBlockEntity lockableTileEntity, int x, int y, int z) {
        int i = 0;

        for (Player playerentity : world.getEntitiesOfClass(Player.class, new AABB((double) ((float) x - 5.0F), (double) ((float) y - 5.0F), (double) ((float) z - 5.0F), (double) ((float) (x + 1) + 5.0F), (double) ((float) (y + 1) + 5.0F), (double) ((float) (z + 1) + 5.0F)))) {
            if (playerentity.containerMenu instanceof LockedMaterialContainer) {
                ++i;
            }
        }

        return i;
    }
}
