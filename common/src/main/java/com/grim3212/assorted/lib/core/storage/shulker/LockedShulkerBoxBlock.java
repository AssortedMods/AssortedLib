package com.grim3212.assorted.lib.core.storage.shulker;

import com.grim3212.assorted.lib.core.inventory.locking.LockItems;
import com.grim3212.assorted.lib.core.storage.BaseStorageBlock;
import com.grim3212.assorted.lib.core.block.IBlockMapColor;
import com.grim3212.assorted.lib.core.inventory.locking.ILockable;
import com.grim3212.assorted.lib.core.inventory.locking.StorageUtil;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.util.NBTHelper;
import com.grim3212.assorted.lib.core.inventory.locking.StorageAccessUtil;
import com.grim3212.assorted.lib.core.storage.StorageMaterial;
import com.grim3212.assorted.lib.core.storage.IStorageMaterial;
import com.grim3212.assorted.lib.core.storage.LockedMaterialContainer;
import com.grim3212.assorted.lib.core.storage.StorageTypes;
import com.grim3212.assorted.lib.core.storage.BaseStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity.AnimationStatus;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A shulker box of a {@link StorageMaterial}, or with none the locked stand-in for a vanilla one, which unlocks back into it. */
public class LockedShulkerBoxBlock extends Block implements EntityBlock, IStorageMaterial, IBlockMapColor {

    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    private static final BlockBehaviour.StatePredicate CLOSED = (state, level, pos) -> !(level.getBlockEntity(pos) instanceof LockedShulkerBoxBlockEntity shulker) || shulker.isClosed();

    private final StorageMaterial material;
    private final StorageTypes<LockedShulkerBoxBlockEntity, LockedMaterialContainer> types;

    public LockedShulkerBoxBlock(@Nullable StorageMaterial material, StorageTypes<LockedShulkerBoxBlockEntity, LockedMaterialContainer> types, Block.Properties props) {
        super(props.dynamicShape().noOcclusion().isSuffocating(CLOSED).isViewBlocking(CLOSED).pushReaction(PushReaction.DESTROY));
        this.material = material;
        this.types = types;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    public MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof LockedShulkerBoxBlockEntity shulkerBE) {
            return shulkerBE.getColor().getMapColor();
        }

        return state.getMapColor(level, pos);
    }

    protected boolean canBeLocked(Level worldIn, BlockPos pos) {
        return !((ILockable) worldIn.getBlockEntity(pos)).isLocked();
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

    public boolean removeLock(Level level, BlockPos pos, Player player) {
        if (this.getStorageMaterial() != null) {
            return BaseStorageBlock.tryRemoveLock(level, pos, player);
        }

        level.playSound(player, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LockedShulkerBoxBlock && level.getBlockEntity(pos) instanceof LockedShulkerBoxBlockEntity shulkerBE) {
            DyeColor color = shulkerBE.getColor();
            level.setBlock(pos, (color == null ? Blocks.SHULKER_BOX : Blocks.DYED_SHULKER_BOX.pick(color)).defaultBlockState().setValue(ShulkerBoxBlock.FACING, state.getValue(LockedShulkerBoxBlock.FACING)), 3);
            if (level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity newShulkerBE) {
                for (int i = 0; i < shulkerBE.getItemStackStorageHandler().getSlots(); i++) {
                    newShulkerBE.setItem(i, shulkerBE.getItemStackStorageHandler().getStackInSlot(i).copy());
                }

            }
        }
        return true;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack heldStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (this.canBeLocked(level, pos) && LockItems.isLock(player.getItemInHand(hand))) {
            if (BaseStorageBlock.tryPlaceLock(level, pos, player, hand))
                return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown() && StorageAccessUtil.canAccess(level, pos, player)) {
            BlockEntity tileentity = level.getBlockEntity(pos);
            if (tileentity instanceof ILockable) {
                ILockable teStorage = (ILockable) tileentity;

                if (teStorage.isLocked()) {
                    ItemStack lockStack = LockItems.createLock(teStorage.getLockCode());

                    if (removeLock(level, pos, player)) {
                        ItemEntity blockDropped = new ItemEntity(level, (double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), lockStack);
                        if (!level.isClientSide() && !lockStack.isEmpty()) {
                            level.addFreshEntity(blockDropped);
                            if (!Services.PLATFORM.isFakePlayer(player)) {
                                blockDropped.playerTouch(player);
                            }
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }

        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof LockedShulkerBoxBlockEntity shulkerBE) {
            if (canOpen(state, level, pos, shulkerBE) && StorageAccessUtil.canAccess(level, pos, player)) {
                if (!level.isClientSide()) {
                    MenuProvider inamedcontainerprovider = this.getMenuProvider(state, level, pos);
                    if (inamedcontainerprovider != null) {
                        Services.PLATFORM.openMenu((ServerPlayer) player, inamedcontainerprovider);
                        player.awardStat(Stats.OPEN_SHULKER_BOX);
                        if (level instanceof ServerLevel serverLevel) {
                            PiglinAi.angerNearbyPiglins(serverLevel, player, true);
                        }
                    }
                }
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof LockedShulkerBoxBlockEntity shulkerBE) {
            // The color rides along in the stack's custom_data component.
            int savedColor = NBTHelper.getInt(stack, "Color", -1);
            shulkerBE.setColor(savedColor == -1 ? null : DyeColor.byId(savedColor));

            ILockable lockeable = (ILockable) level.getBlockEntity(pos);
            if (lockeable != null) {
                lockeable.setLockCode(StorageUtil.getCode(stack));
            }
        }

    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable StorageMaterial getStorageMaterial() {
        return material;
    }

    public StorageTypes<LockedShulkerBoxBlockEntity, LockedMaterialContainer> types() {
        return this.types;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LockedShulkerBoxBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        BlockEntity blockentity = worldIn.getBlockEntity(pos);
        return blockentity instanceof LockedShulkerBoxBlockEntity ? Shapes.create(((LockedShulkerBoxBlockEntity) blockentity).getBoundingBox(state)) : Shapes.block();
    }

    private static boolean canOpen(BlockState state, Level level, BlockPos pos, LockedShulkerBoxBlockEntity shulker) {
        if (shulker.getAnimationStatus() != AnimationStatus.CLOSED) {
            return true;
        } else {
            AABB aabb = Shulker.getProgressDeltaAabb(1.0F, state.getValue(FACING), 0.0F, 0.5F, Vec3.atBottomCenterOf(pos)).deflate(1.0E-6D);
            return level.noCollision(aabb);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel worldIn, BlockPos pos, boolean movedByPiston) {
        worldIn.updateNeighbourForOutputSignal(pos, this);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader worldIn, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack itemstack = super.getCloneItemStack(worldIn, pos, state, includeData);
        worldIn.getBlockEntity(pos, this.types.blockEntity().get()).ifPresent((shulkerBE) -> {
            itemstack.applyComponents(shulkerBE.collectComponents());
            NBTHelper.putInt(itemstack, "Color", shulkerBE.colorToSave());
            String lockCode = StorageUtil.getCode(shulkerBE);
            StorageUtil.writeCodeToStack(lockCode, itemstack);
        });
        return itemstack;
    }

    @Override
    public BlockState playerWillDestroy(Level worldIn, BlockPos pos, BlockState state, Player player) {
        BlockEntity tileentity = worldIn.getBlockEntity(pos);
        if (tileentity instanceof LockedShulkerBoxBlockEntity shulkerBE) {
            if (!worldIn.isClientSide() && player.isCreative() && (!shulkerBE.getItemStackStorageHandler().isEmpty() || shulkerBE.isLocked())) {
                ItemStack itemstack = new ItemStack(this);
                itemstack.applyComponents(tileentity.collectComponents());
                NBTHelper.putInt(itemstack, "Color", shulkerBE.colorToSave());

                String lockCode = StorageUtil.getCode(shulkerBE);
                StorageUtil.writeCodeToStack(lockCode, itemstack);

                ItemEntity itementity = new ItemEntity(worldIn, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, itemstack);
                itementity.setDefaultPickUpDelay();
                worldIn.addFreshEntity(itementity);
            }
        }

        return super.playerWillDestroy(worldIn, pos, state, player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity tileentity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (tileentity instanceof LockedShulkerBoxBlockEntity shulkerBE) {
            builder = builder.withDynamicDrop(ShulkerBoxBlock.CONTENTS, (stackConsumer) -> {
                for (int i = 0; i < shulkerBE.getItemStackStorageHandler().getSlots(); ++i) {
                    stackConsumer.accept(shulkerBE.getItemStackStorageHandler().getStackInSlot(i).copy());
                }
            });
        }

        return super.getDrops(state, builder);
    }

    @Override
    @Nullable
    protected MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        BlockEntity tileentity = world.getBlockEntity(pos);
        return tileentity instanceof MenuProvider ? (MenuProvider) tileentity : null;
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level worldIn, BlockPos pos, int id, int param) {
        super.triggerEvent(state, worldIn, pos, id, param);
        BlockEntity tileentity = worldIn.getBlockEntity(pos);
        return tileentity == null ? false : tileentity.triggerEvent(id, param);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof BaseStorageBlockEntity storageBlockEntity) {
            return StorageUtil.getRedstoneSignalFromContainer(storageBlockEntity.getItemStackStorageHandler());
        }

        return super.getAnalogOutputSignal(state, level, pos, direction);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, this.types.blockEntity().get(), LockedShulkerBoxBlockEntity::tick);
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> entityType, BlockEntityType<E> p_152134_, BlockEntityTicker<? super E> entityTicker) {
        return p_152134_ == entityType ? (BlockEntityTicker<A>) entityTicker : null;
    }
}
