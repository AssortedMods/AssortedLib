package com.grim3212.assorted.lib.core.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * How each mod's containers upgrade from one {@link StorageMaterial} to the next. A level upgrade
 * asks here, so it works on the containers of whichever mods are installed.
 */
public class LevelUpgrades {

    private static final List<LevelUpgrade> UPGRADES = new CopyOnWriteArrayList<>();

    /** Asked in the order registered, so a block that extends a vanilla one registers before it. */
    public static void register(LevelUpgrade upgrade) {
        UPGRADES.add(upgrade);
    }

    /** The first registered upgrade for the block at {@code pos}, or null when none can take it. */
    @Nullable
    public static PreparedLevelUpgrade prepare(Level level, BlockPos pos, Player player, StorageMaterial material) {
        BlockState state = level.getBlockState(pos);
        for (LevelUpgrade upgrade : UPGRADES) {
            PreparedLevelUpgrade prepared = upgrade.prepare(level, pos, state, player, material);
            if (prepared != null) {
                return prepared;
            }
        }

        return null;
    }

    /** Whether something made of {@code current}, or of nothing yet, takes the next step up to {@code target}. */
    public static boolean canUpgrade(@Nullable StorageMaterial current, StorageMaterial target) {
        boolean startingUpgrade = target.getStorageLevel() == 0 || target.getStorageLevel() == 1;
        int currentLevel = current != null ? current.getStorageLevel() : 0;
        return (startingUpgrade && (current == null || current.getStorageLevel() == 0)) || currentLevel == target.getStorageLevel() - 1;
    }

    /** Carries over everything a storage block entity holds. */
    public static PreparedLevelUpgrade from(BaseStorageBlockEntity previous, BlockState state, BlockEntity blockEntity) {
        return new PreparedLevelUpgrade(state, blockEntity, previous.getLockCode(), previous.getCustomName(), previous.getItemStackStorageHandler().getStacks());
    }

    /** Carries over a vanilla container's name and a copy of its contents. */
    public static PreparedLevelUpgrade from(BaseContainerBlockEntity previous, BlockState state, BlockEntity blockEntity) {
        NonNullList<ItemStack> items = NonNullList.withSize(previous.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < previous.getContainerSize(); slot++) {
            items.set(slot, previous.getItem(slot).copy());
        }

        return new PreparedLevelUpgrade(state, blockEntity, null, previous.getCustomName(), items);
    }

    /** Swaps the block for the upgraded one; false if the new block entity is not a storage one and took nothing over. */
    public static boolean apply(Level level, BlockPos pos, PreparedLevelUpgrade upgrade) {
        level.removeBlockEntity(pos);
        level.removeBlock(pos, false);

        level.setBlock(pos, upgrade.state(), Block.UPDATE_ALL);
        level.setBlockEntity(upgrade.blockEntity());

        level.sendBlockUpdated(pos, upgrade.state(), upgrade.state(), Block.UPDATE_ALL);

        if (level.getBlockEntity(pos) instanceof BaseStorageBlockEntity storage) {
            if (upgrade.customName() != null) {
                storage.setCustomName(upgrade.customName());
            }

            storage.getItemStackStorageHandler().setStacks(upgrade.items());
            storage.setLockCode(upgrade.lockCode());
            return true;
        }

        return false;
    }
}
