package com.grim3212.assorted.lib.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * Helpers for free-form modded NBT, stored in the {@link DataComponents#CUSTOM_DATA} component.
 * {@link CustomData} is immutable, so every read returns a detached copy that has to be written
 * back through the matching put. Getters return their fallback without storing it, since writing a
 * component would stop otherwise identical stacks from stacking.
 */
public class NBTHelper {

    /** The stack's custom data as a detached tag; empty if it has none. */
    private static CompoundTag customData(ItemStack itemStack) {
        return itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** Applies {@code modifier} to a copy of the stack's custom data and stores the result back. */
    private static void updateCustomData(ItemStack itemStack, Consumer<CompoundTag> modifier) {
        CustomData.update(DataComponents.CUSTOM_DATA, itemStack, modifier);
    }

    public static boolean hasTag(ItemStack itemStack, String keyName) {
        return !itemStack.isEmpty() && customData(itemStack).contains(keyName);
    }

    public static void removeTag(ItemStack itemStack, String keyName) {
        updateCustomData(itemStack, (tag) -> tag.remove(keyName));
    }

    public static CompoundTag getTag(CompoundTag compound, String keyName) {
        if (compound == null) {
            return new CompoundTag();
        }

        return compound.getCompound(keyName);
    }

    public static CompoundTag getTag(ItemStack stack, String keyName) {
        return customData(stack).getCompound(keyName);
    }

    public static void putTag(ItemStack stack, String keyName, CompoundTag compound) {
        updateCustomData(stack, (tag) -> tag.put(keyName, compound));
    }

    // =============== STRING ===============
    public static String getString(CompoundTag compound, String keyName) {
        if (compound == null) {
            return "";
        }

        return compound.getString(keyName);
    }

    public static void putString(CompoundTag compound, String keyName, String keyValue) {
        if (compound == null) {
            return;
        }

        compound.putString(keyName, keyValue);
    }

    public static String getString(ItemStack itemStack, String keyName) {
        return customData(itemStack).getString(keyName);
    }

    public static void putString(ItemStack itemStack, String keyName, String keyValue) {
        updateCustomData(itemStack, (tag) -> tag.putString(keyName, keyValue));
    }

    public static ItemStack putStringItemStack(ItemStack itemStack, String keyName, String keyValue) {
        putString(itemStack, keyName, keyValue);

        return itemStack;
    }
    // =============== END STRING ===============

    // =============== BOOLEAN ===============
    public static boolean getBoolean(ItemStack itemStack, String keyName) {
        return customData(itemStack).getBoolean(keyName);
    }

    public static void putBoolean(ItemStack itemStack, String keyName, boolean keyValue) {
        updateCustomData(itemStack, (tag) -> tag.putBoolean(keyName, keyValue));
    }

    public static ItemStack putBooleanItemStack(ItemStack itemStack, String keyName, boolean keyValue) {
        putBoolean(itemStack, keyName, keyValue);

        return itemStack;
    }
    // =============== END BOOLEAN ===============

    // =============== BYTE ===============
    public static byte getByte(ItemStack itemStack, String keyName) {
        return customData(itemStack).getByte(keyName);
    }

    public static void putByte(ItemStack itemStack, String keyName, byte keyValue) {
        updateCustomData(itemStack, (tag) -> tag.putByte(keyName, keyValue));
    }

    public static ItemStack putByteItemStack(ItemStack itemStack, String keyName, byte keyValue) {
        putByte(itemStack, keyName, keyValue);

        return itemStack;
    }
    // =============== END BYTE ===============

    // =============== SHORT ===============
    public static short getShort(ItemStack itemStack, String keyName) {
        return customData(itemStack).getShort(keyName);
    }

    public static void putShort(ItemStack itemStack, String keyName, short keyValue) {
        updateCustomData(itemStack, (tag) -> tag.putShort(keyName, keyValue));
    }

    public static ItemStack putShortItemStack(ItemStack itemStack, String keyName, short keyValue) {
        putShort(itemStack, keyName, keyValue);

        return itemStack;
    }
    // =============== END SHORT ===============

    // =============== INTEGER ===============
    public static int getInt(CompoundTag compound, String keyName) {
        if (compound == null) {
            return 0;
        }

        return compound.getInt(keyName);
    }

    public static void putInt(CompoundTag compound, String keyName, int keyValue) {
        if (compound == null) {
            return;
        }

        compound.putInt(keyName, keyValue);
    }

    public static int getInt(ItemStack itemStack, String keyName) {
        return getInt(itemStack, keyName, 0);
    }

    public static int getInt(ItemStack itemStack, String keyName, int fallback) {
        CompoundTag tag = customData(itemStack);
        return tag.contains(keyName, Tag.TAG_ANY_NUMERIC) ? tag.getInt(keyName) : fallback;
    }

    public static void putInt(ItemStack itemStack, String keyName, int keyValue) {
        updateCustomData(itemStack, (tag) -> tag.putInt(keyName, keyValue));
    }

    public static ItemStack putIntItemStack(ItemStack itemStack, String keyName, int keyValue) {
        putInt(itemStack, keyName, keyValue);

        return itemStack;
    }
    // =============== END INTEGER ===============

    // =============== LONG ===============
    public static long getLong(CompoundTag compound, String keyName) {
        if (compound == null) {
            return 0;
        }

        return compound.getLong(keyName);
    }

    public static void putLong(CompoundTag compound, String keyName, long keyValue) {
        if (compound == null) {
            return;
        }

        compound.putLong(keyName, keyValue);
    }

    public static long getLong(ItemStack itemStack, String keyName) {
        return customData(itemStack).getLong(keyName);
    }

    public static void putLong(ItemStack itemStack, String keyName, long keyValue) {
        updateCustomData(itemStack, (tag) -> tag.putLong(keyName, keyValue));
    }

    public static ItemStack putLongItemStack(ItemStack itemStack, String keyName, long keyValue) {
        putLong(itemStack, keyName, keyValue);

        return itemStack;
    }
    // =============== END LONG ===============

    // =============== FLOAT ===============
    public static float getFloat(ItemStack itemStack, String keyName) {
        return customData(itemStack).getFloat(keyName);
    }

    public static void putFloat(ItemStack itemStack, String keyName, float keyValue) {
        updateCustomData(itemStack, (tag) -> tag.putFloat(keyName, keyValue));
    }

    public static ItemStack putFloatItemStack(ItemStack itemStack, String keyName, float keyValue) {
        putFloat(itemStack, keyName, keyValue);

        return itemStack;
    }
    // =============== END FLOAT ===============

    // =============== DOUBLE ===============
    public static double getDouble(ItemStack itemStack, String keyName) {
        return customData(itemStack).getDouble(keyName);
    }

    public static void putDouble(ItemStack itemStack, String keyName, double keyValue) {
        updateCustomData(itemStack, (tag) -> tag.putDouble(keyName, keyValue));
    }

    public static ItemStack putDoubleItemStack(ItemStack itemStack, String keyName, double keyValue) {
        putDouble(itemStack, keyName, keyValue);

        return itemStack;
    }
    // =============== END DOUBLE ===============

    // =============== BLOCK POS ===============
    public static BlockPos getBlockPos(ItemStack itemStack, String keyName) {
        return readBlockPos(customData(itemStack), keyName);
    }

    public static BlockPos getBlockPos(CompoundTag tag, String keyName) {
        if (tag == null) {
            return BlockPos.ZERO;
        }

        return readBlockPos(tag, keyName);
    }

    private static BlockPos readBlockPos(CompoundTag tag, String keyName) {
        int[] pos = tag.getIntArray(keyName);
        if (pos.length < 3) {
            return BlockPos.ZERO;
        }

        return new BlockPos(pos[0], pos[1], pos[2]);
    }

    public static void putBlockPos(CompoundTag tag, String keyName, BlockPos keyValue) {
        if (tag == null) {
            return;
        }

        tag.putIntArray(keyName, new int[]{keyValue.getX(), keyValue.getY(), keyValue.getZ()});
    }

    public static void putBlockPos(ItemStack itemStack, String keyName, BlockPos keyValue) {
        updateCustomData(itemStack, (tag) -> tag.putIntArray(keyName, new int[]{keyValue.getX(), keyValue.getY(), keyValue.getZ()}));
    }

    public static ItemStack putBlockPosItemStack(ItemStack itemStack, String keyName, BlockPos keyValue) {
        putBlockPos(itemStack, keyName, keyValue);

        return itemStack;
    }
    // =============== END BLOCK POS ===============
}
