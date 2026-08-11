package dev.karn.karnmining.automation;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Tick-budgeted A* pathfinder over a bounded 3D box around the player.
 *
 * <p>It deliberately attempts a no-mining route first; only when that fails
 * does it consider breakable blocks as route cost, preferring cheap blocks
 * (dirt, stone) over expensive ones so KarnMining avoids unnecessary
 * destruction. The search is spread across ticks through {@link #step}.
 */
final class PathfinderJob {
    private static final Direction[] HORIZONTAL = {
        Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private final ClientWorld world;
    private final ClientPlayerEntity player;
    private final BlockPos start;
    private final BlockPos target;
    private final Set<Long> goals = new HashSet<>();
    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;
    private final int minZ;
    private final int maxZ;

    private final Map<Long, Node> nodes = new HashMap<>();
    private final PriorityQueue<QueueEntry> open = new PriorityQueue<>();

    private boolean allowMining;
    private int expanded;
    private boolean done;
    private boolean failed;
    private PathResult result;

    PathfinderJob(ClientWorld world, ClientPlayerEntity player, BlockPos start, BlockPos target) {
        this.world = world;
        this.player = player;
        this.start = start.toImmutable();
        this.target = target.toImmutable();

        int margin = 14;
        this.minX = Math.min(start.getX(), target.getX()) - margin;
        this.maxX = Math.max(start.getX(), target.getX()) + margin;
        this.minZ = Math.min(start.getZ(), target.getZ()) - margin;
        this.maxZ = Math.max(start.getZ(), target.getZ()) + margin;
        int worldBottom = world.getBottomY();
        int worldTop = worldBottom + world.getHeight() - 1;
        this.minY = Math.max(worldBottom, Math.min(start.getY(), target.getY()) - 14);
        this.maxY = Math.min(worldTop - 1, Math.max(start.getY(), target.getY()) + 14);

        buildGoals();
        beginPhase(false);
    }

    void step(int budget) {
        if (done) {
            return;
        }

        int worked = 0;
        int phaseLimit = allowMining ? 22_000 : 30_000;
        while (worked < budget && !done) {
            if (open.isEmpty() || expanded >= phaseLimit) {
                if (!allowMining) {
                    beginPhase(true);
                    return;
                }
                done = true;
                failed = true;
                return;
            }

            QueueEntry entry = open.poll();
            Node current = nodes.get(entry.position());
            if (current == null || current.closed || Math.abs(current.g - entry.g()) > 0.0001) {
                continue;
            }

            current.closed = true;
            expanded++;
            worked++;

            if (goals.contains(current.position)) {
                result = new PathResult(reconstruct(current), allowMining);
                done = true;
                return;
            }

            expandHorizontal(current);
            consider(current, BlockPos.fromLong(current.position).down());
        }
    }

    boolean isDone() {
        return done;
    }

    boolean hasFailed() {
        return failed;
    }

    PathResult getResult() {
        return result;
    }

    boolean isMiningPhase() {
        return allowMining;
    }

    int getExpanded() {
        return expanded;
    }

    private void beginPhase(boolean mining) {
        allowMining = mining;
        expanded = 0;
        nodes.clear();
        open.clear();
        Node first = new Node(start.asLong(), 0.0, heuristic(start), Long.MIN_VALUE);
        nodes.put(first.position, first);
        open.add(new QueueEntry(first.position, first.g, first.f));
    }

    private void buildGoals() {
        for (Direction direction : HORIZONTAL) {
            BlockPos side = target.offset(direction);
            for (int dy = -1; dy <= 1; dy++) {
                goals.add(side.up(dy).asLong());
            }
        }
        goals.add(target.up().asLong());
        goals.add(target.down(2).asLong());
    }

    private void expandHorizontal(Node current) {
        BlockPos position = BlockPos.fromLong(current.position);
        for (Direction direction : HORIZONTAL) {
            BlockPos side = position.offset(direction);
            consider(current, side.up());
            consider(current, side);
            consider(current, side.down());
            consider(current, side.down(2));
            consider(current, side.down(3));
        }
    }

    private void consider(Node from, BlockPos to) {
        if (!insideBounds(to) || !world.isChunkLoaded(to.getX() >> 4, to.getZ() >> 4)) {
            return;
        }

        BlockPos fromPos = BlockPos.fromLong(from.position);
        int dy = to.getY() - fromPos.getY();
        int horizontalDistance = Math.abs(to.getX() - fromPos.getX()) + Math.abs(to.getZ() - fromPos.getZ());
        if (horizontalDistance > 1 || dy > 1 || dy < -3 || !canOccupy(to)) {
            return;
        }

        // A jump needs room above the current head. A drop needs a clear
        // vertical column at the destination so it cannot skip through rock.
        if (dy > 0 && !canClear(fromPos.up(2))) {
            return;
        }
        if (dy < -1 && horizontalDistance > 0) {
            for (int y = to.getY() + 1; y <= fromPos.getY(); y++) {
                if (!canClear(new BlockPos(to.getX(), y, to.getZ()))) {
                    return;
                }
            }
        }

        double movementCost = horizontalDistance == 0 ? 8.0 : 10.0;
        if (dy > 0) {
            movementCost += 6.0;
        } else if (dy < 0) {
            movementCost += 2.0 * Math.abs(dy);
        }
        double miningCost = miningCost(to);
        if (allowMining && dy > 0) {
            miningCost += costToClear(fromPos.up(2));
        }
        if (allowMining && dy < -1 && horizontalDistance > 0) {
            for (int y = to.getY() + 2; y <= fromPos.getY(); y++) {
                miningCost += costToClear(new BlockPos(to.getX(), y, to.getZ()));
            }
        }
        double tentative = from.g + movementCost + miningCost;
        long key = to.asLong();
        Node known = nodes.get(key);
        if (known != null && tentative >= known.g - 0.0001) {
            return;
        }

        double f = tentative + heuristic(to);
        if (known == null) {
            known = new Node(key, tentative, f, from.position);
            nodes.put(key, known);
        } else {
            known.g = tentative;
            known.f = f;
            known.parent = from.position;
            known.closed = false;
        }
        open.add(new QueueEntry(key, tentative, f));
    }

    private boolean insideBounds(BlockPos position) {
        return position.getX() >= minX && position.getX() <= maxX
            && position.getY() >= minY && position.getY() <= maxY
            && position.getZ() >= minZ && position.getZ() <= maxZ;
    }

    private boolean canOccupy(BlockPos feet) {
        if (!canClear(feet) || !canClear(feet.up())) {
            return false;
        }
        BlockState support = world.getBlockState(feet.down());
        return support.getFluidState().isEmpty()
            && !support.getCollisionShape(world, feet.down()).isEmpty();
    }

    private boolean canClear(BlockPos position) {
        BlockState state = world.getBlockState(position);
        if (state.getCollisionShape(world, position).isEmpty() && state.getFluidState().isEmpty()) {
            return true;
        }
        return allowMining && canMine(position, state);
    }

    private boolean canMine(BlockPos position, BlockState state) {
        return !position.equals(target)
            && state.getFluidState().isEmpty()
            && state.getHardness(world, position) >= 0.0F;
    }

    private double miningCost(BlockPos feet) {
        if (!allowMining) {
            return 0.0;
        }
        return costToClear(feet) + costToClear(feet.up());
    }

    private double costToClear(BlockPos position) {
        BlockState state = world.getBlockState(position);
        if (state.getCollisionShape(world, position).isEmpty() && state.getFluidState().isEmpty()) {
            return 0.0;
        }
        float delta = state.calcBlockBreakingDelta(player, world, position);
        if (delta <= 0.0F) {
            return 4_000.0;
        }
        // Mining time is represented in movement-cost units. Expensive blocks
        // are avoided when a faster tunnel is available.
        return Math.min(4_000.0, Math.ceil(1.0 / delta) * 10.0);
    }

    private double heuristic(BlockPos position) {
        int dx = Math.abs(position.getX() - target.getX());
        int dy = Math.abs(position.getY() - target.getY());
        int dz = Math.abs(position.getZ() - target.getZ());
        return Math.max(0, dx + dy + dz - 1) * 8.0;
    }

    private List<BlockPos> reconstruct(Node end) {
        List<BlockPos> reverse = new ArrayList<>();
        Node current = end;
        while (current != null) {
            reverse.add(BlockPos.fromLong(current.position).toImmutable());
            current = current.parent == Long.MIN_VALUE ? null : nodes.get(current.parent);
        }
        Collections.reverse(reverse);
        return reverse;
    }

    record PathResult(List<BlockPos> positions, boolean usesMining) {
    }

    private record QueueEntry(long position, double g, double f) implements Comparable<QueueEntry> {
        @Override
        public int compareTo(QueueEntry other) {
            int byF = Double.compare(f, other.f);
            return byF != 0 ? byF : Double.compare(g, other.g);
        }
    }

    private static final class Node {
        private final long position;
        private double g;
        private double f;
        private long parent;
        private boolean closed;

        private Node(long position, double g, double f, long parent) {
            this.position = position;
            this.g = g;
            this.f = f;
            this.parent = parent;
        }
    }
}
