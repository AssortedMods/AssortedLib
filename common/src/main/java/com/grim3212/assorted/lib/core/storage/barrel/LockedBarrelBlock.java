package com.grim3212.assorted.lib.core.storage.barrel;

import com.grim3212.assorted.lib.core.inventory.INamed;
import com.grim3212.assorted.lib.core.inventory.locking.ILockable;
import com.grim3212.assorted.lib.core.inventory.locking.LockItems;
import com.grim3212.assorted.lib.core.inventory.locking.StorageAccessUtil;
import com.grim3212.assorted.lib.core.inventory.locking.StorageUtil;
import com.grim3212.assorted.lib.core.storage.BaseStorageBlock;
import com.grim3212.assorted.lib.core.storage.BaseStorageBlockEntity;
import com.grim3212.assorted.lib.core.storage.IStorageMaterial;
import com.grim3212.assorted.lib.core.storage.LockedMaterialContainer;
import com.grim3212.assorted.lib.core.storage.StorageMaterial;
import com.grim3212.assorted.lib.core.storage.StorageTypes;
import com.grim3212.assorted.lib.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** A barrel of a {@link StorageMaterial}, or with none the locked stand-in for a vanilla barrel, which unlocks back into one. */
public class LockedBarrelBlock extends Block implements EntityBlock, IStorageMaterial {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    private final StorageMaterial material;
    private final StorageTypes<LockedBarrelBlockEntity, LockedMaterialContainer> types;

    public LockedBarrelBlock(@Nullable StorageMaterial material, StorageTypes<LockedBarrelBlockEntity, LockedMaterialContainer> types, Block.Properties props) {
        super(props);
        this.material = material;
        this.types = types;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    @Override
    public @Nullable StorageMaterial getStorageMaterial() {
        return material;
    }

    public StorageTypes<LockedBarrelBlockEntity, LockedMaterialContainer> types() {
        return this.types;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader worldIn, BlockPos pos, BlockState state, boolean includeData) {
        if (this.getStorageMaterial() == null) {
            return StorageUtil.setCodeOnStack(StorageUtil.getCode(worldIn.getBlockEntity(pos)), new ItemStack(this));
        }
        return super.getCloneItemStack(worldIn, pos, state, includeData);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter worldIn, BlockPos pos) {
        if (worldIn.getBlockEntity(pos) instanceof ILockable lockable && lockable.isLocked() && !StorageAccessUtil.canAccess(worldIn, pos, player)) {
            return -1.0F;
        }

        return super.getDestroyProgress(state, player, worldIn, pos);
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        BlockEntity tileentity = worldIn.getBlockEntity(pos);

        if (tileentity instanceof INamed named && stack.has(DataComponents.CUSTOM_NAME)) {
            named.setCustomName(stack.getHoverName());
        }

        if (tileentity instanceof ILockable lockable) {
            lockable.setLockCode(StorageUtil.getCode(stack));
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
            if (worldIn.getBlockEntity(pos) instanceof ILockable teStorage && teStorage.isLocked()) {
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

        if (StorageAccessUtil.canAccess(worldIn, pos, player)) {
            if (!worldIn.isClientSide()) {
                MenuProvider provider = this.getMenuProvider(state, worldIn, pos);
                if (provider != null) {
                    Services.PLATFORM.openMenu((ServerPlayer) player, provider);
                    player.awardStat(Stats.OPEN_BARREL);
                    if (worldIn instanceof ServerLevel serverLevel) {
                        PiglinAi.angerNearbyPiglins(serverLevel, player, true);
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @Nullable
    protected MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof MenuProvider provider ? provider : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (level1, blockPos, blockState, t) -> {
            if (t instanceof LockedBarrelBlockEntity storage) {
                storage.tick();
            }
        };
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level worldIn, BlockPos pos, int id, int param) {
        super.triggerEvent(state, worldIn, pos, id, param);
        BlockEntity tileentity = worldIn.getBlockEntity(pos);
        return tileentity != null && tileentity.triggerEvent(id, param);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos, Direction direction) {
        if (worldIn.getBlockEntity(pos) instanceof BaseStorageBlockEntity storageBlockEntity) {
            return StorageUtil.getRedstoneSignalFromContainer(storageBlockEntity.getItemStackStorageHandler());
        }

        return super.getAnalogOutputSignal(blockState, worldIn, pos, direction);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
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
        if (state.getBlock() instanceof LockedBarrelBlock && worldIn.getBlockEntity(pos) instanceof LockedBarrelBlockEntity barrelBE) {
            NonNullList<ItemStack> barrelItems = NonNullList.withSize(barrelBE.getItemStackStorageHandler().getSlots(), ItemStack.EMPTY);
            for (int i = 0; i < barrelBE.getItemStackStorageHandler().getSlots(); i++) {
                barrelItems.set(i, barrelBE.getItemStackStorageHandler().getStackInSlot(i).copy());
                barrelBE.getItemStackStorageHandler().setStackInSlot(i, ItemStack.EMPTY);
            }
            barrelBE.setLockCode(null);

            worldIn.setBlock(pos, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, state.getValue(LockedBarrelBlock.FACING)), 3);
            if (worldIn.getBlockEntity(pos) instanceof BarrelBlockEntity newBarrelBE) {
                for (int i = 0; i < newBarrelBE.getContainerSize(); i++) {
                    newBarrelBE.setItem(i, barrelItems.get(i));
                }
            }
        }
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LockedBarrelBlockEntity(pos, state);
    }
}
