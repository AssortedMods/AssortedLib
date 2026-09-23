package com.grim3212.assorted.lib.spawn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A random height in the column, as vanilla's spawner picks: surface or cave, whichever the placement takes. Bounded by
 * the level's floor and the surface unless told otherwise. Most heights are inside stone, so give it tries.
 */
public record ColumnSite(Optional<Integer> minY, Optional<Integer> maxY, boolean belowSurface) implements SpawnSite {

    public static final MapCodec<ColumnSite> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("min_y").forGetter(ColumnSite::minY),
            Codec.INT.optionalFieldOf("max_y").forGetter(ColumnSite::maxY),
            Codec.BOOL.optionalFieldOf("below_surface", false).forGetter(ColumnSite::belowSurface)
    ).apply(instance, ColumnSite::new));

    public static final ColumnSite ANYWHERE = new ColumnSite(Optional.empty(), Optional.empty(), false);
    public static final ColumnSite UNDERGROUND = new ColumnSite(Optional.empty(), Optional.empty(), true);

    @Override
    public Identifier type() {
        return SpawnSites.COLUMN;
    }

    @Override
    public @Nullable BlockPos find(ServerLevelAccessor level, BlockPos column, RandomSource random) {
        // The block above the surface; the surface itself is one down, and a cave spot is below that.
        int top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).getY();
        int max = Math.min(this.maxY.orElse(top), this.belowSurface ? top - 2 : top);
        int min = Math.max(this.minY.orElse(level.getMinY()), level.getMinY());
        if (max < min) {
            return null;
        }
        return new BlockPos(column.getX(), min + random.nextInt(max - min + 1), column.getZ());
    }
}
