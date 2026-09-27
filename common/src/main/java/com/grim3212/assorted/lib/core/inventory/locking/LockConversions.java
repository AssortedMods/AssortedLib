package com.grim3212.assorted.lib.core.inventory.locking;

import com.grim3212.assorted.lib.core.storage.BaseStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * What a lock turns a block into. The mod that owns a locked version registers the block it stands
 * in for; the lock item looks here, so it never needs to know which mods are installed.
 */
public class LockConversions {

    private static final Map<Identifier, LockConversion> CONVERSIONS = new ConcurrentHashMap<>();

    /** Registers by id, so a block from a mod that may not be installed can still be named. */
    public static void register(Identifier block, LockConversion conversion) {
        CONVERSIONS.put(block, conversion);
    }

    public static void register(Block block, LockConversion conversion) {
        register(BuiltInRegistries.BLOCK.getKey(block), conversion);
    }

    @Nullable
    public static LockConversion get(BlockState state) {
        return CONVERSIONS.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    /** Locks the block at {@code pos} with {@code code}; false when nothing is registered for it or it refused. */
    public static boolean tryLock(Level level, BlockPos pos, String code) {
        BlockState state = level.getBlockState(pos);
        LockConversion conversion = get(state);
        return conversion != null && conversion.lock(level, pos, state, code);
    }

    /**
     * The usual conversion: empties a vanilla container, puts {@code locked} in its place with the
     * state {@code copyState} makes from the old one, and moves the contents and the lock across.
     */
    public static LockConversion container(Supplier<? extends Block> locked, BiFunction<BlockState, BlockState, BlockState> copyState) {
        return (level, pos, state, code) -> {
            if (!(level.getBlockEntity(pos) instanceof Container previous)) {
                return false;
            }

            NonNullList<ItemStack> items = NonNullList.withSize(previous.getContainerSize(), ItemStack.EMPTY);
            for (int i = 0; i < previous.getContainerSize(); i++) {
                items.set(i, previous.getItem(i).copy());
            }
            // Emptied first, or the old block drops its contents as it goes and they are duplicated
            previous.clearContent();

            level.setBlock(pos, copyState.apply(state, locked.get().defaultBlockState()), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof BaseStorageBlockEntity storage) {
                storage.setLockCode(code);
                storage.getItemStackStorageHandler().setStacks(items);
            }

            return true;
        };
    }
}
