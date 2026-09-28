package com.grim3212.assorted.lib.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ItemUtil {

    /** Whether no item is in {@code tag}, including when no pack defines it, as with Assorted Core's metals without Core. */
    public static boolean isTagEmpty(TagKey<Item> tag) {
        return !BuiltInRegistries.ITEM.getTagOrEmpty(tag).iterator().hasNext();
    }

    /**
     * Breaks a block as the player would. It drops nothing unless the main-hand item is the correct
     * tool for it.
     *
     * @return false if the player may not break it
     */
    public static boolean destroyBlock(BlockPos blockPos, Level level, Player player) {
        BlockState blockState = level.getBlockState(blockPos);
        if (!player.getMainHandItem().getItem().canDestroyBlock(player.getMainHandItem(), blockState, level, blockPos, player)) {
            return false;
        } else {
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            Block block = blockState.getBlock();

            if (block instanceof GameMasterBlock && !player.canUseGameMasterBlocks()) {
                level.sendBlockUpdated(blockPos, blockState, blockState, 3);
                return false;
            } else {
                if (player instanceof ServerPlayer serverPlayer) {
                    if (player.blockActionRestricted(level, blockPos, serverPlayer.gameMode.getGameModeForPlayer())) {
                        return false;
                    }
                } else {
                    if (player.isSpectator()) {
                        return false;
                    }
                }

                block.playerWillDestroy(level, blockPos, blockState, player);
                boolean flag = level.removeBlock(blockPos, false);
                if (flag) {
                    block.destroy(level, blockPos, blockState);
                }

                if (player.isCreative()) {
                    return true;
                } else {
                    ItemStack mainHandItem = player.getMainHandItem();
                    ItemStack mainHandCopy = mainHandItem.copy();
                    // NeoForge deprecates this in favour of a level/pos aware overload that only exists in
                    // its patched jar; vanilla only has the state based one, which is what this builds against.
                    @SuppressWarnings("deprecation")
                    boolean hasCorrectToolForDrops = player.hasCorrectToolForDrops(blockState);
                    mainHandItem.mineBlock(level, blockState, blockPos, player);
                    if (flag && hasCorrectToolForDrops) {
                        block.playerDestroy(level, player, blockPos, blockState, blockEntity, mainHandCopy);
                    }

                    return true;
                }
            }
        }
    }

}
