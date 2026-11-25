package com.grim3212.assorted.lib.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.world.phys.Vec3;

/**
 * A test helper whose positions count from the test box itself, as the 26.x line's do. 1.21.1 places a
 * test's structure one block above its structure block but still counts every position from the
 * structure block, so {@code (x, 1, z)} would be the box's floor instead of the first layer above it.
 * Every other position this helper hands out or reads goes through these four methods, so moving the
 * origin here moves it for the whole test; the shared test bodies and their positions stay unchanged.
 */
public class BoxOriginTestHelper extends GameTestHelper {

    public BoxOriginTestHelper(GameTestInfo info) {
        super(info);
    }

    @Override
    public BlockPos absolutePos(BlockPos relative) {
        return super.absolutePos(relative.above());
    }

    @Override
    public BlockPos relativePos(BlockPos absolute) {
        return super.relativePos(absolute).below();
    }

    @Override
    public Vec3 absoluteVec(Vec3 relative) {
        return super.absoluteVec(relative.add(0.0D, 1.0D, 0.0D));
    }

    @Override
    public Vec3 relativeVec(Vec3 absolute) {
        return super.relativeVec(absolute).subtract(0.0D, 1.0D, 0.0D);
    }
}
