package com.grim3212.assorted.lib.spawn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * A water block {@code depth} under the top of the column's water, which must be open to the air unless
 * {@code under_ice}: a narwhal under the frozen ocean. Null where the column is dry or too shallow.
 */
public record WaterSite(Span depth, boolean underIce) implements SpawnSite {

    public static final MapCodec<WaterSite> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Span.CODEC.optionalFieldOf("depth", new Span(0, 0)).forGetter(WaterSite::depth),
            Codec.BOOL.optionalFieldOf("under_ice", false).forGetter(WaterSite::underIce)
    ).apply(instance, WaterSite::new));

    public static final WaterSite SURFACE = new WaterSite(new Span(0, 0), false);

    @Override
    public Identifier type() {
        return SpawnSites.WATER;
    }

    @Override
    public @Nullable BlockPos find(ServerLevelAccessor level, BlockPos column, RandomSource random) {
        // MOTION_BLOCKING counts fluid, so below it is the top of the water, or the ice over it.
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column).below();
        if (this.underIce) {
            while (top.getY() > level.getMinY() && level.getBlockState(top).is(BlockTags.ICE)) {
                top = top.below();
            }
        }
        if (!level.getFluidState(top).is(FluidTags.WATER)) {
            return null;
        }
        BlockPos pos = top.below(this.depth.sample(random));
        return level.getFluidState(pos).is(FluidTags.WATER) ? pos : null;
    }
}
