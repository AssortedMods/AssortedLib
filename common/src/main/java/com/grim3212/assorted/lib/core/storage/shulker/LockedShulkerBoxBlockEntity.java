package com.grim3212.assorted.lib.core.storage.shulker;

import com.grim3212.assorted.lib.core.storage.BaseStorageBlockEntity;
import com.grim3212.assorted.lib.core.storage.StorageMaterial;
import com.grim3212.assorted.lib.core.storage.LockedMaterialContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity.AnimationStatus;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import com.grim3212.assorted.lib.core.inventory.IMenuDataProvider;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;

/** The block entity of a {@link LockedShulkerBoxBlock}, of the type its block names. */
public class LockedShulkerBoxBlockEntity extends BaseStorageBlockEntity implements IMenuDataProvider<Optional<StorageMaterial>> {

    private final StorageMaterial storageMaterial;
    private AnimationStatus animationStatus = AnimationStatus.CLOSED;
    private float progress;
    private float progressOld;
    private DyeColor color = null;

    public LockedShulkerBoxBlockEntity(BlockPos pos, BlockState state) {
        super(block(state).types().blockEntity().get(), pos, state);
        this.storageMaterial = block(state).getStorageMaterial();
        this.setStorageHandler(new ShulkerItemStackStorageHandler(this, storageMaterial != null ? storageMaterial.totalItems() : 27));
    }

    private static LockedShulkerBoxBlock block(BlockState state) {
        return (LockedShulkerBoxBlock) state.getBlock();
    }

    public StorageMaterial getStorageMaterial() {
        return this.storageMaterial;
    }

    public int colorToSave() {
        return this.color == null ? -1 : this.color.getId();
    }

    public DyeColor colorFromInput(ValueInput input) {
        int color = input.getIntOr("Color", -1);
        return color == -1 ? null : DyeColor.byId(color);
    }

    public DyeColor getColor() {
        return color;
    }

    public void setColor(DyeColor color) {
        this.color = color;

        this.setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LockedShulkerBoxBlockEntity shulkerBE) {
        shulkerBE.updateAnimation(level, pos, state);
    }

    private void updateAnimation(Level level, BlockPos pos, BlockState state) {
        this.progressOld = this.progress;
        switch (this.animationStatus) {
            case CLOSED:
                this.progress = 0.0F;
                break;
            case OPENING:
                this.progress += 0.1F;
                if (this.progress >= 1.0F) {
                    this.animationStatus = AnimationStatus.OPENED;
                    this.progress = 1.0F;
                    doNeighborUpdates(level, pos, state);
                }

                this.moveCollidedEntities(level, pos, state);
                break;
            case CLOSING:
                this.progress -= 0.1F;
                if (this.progress <= 0.0F) {
                    this.animationStatus = AnimationStatus.CLOSED;
                    this.progress = 0.0F;
                    doNeighborUpdates(level, pos, state);
                }
                break;
            case OPENED:
                this.progress = 1.0F;
        }

    }

    public AnimationStatus getAnimationStatus() {
        return this.animationStatus;
    }

    private void moveCollidedEntities(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof LockedShulkerBoxBlock) {
            Direction direction = state.getValue(LockedShulkerBoxBlock.FACING);
            AABB aabb = Shulker.getProgressDeltaAabb(1.0F, direction, this.progressOld, this.progress, Vec3.atBottomCenterOf(pos));
            List<Entity> list = level.getEntities((Entity) null, aabb);
            if (!list.isEmpty()) {
                for (int i = 0; i < list.size(); ++i) {
                    Entity entity = list.get(i);
                    if (entity.getPistonPushReaction() != PushReaction.IGNORE) {
                        entity.move(MoverType.SHULKER_BOX, new Vec3((aabb.getXsize() + 0.01D) * (double) direction.getStepX(), (aabb.getYsize() + 0.01D) * (double) direction.getStepY(), (aabb.getZsize() + 0.01D) * (double) direction.getStepZ()));
                    }
                }

            }
        }
    }

    @Override
    public boolean triggerEvent(int p_59678_, int p_59679_) {
        if (p_59678_ == 1) {
            this.numPlayersUsing = p_59679_;
            if (p_59679_ == 0) {
                this.animationStatus = AnimationStatus.CLOSING;
                doNeighborUpdates(this.getLevel(), this.worldPosition, this.getBlockState());
            }

            if (p_59679_ == 1) {
                this.animationStatus = AnimationStatus.OPENING;
                doNeighborUpdates(this.getLevel(), this.worldPosition, this.getBlockState());
            }

            return true;
        } else {
            return super.triggerEvent(p_59678_, p_59679_);
        }
    }

    private static void doNeighborUpdates(Level level, BlockPos pos, BlockState state) {
        state.updateNeighbourShapes(level, pos, 3);
    }

    @Override
    protected Component getDefaultName() {
        return this.blockContainerName();
    }

    public AABB getBoundingBox(BlockState state) {
        return Shulker.getProgressAabb(1.0F, state.getValue(LockedShulkerBoxBlock.FACING), 0.5F * this.getProgress(1.0F), new Vec3(0.5D, 0.0D, 0.5D));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.color = colorFromInput(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Color", this.colorToSave());
    }

    public float getProgress(float p_59658_) {
        return Mth.lerp(p_59658_, this.progressOld, this.progress);
    }

    public boolean isClosed() {
        return this.animationStatus == AnimationStatus.CLOSED;
    }

    @Override
    public Optional<StorageMaterial> getMenuData(ServerPlayer player) {
        return Optional.ofNullable(this.storageMaterial);
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory player, Player playerEntity) {
        return new LockedMaterialContainer(block(this.getBlockState()).types().menu().get(), windowId, player, this.getItemStackStorageHandler(), storageMaterial, true);
    }


    /**
     * The color rides along with the lock in the item's custom data, which is what the item's own
     * name and the block's setPlacedBy read back.
     */
    @Override
    protected void writeCustomData(CompoundTag tag) {
        super.writeCustomData(tag);
        tag.putInt("Color", this.colorToSave());
    }

    @Override
    protected void readCustomData(CompoundTag tag) {
        super.readCustomData(tag);
        int savedColor = tag.getIntOr("Color", -1);
        this.color = savedColor == -1 ? null : DyeColor.byId(savedColor);
    }

    /**
     * The dropped item carries the contents and the lock as data components, so nothing is dropped loose on
     * removal.
     */
    @Override
    protected boolean shouldDropContents() {
        return false;
    }

    @Override
    protected boolean shouldDropLock(BlockPos pos, BlockState state) {
        return false;
    }
}
