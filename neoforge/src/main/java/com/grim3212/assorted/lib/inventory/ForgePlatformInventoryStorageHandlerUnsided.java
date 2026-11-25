package com.grim3212.assorted.lib.inventory;

import com.grim3212.assorted.lib.core.inventory.IItemStorageHandler;
import com.grim3212.assorted.lib.core.inventory.IPlatformInventoryStorageHandler;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * See {@link ForgePlatformInventoryStorageHandlerSided}. The wrapper is created eagerly and
 * {@link #invalidate()} only drops the cached instance.
 */
public class ForgePlatformInventoryStorageHandlerUnsided implements IPlatformInventoryStorageHandler {

    private final IItemStorageHandler handler;
    private IItemHandler itemHandler;

    public ForgePlatformInventoryStorageHandlerUnsided(IItemStorageHandler handler) {
        this.handler = handler;
        this.itemHandler = new ForgeItemStorageHandler(handler);
    }

    public IItemHandler getCapability() {
        if (this.itemHandler == null) {
            this.itemHandler = new ForgeItemStorageHandler(this.handler);
        }
        return this.itemHandler;
    }

    @Override
    public void invalidate() {
        this.itemHandler = null;
    }

    @Override
    public IItemStorageHandler getItemStorageHandler(@Nullable Direction direction) {
        return this.handler;
    }

}
