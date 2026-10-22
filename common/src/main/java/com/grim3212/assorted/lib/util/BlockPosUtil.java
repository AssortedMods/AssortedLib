package com.grim3212.assorted.lib.util;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;

/** Block searches 26.3 took out of {@link BlockPos}, which now only searches a Manhattan diamond. */
public final class BlockPosUtil {

    private BlockPosUtil() {
    }

    /** Every position in the box {@code reach} out on each axis, nearest by Manhattan distance first. */
    public static Iterable<BlockPos> withinManhattan(BlockPos origin, int reachX, int reachY, int reachZ) {
        return () -> new Iterator<>() {
            // One Manhattan shell at a time, so a search that stops early never builds the rest of the box.
            private final Deque<BlockPos> shell = new ArrayDeque<>();
            private final int maxDepth = reachX + reachY + reachZ;
            private int depth = -1;

            private boolean fill() {
                while (this.shell.isEmpty() && this.depth < this.maxDepth) {
                    this.depth++;
                    for (int x = -Math.min(reachX, this.depth); x <= Math.min(reachX, this.depth); x++) {
                        int afterX = this.depth - Math.abs(x);
                        for (int y = -Math.min(reachY, afterX); y <= Math.min(reachY, afterX); y++) {
                            int z = afterX - Math.abs(y);
                            if (z <= reachZ) {
                                this.shell.add(origin.offset(x, y, z));
                                if (z != 0) {
                                    this.shell.add(origin.offset(x, y, -z));
                                }
                            }
                        }
                    }
                }
                return !this.shell.isEmpty();
            }

            @Override
            public boolean hasNext() {
                return this.fill();
            }

            @Override
            public BlockPos next() {
                if (!this.fill()) {
                    throw new NoSuchElementException();
                }
                return this.shell.poll();
            }
        };
    }

    /** The nearest position in the box that matches, as {@code BlockPos#findClosestMatch} found it. */
    public static Optional<BlockPos> findClosestMatch(BlockPos origin, int horizontalReach, int verticalReach, Predicate<BlockPos> predicate) {
        for (BlockPos pos : withinManhattan(origin, horizontalReach, verticalReach, horizontalReach)) {
            if (predicate.test(pos)) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }
}
