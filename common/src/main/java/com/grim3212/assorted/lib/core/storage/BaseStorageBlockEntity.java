package com.grim3212.assorted.lib.core.storage;

import com.grim3212.assorted.lib.core.inventory.IInventoryBlockEntity;
import com.grim3212.assorted.lib.core.inventory.INamed;
import com.grim3212.assorted.lib.core.inventory.IPlatformInventoryStorageHandler;
import com.grim3212.assorted.lib.core.inventory.impl.LockedItemStackStorageHandler;
import com.grim3212.assorted.lib.core.inventory.locking.ILockable;
import com.grim3212.assorted.lib.core.inventory.locking.LockItems;
import com.grim3212.assorted.lib.platform.ClientServices;
import com.grim3212.assorted.lib.platform.Services;
import com.grim3212.assorted.lib.core.inventory.locking.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** A storage block entity: a lockable, nameable inventory with a lid that opens while players use it. */
public abstract class BaseStorageBlockEntity extends BlockEntity implements MenuProvider, INamed, IStorage, ILockable, IInventoryBlockEntity {

    public int numPlayersUsing;
    private int ticksSinceSync;
    protected float rotation;
    protected float prevRotation;
    protected String lockCode = "";
    protected Component customName;
    protected IPlatformInventoryStorageHandler platformInventoryStorageHandler;
    private LockedItemStackStorageHandler storageHandler;

    protected BaseStorageBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        this(typeIn, pos, state, 27);
    }

    protected BaseStorageBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state, int inventorySize) {
        super(typeIn, pos, state);

        this.storageHandler = new StorageItemStackStorageHandler(this, inventorySize);
    }

    @Override
    public IPlatformInventoryStorageHandler getStorageHandler() {
        if (this.platformInventoryStorageHandler == null) {
            this.platformInventoryStorageHandler = this.createStorageHandler();
        }

        return this.platformInventoryStorageHandler;
    }

    public IPlatformInventoryStorageHandler createStorageHandler() {
        return Services.INVENTORY.createStorageInventoryHandler(this.storageHandler);
    }

    public LockedItemStackStorageHandler getItemStackStorageHandler() {
        return this.storageHandler;
    }

    public void setStorageHandler(LockedItemStackStorageHandler storageHandler) {
        this.storageHandler = storageHandler;
    }

    @Override
    public boolean isLocked() {
        return this.lockCode != null && !this.lockCode.isEmpty();
    }

    @Override
    public String getLockCode() {
        return this.lockCode;
    }

    @Override
    public void setLockCode(String s) {
        if (s == null || s.isEmpty())
            this.lockCode = "";
        else
            this.lockCode = s;

        this.setChanged();
        this.modelDataUpdate();
    }

    /**
     * Re-renders the lock. On a server the block update is what sends the new lock, without which a
     * block someone else locked kept looking unlocked to every other player.
     */
    protected void modelDataUpdate() {
        Level level = this.getLevel();
        if (level == null) {
            return;
        }

        if (level.isClientSide()) {
            ClientServices.MODELS.requestModelDataRefresh(this);
        }

        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        if (this.selfInventory()) {
            input.child("Inventory").ifPresent(this.storageHandler::deserialize);
        }

        this.customName = parseCustomNameSafe(input, "CustomName");
        this.lockCode = StorageUtil.readLock(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.selfInventory()) {
            this.storageHandler.serialize(output.child("Inventory"));
        }

        output.storeNullable("CustomName", ComponentSerialization.CODEC, this.customName);
        StorageUtil.writeLock(output, this.lockCode);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    protected boolean selfInventory() {
        return true;
    }

    /**
     * Drops the lock and the contents when the block is removed. It has to happen here: the block
     * entity is already gone by {@code affectNeighborsAfterRemoval}.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);

        if (this.level == null) {
            return;
        }

        if (this.shouldDropLock(pos, state)) {
            Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), LockItems.createLock(this.lockCode));
        }

        if (this.shouldDropContents()) {
            StorageUtil.dropContents(this.level, pos, this.getItemStackStorageHandler());
        }
    }

    protected boolean shouldDropLock(BlockPos pos, BlockState state) {
        return this.isLocked();
    }

    protected boolean shouldDropContents() {
        return this.selfInventory();
    }

    /**
     * Hands the stack the block entity's contents, name and lock so a picked or creative-dropped
     * item keeps them. Replaces the old {@code saveToItem} / {@code BlockEntityTag} round trip.
     */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CUSTOM_NAME, this.customName);
        if (this.selfInventory()) {
            components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItemStackStorageHandler().getStacks()));
        }
        CustomData customData = CustomData.EMPTY.update(this::writeCustomData);
        if (!customData.isEmpty()) {
            components.set(DataComponents.CUSTOM_DATA, customData);
        }
    }

    /**
     * The free form data that rides along on the dropped or picked item. This is the lock code for
     * every storage block; subclasses add to it.
     */
    protected void writeCustomData(CompoundTag tag) {
        StorageUtil.writeLock(tag, this.lockCode);
    }

    protected void readCustomData(CompoundTag tag) {
        this.lockCode = StorageUtil.readLock(tag);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.customName = components.get(DataComponents.CUSTOM_NAME);
        if (this.selfInventory()) {
            components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.getItemStackStorageHandler().getStacks());
        }
        this.readCustomData(components.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
    }

    protected abstract Component getDefaultName();

    /** {@code <namespace>.container.<path>} of this block's id, so each mod names the blocks it registered. */
    protected Component blockContainerName() {
        Identifier id = BuiltInRegistries.BLOCK.getKey(this.getBlockState().getBlock());
        return Component.translatable(id.getNamespace() + ".container." + id.getPath());
    }

    protected SoundEvent openSound() {
        return SoundEvents.CHEST_OPEN;
    }

    protected SoundEvent closeSound() {
        return SoundEvents.CHEST_CLOSE;
    }

    public void tick() {
        int i = this.worldPosition.getX();
        int j = this.worldPosition.getY();
        int k = this.worldPosition.getZ();
        ++this.ticksSinceSync;
        this.numPlayersUsing = getNumberOfPlayersUsing(this.level, this, this.ticksSinceSync, i, j, k, this.numPlayersUsing);
        this.prevRotation = this.rotation;
        if (this.numPlayersUsing > 0 && this.rotation == 0.0F) {
            this.playSound(openSound());
        }

        if (this.numPlayersUsing == 0 && this.rotation > 0.0F || this.numPlayersUsing > 0 && this.rotation < 1.0F) {
            float f1 = this.rotation;
            if (this.numPlayersUsing > 0) {
                this.rotation += 0.1F;
            } else {
                this.rotation -= 0.1F;
            }

            if (this.rotation > 1.0F) {
                this.rotation = 1.0F;
            }

            if (this.rotation < 0.5F && f1 >= 0.5F) {
                this.playSound(closeSound());
            }

            if (this.rotation < 0.0F) {
                this.rotation = 0.0F;
            }
        }
    }

    @Override
    public float getRotation(float partialTicks) {
        return Mth.lerp(partialTicks, this.prevRotation, this.rotation);
    }

    public int getNumberOfPlayersUsing(Level worldIn, BaseStorageBlockEntity lockableTileEntity, int ticksSinceSync, int x, int y, int z, int numPlayersUsing) {
        if (!worldIn.isClientSide() && numPlayersUsing != 0 && (ticksSinceSync + x + y + z) % 200 == 0) {
            numPlayersUsing = getNumberOfPlayersUsing(worldIn, lockableTileEntity, x, y, z);
        }

        return numPlayersUsing;
    }

    public int getNumberOfPlayersUsing(Level world, BaseStorageBlockEntity lockableTileEntity, int x, int y, int z) {
        int i = 0;

        for (Player playerentity : world.getEntitiesOfClass(Player.class, new AABB((double) ((float) x - 5.0F), (double) ((float) y - 5.0F), (double) ((float) z - 5.0F), (double) ((float) (x + 1) + 5.0F), (double) ((float) (y + 1) + 5.0F), (double) ((float) (z + 1) + 5.0F)))) {
            // This block, not any nearby storage menu: a row of lockers would hold each other open.
            if (playerentity.containerMenu instanceof IStorageMenu menu && menu.holdsOpen(lockableTileEntity)) {
                ++i;
            }
        }

        return i;
    }

    public int getNumberOfPlayersUsing(Level world, BaseStorageBlockEntity lockableTileEntity) {
        if (lockableTileEntity != null) {
            return this.getNumberOfPlayersUsing(world, lockableTileEntity, lockableTileEntity.worldPosition.getX(), lockableTileEntity.worldPosition.getY(), lockableTileEntity.worldPosition.getZ());
        }

        return 0;
    }

    private void playSound(SoundEvent soundIn) {
        double d0 = (double) this.worldPosition.getX() + 0.5D;
        double d1 = (double) this.worldPosition.getY() + 0.5D;
        double d2 = (double) this.worldPosition.getZ() + 0.5D;

        this.level.playSound((Player) null, d0, d1, d2, soundIn, SoundSource.BLOCKS, 0.5F, this.level.getRandom().nextFloat() * 0.1F + 0.9F);
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            this.numPlayersUsing = type;
            return true;
        } else {
            return super.triggerEvent(id, type);
        }
    }

    /** Tells the block its open count changed; a block that is not a {@link BaseStorageBlock} overrides this. */
    public void onOpenOrClose() {
        Block block = this.getBlockState().getBlock();

        if (block instanceof BaseStorageBlock) {
            this.level.blockEvent(this.worldPosition, block, 1, this.numPlayersUsing);
            this.level.updateNeighborsAt(this.worldPosition, block);
        }
    }

    /**
     * invalidates a tile entity
     */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.platformInventoryStorageHandler != null) {
            this.platformInventoryStorageHandler.invalidate();
        }
    }

    @Override
    public void setCustomName(Component name) {
        this.customName = name;
    }

    @Override
    public Component getName() {
        return this.customName != null ? this.customName : this.getDefaultName();
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    @Nullable
    public Component getCustomName() {
        return this.customName;
    }
}
