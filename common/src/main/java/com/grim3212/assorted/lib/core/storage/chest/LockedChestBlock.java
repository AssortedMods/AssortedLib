package com.grim3212.assorted.lib.core.storage.chest;

import com.grim3212.assorted.lib.core.inventory.locking.ILockable;
import com.grim3212.assorted.lib.core.inventory.locking.StorageUtil;
import com.grim3212.assorted.lib.core.storage.BaseStorageBlock;
import com.grim3212.assorted.lib.core.storage.IStorageMaterial;
import com.grim3212.assorted.lib.core.storage.LockedMaterialContainer;
import com.grim3212.assorted.lib.core.storage.StorageMaterial;
import com.grim3212.assorted.lib.core.storage.StorageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** A chest of a {@link StorageMaterial}, or with none the locked stand-in for a vanilla chest, which unlocks back into one. */
public class LockedChestBlock extends BaseStorageBlock implements IStorageMaterial {

    protected static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);

    private final StorageMaterial material;
    private final StorageTypes<LockedChestBlockEntity, LockedMaterialContainer> types;

    public LockedChestBlock(@Nullable StorageMaterial material, StorageTypes<LockedChestBlockEntity, LockedMaterialContainer> types, Block.Properties props) {
        super(props);
        this.material = material;
        this.types = types;
    }

    @Override
    public @Nullable StorageMaterial getStorageMaterial() {
        return material;
    }

    public StorageTypes<LockedChestBlockEntity, LockedMaterialContainer> types() {
        return this.types;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LockedChestBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isDoorBlocked(LevelAccessor world, BlockPos pos) {
        return isInvalidBlock(world, pos.above());
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader worldIn, BlockPos pos, BlockState state, boolean includeData) {
        if (this.getStorageMaterial() == null) {
            return StorageUtil.setCodeOnStack(StorageUtil.getCode(worldIn.getBlockEntity(pos)), new ItemStack(this));
        }
        return super.getCloneItemStack(worldIn, pos, state, includeData);
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);

        if (worldIn.getBlockEntity(pos) instanceof ILockable lockable) {
            lockable.setLockCode(StorageUtil.getCode(stack));
        }
    }

    @Override
    protected boolean removeLock(Level worldIn, BlockPos pos, Player entityplayer) {
        if (this.getStorageMaterial() != null) {
            return super.removeLock(worldIn, pos, entityplayer);
        }

        worldIn.playSound(entityplayer, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.5F, worldIn.getRandom().nextFloat() * 0.1F + 0.9F);

        BlockState state = worldIn.getBlockState(pos);
        if (state.getBlock() instanceof LockedChestBlock && worldIn.getBlockEntity(pos) instanceof LockedChestBlockEntity chestBE) {
            NonNullList<ItemStack> chestItems = NonNullList.withSize(chestBE.getItemStackStorageHandler().getSlots(), ItemStack.EMPTY);
            for (int i = 0; i < chestBE.getItemStackStorageHandler().getSlots(); i++) {
                chestItems.set(i, chestBE.getItemStackStorageHandler().getStackInSlot(i).copy());
                chestBE.getItemStackStorageHandler().setStackInSlot(i, ItemStack.EMPTY);
            }
            chestBE.setLockCode(null);

            worldIn.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, state.getValue(LockedChestBlock.FACING)), 3);
            if (worldIn.getBlockEntity(pos) instanceof ChestBlockEntity newChestBE) {
                for (int i = 0; i < newChestBE.getContainerSize(); i++) {
                    newChestBE.setItem(i, chestItems.get(i));
                }
            }
        }
        return true;
    }
}
