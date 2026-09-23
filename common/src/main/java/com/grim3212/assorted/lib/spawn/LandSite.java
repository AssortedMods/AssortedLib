package com.grim3212.assorted.lib.spawn;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

/** The block above the ground by a heightmap, as the biome spawner places land creatures. Never a cave. */
public record LandSite(Heightmap.Types heightmap) implements SpawnSite {

    public static final MapCodec<LandSite> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Heightmap.Types.CODEC.optionalFieldOf("heightmap", Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).forGetter(LandSite::heightmap)
    ).apply(instance, LandSite::new));

    public static final LandSite DEFAULT = new LandSite(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);

    @Override
    public Identifier type() {
        return SpawnSites.LAND;
    }

    @Override
    public BlockPos find(ServerLevelAccessor level, BlockPos column, RandomSource random) {
        return level.getHeightmapPos(this.heightmap, column);
    }
}
