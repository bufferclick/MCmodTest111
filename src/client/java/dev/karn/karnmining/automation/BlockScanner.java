package dev.karn.karnmining.automation;

import net.minecraft.block.Block;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Set;

/**
 * Incremental nearest-block scan. Offsets are sorted by squared distance, so
 * the first matching loaded block is guaranteed to be the nearest in range.
 */
final class BlockScanner {
    private static int cachedRadius = -1;
    private static long[] cachedOffsets = new long[0];

    private final BlockPos center;
    private final Block wanted;
    private final int radius;
    private final long[] offsets;
    private final Set<Long> ignoredPositions;
    private int cursor;
    private BlockPos result;
    private boolean done;

    BlockScanner(BlockPos center, Block wanted, int radius, Set<Long> ignoredPositions) {
        this.center = center.toImmutable();
        this.wanted = wanted;
        this.radius = radius;
        this.offsets = offsetsFor(radius);
        this.ignoredPositions = ignoredPositions;
    }

    void step(ClientWorld world, int budget) {
        if (done) {
            return;
        }

        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int bottom = world.getBottomY();
        int topExclusive = bottom + world.getHeight();
        int checked = 0;

        while (cursor < offsets.length && checked < budget) {
            long encoded = offsets[cursor++];
            int dx = (int) ((encoded >>> 16) & 0xFFL) - radius;
            int dy = (int) ((encoded >>> 8) & 0xFFL) - radius;
            int dz = (int) (encoded & 0xFFL) - radius;
            int x = center.getX() + dx;
            int y = center.getY() + dy;
            int z = center.getZ() + dz;
            checked++;

            if (y < bottom || y >= topExclusive || !world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }

            mutable.set(x, y, z);
            if (!ignoredPositions.contains(mutable.asLong()) && world.getBlockState(mutable).isOf(wanted)) {
                result = mutable.toImmutable();
                done = true;
                return;
            }
        }

        if (cursor >= offsets.length) {
            done = true;
        }
    }

    boolean isDone() {
        return done;
    }

    BlockPos getResult() {
        return result;
    }

    int getProgressPercent() {
        return offsets.length == 0 ? 100 : Math.min(100, cursor * 100 / offsets.length);
    }

    private static synchronized long[] offsetsFor(int radius) {
        if (cachedRadius == radius) {
            return cachedOffsets;
        }

        int diameter = radius * 2 + 1;
        int estimate = (int) Math.ceil(4.0 / 3.0 * Math.PI * radius * radius * radius) + 1;
        long[] values = new long[Math.max(estimate, diameter)];
        int size = 0;
        int radiusSquared = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distanceSquared = dx * dx + dy * dy + dz * dz;
                    if (distanceSquared > radiusSquared) {
                        continue;
                    }
                    if (size == values.length) {
                        values = Arrays.copyOf(values, values.length + Math.max(1024, values.length / 4));
                    }
                    values[size++] = ((long) distanceSquared << 24)
                        | ((long) (dx + radius) << 16)
                        | ((long) (dy + radius) << 8)
                        | (long) (dz + radius);
                }
            }
        }

        cachedOffsets = Arrays.copyOf(values, size);
        Arrays.sort(cachedOffsets);
        cachedRadius = radius;
        return cachedOffsets;
    }
}
