package com.grim3212.assorted.lib.inventory;

import com.grim3212.assorted.lib.core.inventory.IItemStorageHandler;
import com.grim3212.assorted.lib.core.inventory.IPlatformInventoryStorageHandler;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A capability lookup returns an {@link IItemHandler} or {@code null}, with no invalidation token,
 * so {@link #invalidate()} just clears the cache; a block entity that changes what it exposes calls
 * {@code level.invalidateCapabilities(pos)} itself.
 */
public class ForgePlatformInventoryStorageHandlerSided implements IPlatformInventoryStorageHandler {

    private final Function<Direction, IItemStorageHandler> handler;
    private final Map<Direction, IItemHandler> itemHandlers;

    public ForgePlatformInventoryStorageHandlerSided(Function<Direction, IItemStorageHandler> handler) {
        this.handler = handler;
        this.itemHandlers = new HashMap<>();
    }

    @Nullable
    public IItemHandler getCapability(@Nullable Direction direction) {
        if (!this.itemHandlers.containsKey(direction)) {
            IItemStorageHandler newHandler = this.handler.apply(direction);
            this.itemHandlers.put(direction, newHandler == null ? null : new ForgeItemStorageHandler(newHandler));
        }
        return this.itemHandlers.get(direction);
    }

    @Override
    public void invalidate() {
        this.itemHandlers.clear();
    }

    @Override
    public IItemStorageHandler getItemStorageHandler(@Nullable Direction direction) {
        return this.handler.apply(direction);
    }

}
