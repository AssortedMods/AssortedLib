package com.grim3212.assorted.lib.core.storage.hopper;

import com.grim3212.assorted.lib.core.inventory.locking.LockItems;
import com.grim3212.assorted.lib.core.storage.BaseStorageBlock;
import com.grim3212.assorted.lib.core.inventory.INamed;
import com.grim3212.assorted.lib.core.inventory.locking.ILockable;
import com.grim3212.assorted.lib.core.inventory.locking.StorageUtil;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.core.inventory.locking.StorageAccessUtil;
import com.grim3212.assorted.lib.core.storage.StorageMaterial;
import com.grim3212.assorted.lib.core.storage.IStorageMaterial;
import com.grim3212.assorted.lib.core.storage.StorageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** A hopper of a {@link StorageMaterial}, or with none the locked stand-in for a vanilla hopper, which unlocks back into one. */
public class LockedHopperBlock extends HopperBlock implements IStorageMaterial {

    private final StorageMaterial material;
    private final StorageTypes<LockedHopperBlockEntity, LockedHopperContainer> types;

    public LockedHopperBlock(@Nullable StorageMaterial material, StorageTypes<LockedHopperBlockEntity, LockedHopperContainer> types, Block.Properties props) {
        super(props);
        this.material = material;
        this.types = types;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ENABLED, true));
    }

    @Override
    public @Nullable StorageMaterial getStorageMaterial() {
        return material;
    }

    public StorageTypes<LockedHopperBlockEntity, LockedHopperContainer> types() {
        return this.types;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader worldIn, BlockPos pos, BlockState state, boolean includeData) {
        if (this.getStorageMaterial() == null) {
            return StorageUtil.setCodeOnStack(StorageUtil.getCode(worldIn.getBlockEntity(pos)), new ItemStack(this));
        }
        return super.getCloneItemStack(worldIn, pos, state, includeData);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter worldIn, BlockPos pos) {
        BlockEntity te = worldIn.getBlockEntity(pos);

        if (te instanceof ILockable) {
            ILockable tileentity = (ILockable) te;

            if (tileentity.isLocked() && !StorageAccessUtil.canAccess(worldIn, pos, player))
                return -1.0F;
        }

        return super.getDestroyProgress(state, player, worldIn, pos);
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        BlockEntity tileentity = worldIn.getBlockEntity(pos);

        if (tileentity instanceof INamed) {
            if (stack.has(DataComponents.CUSTOM_NAME)) {
                ((INamed) tileentity).setCustomName(stack.getHoverName());
            }
        }

        ILockable lockeable = (ILockable) worldIn.getBlockEntity(pos);
        if (lockeable != null) {
            lockeable.setLockCode(StorageUtil.getCode(stack));
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel worldIn, BlockPos pos, boolean movedByPiston) {
        worldIn.updateNeighbourForOutputSignal(pos, this);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack heldStack, BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (this.canBeLocked(worldIn, pos) && LockItems.isLock(player.getItemInHand(handIn))) {
            if (BaseStorageBlock.tryPlaceLock(worldIn, pos, player, handIn))
                return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown() && StorageAccessUtil.canAccess(worldIn, pos, player)) {
            BlockEntity tileentity = worldIn.getBlockEntity(pos);
            if (tileentity instanceof ILockable) {
                ILockable teStorage = (ILockable) tileentity;

                if (teStorage.isLocked()) {
                    ItemStack lockStack = LockItems.createLock(teStorage.getLockCode());

                    if (removeLock(worldIn, pos, player)) {
                        ItemEntity blockDropped = new ItemEntity(worldIn, (double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), lockStack);
                        if (!worldIn.isClientSide() && !lockStack.isEmpty()) {
                            worldIn.addFreshEntity(blockDropped);
                            if (!Services.PLATFORM.isFakePlayer(player)) {
                                blockDropped.playerTouch(player);
                            }
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }

        if (StorageAccessUtil.canAccess(worldIn, pos, player)) {
            if (!worldIn.isClientSide()) {
                MenuProvider inamedcontainerprovider = this.getMenuProvider(state, worldIn, pos);
                if (inamedcontainerprovider != null) {
                    Services.PLATFORM.openMenu((ServerPlayer) player, inamedcontainerprovider);
                    player.awardStat(Stats.INSPECT_HOPPER);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @Nullable
    protected MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        BlockEntity tileentity = world.getBlockEntity(pos);
        return tileentity instanceof MenuProvider ? (MenuProvider) tileentity : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, this.types.blockEntity().get(), LockedHopperBlockEntity::pushItemsTick);
    }

    protected boolean canBeLocked(Level worldIn, BlockPos pos) {
        return !((ILockable) worldIn.getBlockEntity(pos)).isLocked();
    }

    protected boolean removeLock(Level worldIn, BlockPos pos, Player entityplayer) {
        if (this.getStorageMaterial() != null) {
            return BaseStorageBlock.tryRemoveLock(worldIn, pos, entityplayer);
        }

        worldIn.playSound(entityplayer, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.5F, worldIn.getRandom().nextFloat() * 0.1F + 0.9F);

        BlockState state = worldIn.getBlockState(pos);
        if (state.getBlock() instanceof LockedHopperBlock && worldIn.getBlockEntity(pos) instanceof LockedHopperBlockEntity hopperBE) {
            NonNullList<ItemStack> chestItems = NonNullList.withSize(hopperBE.getItemStackStorageHandler().getSlots(), ItemStack.EMPTY);
            for (int i = 0; i < hopperBE.getItemStackStorageHandler().getSlots(); i++) {
                chestItems.set(i, hopperBE.getItemStackStorageHandler().getStackInSlot(i).copy());
                hopperBE.getItemStackStorageHandler().setStackInSlot(i, ItemStack.EMPTY);
            }
            hopperBE.setLockCode(null);

            worldIn.setBlock(pos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, state.getValue(LockedHopperBlock.FACING)), 3);
            if (worldIn.getBlockEntity(pos) instanceof HopperBlockEntity newHopperBE) {
                for (int i = 0; i < newHopperBE.getContainerSize(); i++) {
                    newHopperBE.setItem(i, chestItems.get(i));
                }
            }
        }
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LockedHopperBlockEntity(pos, state);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean flag) {
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof LockedHopperBlockEntity) {
            LockedHopperBlockEntity.entityInside(level, pos, state, entity, (LockedHopperBlockEntity) blockentity);
        }
    }
}