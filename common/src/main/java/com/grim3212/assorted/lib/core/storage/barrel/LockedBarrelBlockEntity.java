package com.grim3212.assorted.lib.core.storage.barrel;

import com.grim3212.assorted.lib.client.model.data.IBlockModelData;
import com.grim3212.assorted.lib.client.model.data.IModelDataBuilder;
import com.grim3212.assorted.lib.core.block.IBlockEntityWithModelData;
import com.grim3212.assorted.lib.core.inventory.IMenuDataProvider;
import com.grim3212.assorted.lib.core.storage.BaseStorageBlockEntity;
import com.grim3212.assorted.lib.core.storage.LockedMaterialContainer;
import com.grim3212.assorted.lib.core.storage.StorageItemStackStorageHandler;
import com.grim3212.assorted.lib.core.storage.StorageMaterial;
import com.grim3212.assorted.lib.core.storage.StorageModelProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/** The block entity of a {@link LockedBarrelBlock}, of the type its block names. */
public class LockedBarrelBlockEntity extends BaseStorageBlockEntity implements IBlockEntityWithModelData, IMenuDataProvider<Optional<StorageMaterial>> {

    private final StorageMaterial storageMaterial;

    public LockedBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(block(state).types().blockEntity().get(), pos, state);
        this.storageMaterial = block(state).getStorageMaterial();
        this.setStorageHandler(new StorageItemStackStorageHandler(this, storageMaterial != null ? storageMaterial.totalItems() : 27));
    }

    private static LockedBarrelBlock block(BlockState state) {
        return (LockedBarrelBlock) state.getBlock();
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
    protected SoundEvent openSound() {
        return SoundEvents.BARREL_OPEN;
    }

    @Override
    protected SoundEvent closeSound() {
        return SoundEvents.BARREL_CLOSE;
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

    /** A barrel shows it is open through its block state rather than a lid. */
    @Override
    public void onOpenOrClose() {
        if (this.getBlockState().getBlock() instanceof LockedBarrelBlock) {
            this.level.setBlock(this.getBlockPos(), this.getBlockState().setValue(BarrelBlock.OPEN, this.numPlayersUsing > 0), 3);
        }
    }

    @Override
    public @NotNull IBlockModelData getBlockModelData() {
        return IModelDataBuilder.create().withInitial(StorageModelProperties.IS_LOCKED, this.isLocked()).build();
    }
}
