package com.grim3212.assorted.lib.spawn;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * Turns a column into the block a creature is set down at, or null: the ground, the water's top, or inside a
 * structure piece. Whether it may stand there is the spawn placement's question, asked afterwards. Types are
 * registered in {@link SpawnSites}, so a mod can add its own.
 */
public interface SpawnSite {

    Codec<SpawnSite> CODEC = Identifier.CODEC.dispatch("type", SpawnSite::type, SpawnSites::codec);

    Identifier type();

    /** False for a site that reads beyond the generating chunk, which a worldgen thread may not: no seeding for it. */
    default boolean seedsAtGeneration() {
        return true;
    }

    @Nullable
    BlockPos find(ServerLevelAccessor level, BlockPos column, RandomSource random);
}
