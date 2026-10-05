package com.grim3212.assorted.lib.core.tool;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Maps a configured numeric harvest level to the block tag a tool cannot get drops from. Levels above
 * netherite map to netherite's tag, the strongest vanilla has.
 */
public final class HarvestTiers {

    private HarvestTiers() {
    }

    public static TagKey<Block> incorrectBlocksForDrops(int harvestLevel) {
        return switch (Math.max(0, harvestLevel)) {
            case 0 -> BlockTags.INCORRECT_FOR_WOODEN_TOOL;
            case 1 -> BlockTags.INCORRECT_FOR_STONE_TOOL;
            case 2 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
            case 3 -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
            default -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        };
    }

    /** The inverse, to seed a config default from a vanilla material, which no longer carries a number. */
    public static int harvestLevelOf(TagKey<Block> incorrectBlocksForDrops) {
        if (incorrectBlocksForDrops == BlockTags.INCORRECT_FOR_WOODEN_TOOL) return 0;
        // Gold gets drops from iron-level blocks yet breaks like wood, so it sits off the ladder; 0 is what it mined in 1.20.1.
        if (incorrectBlocksForDrops == BlockTags.INCORRECT_FOR_GOLD_TOOL) return 0;
        if (incorrectBlocksForDrops == BlockTags.INCORRECT_FOR_STONE_TOOL) return 1;
        if (incorrectBlocksForDrops == BlockTags.INCORRECT_FOR_COPPER_TOOL) return 1;
        if (incorrectBlocksForDrops == BlockTags.INCORRECT_FOR_IRON_TOOL) return 2;
        if (incorrectBlocksForDrops == BlockTags.INCORRECT_FOR_DIAMOND_TOOL) return 3;
        if (incorrectBlocksForDrops == BlockTags.INCORRECT_FOR_NETHERITE_TOOL) return 4;
        return 0;
    }
}
