package dev.karn.karnmining.automation;

import dev.karn.karnmining.config.KarnMiningConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Coordinates scanning, pathfinding, movement, and vanilla block breaking.
 *
 * <p>The loop is: find the nearest block of the selected type, plan a route
 * (no-mining first, mining only when necessary), walk there, mine it, then
 * immediately search for the next one. Everything is budgeted per tick so the
 * mod stays lightweight, and it only ever uses the tool the player is holding.
 */
public final class AutomationController {
    private static final int SCAN_BUDGET_PER_TICK = 12_000;
    private static final int PATH_BUDGET_PER_TICK = 900;
    private static final int REPLAN_INTERVAL = 80;   // ticks between route rechecks while walking
    private static final int MAX_BREAK_TICKS = 1_200;

    private final KarnMiningConfig config;
    private final Set<Long> ignoredTargets = new HashSet<>();

    private boolean enabled;
    private boolean controllingMovement;
    private ClientWorld activeWorld;
    private Block selectedBlock;
    private BlockScanner scanner;
    private PathfinderJob pathfinder;
    private BlockPos target;
    private List<BlockPos> path;
    private boolean pathUsesMining;
    private int pathIndex;
    private BlockPos breakingPosition;
    private int breakingTicks;
    private int waitTicks;
    private int ticks;
    private int stuckTicks;
    private double bestStepDistance = Double.MAX_VALUE;
    private int replanTimer;
    private String status = "Disabled";

    public AutomationController(KarnMiningConfig config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Flips the enabled state. Returns false (and notifies the player) when
     * trying to enable without a selected block.
     */
    public boolean toggle(MinecraftClient client) {
        if (!enabled && config.getSelectedBlock().isEmpty()) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Choose a block in the KarnMining menu first."), false);
            }
            return false;
        }

        enabled = !enabled;
        if (enabled) {
            resetWork(client, "Starting...");
        } else {
            stop(client, "Disabled");
        }
        return true;
    }

    public void onConfigurationChanged(MinecraftClient client) {
        if (enabled) {
            resetWork(client, "Configuration changed; restarting...");
        }
    }

    public void tick(MinecraftClient client) {
        ticks++;
        if (!enabled) {
            return;
        }

        if (client.player == null || client.world == null || client.interactionManager == null) {
            enabled = false;
            stop(client, "Disabled");
            return;
        }

        if (client.player.isSpectator() || !client.player.isAlive()) {
            releaseMovement(client);
            status = "Paused (spectator or dead)";
            return;
        }

        if (client.currentScreen != null) {
            releaseMovement(client);
            status = "Paused while a menu is open";
            return;
        }

        Optional<Block> configured = config.getSelectedBlock();
        if (configured.isEmpty()) {
            enabled = false;
            stop(client, "No block selected");
            return;
        }
        if (client.world != activeWorld || configured.get() != selectedBlock) {
            resetWork(client, "Starting...");
        }

        if (waitTicks > 0) {
            releaseMovement(client);
            waitTicks--;
            if (waitTicks == 0) {
                beginScan(client);
            }
            return;
        }

        if (scanner != null) {
            tickScanner(client);
            return;
        }
        if (pathfinder != null) {
            tickPathfinder(client);
            return;
        }
        if (target == null) {
            beginScan(client);
            return;
        }

        if (!client.world.getBlockState(target).isOf(selectedBlock)) {
            finishTarget(client);
            return;
        }

        if (path == null || pathIndex >= path.size()) {
            tickTargetMining(client);
        } else {
            tickMovement(client);
        }
    }

    public String getStatus() {
        return status;
    }

    private void tickScanner(MinecraftClient client) {
        releaseMovement(client);
        scanner.step(client.world, SCAN_BUDGET_PER_TICK);
        status = "Searching loaded blocks... " + scanner.getProgressPercent() + "%";
        if (!scanner.isDone()) {
            return;
        }

        target = scanner.getResult();
        scanner = null;
        if (target == null) {
            if (!ignoredTargets.isEmpty()) {
                ignoredTargets.clear();
                status = "No reachable match; retrying all targets...";
                waitTicks = 80;
            } else {
                status = "No match within " + config.getSearchRadius() + " blocks; retrying...";
                waitTicks = 40;
            }
            return;
        }

        status = "Planning a walking route...";
        pathfinder = new PathfinderJob(client.world, client.player, client.player.getBlockPos(), target);
    }

    private void tickPathfinder(MinecraftClient client) {
        releaseMovement(client);
        pathfinder.step(PATH_BUDGET_PER_TICK);
        status = pathfinder.isMiningPhase()
            ? "Planning the quickest mining route..."
            : "Planning a no-mining route...";

        if (!pathfinder.isDone()) {
            return;
        }
        if (pathfinder.hasFailed() || pathfinder.getResult() == null) {
            pathfinder = null;
            skipTarget(client, "No safe route to that match; trying another...");
            return;
        }

        PathfinderJob.PathResult result = pathfinder.getResult();
        pathfinder = null;
        path = result.positions();
        pathUsesMining = result.usesMining();
        pathIndex = path.size() > 1 ? 1 : path.size();
        stuckTicks = 0;
        bestStepDistance = Double.MAX_VALUE;
        replanTimer = REPLAN_INTERVAL;
        status = pathUsesMining ? "Following mining route..." : "Moving to target...";
    }

    private void tickMovement(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BlockPos next = path.get(pathIndex);
        double dx = next.getX() + 0.5 - player.getX();
        double dy = next.getY() - player.getY();
        double dz = next.getZ() + 0.5 - player.getZ();
        double horizontalSquared = dx * dx + dz * dz;
        double distance = horizontalSquared + dy * dy;

        if (horizontalSquared < 0.38 && Math.abs(dy) < 0.75) {
            pathIndex++;
            stuckTicks = 0;
            bestStepDistance = Double.MAX_VALUE;
            releaseMovement(client);
            if (pathIndex >= path.size()) {
                status = "Mining target...";
            }
            return;
        }

        replanTimer--;
        if (replanTimer <= 0) {
            replanTimer = REPLAN_INTERVAL;
            replan(client);
            if (!enabled || path == null) {
                return;
            }
        }

        BlockPos obstruction = firstObstruction(client, next);
        if (obstruction != null) {
            releaseMovement(client);
            status = "Clearing route block...";
            MiningResult mining = mineBlock(client, obstruction, false);
            if (mining == MiningResult.COMPLETE) {
                stuckTicks = 0;
                bestStepDistance = Double.MAX_VALUE;
            } else if (mining == MiningResult.OUT_OF_REACH) {
                watchForStall(client, distance);
            } else if (mining == MiningResult.UNBREAKABLE) {
                skipTarget(client, "Route became blocked; trying another target...");
            }
            return;
        }

        clearBreaking(client);
        lookAt(client, next.getX() + 0.5, player.getEyeY(), next.getZ() + 0.5);
        client.options.forwardKey.setPressed(true);
        client.options.sprintKey.setPressed(true);
        client.options.jumpKey.setPressed(dy > 0.35 || player.horizontalCollision);
        player.setSprinting(true);
        controllingMovement = true;
        status = pathUsesMining ? "Following mining route..." : "Moving to target...";
        watchForStall(client, distance);
    }

    /**
     * Re-checks the route against the current world state. Adopts a new path
     * if a better one exists, or abandons the target when it is unreachable.
     */
    private void replan(MinecraftClient client) {
        if (target == null || !client.world.getBlockState(target).isOf(selectedBlock)) {
            finishTarget(client);
            return;
        }
        PathfinderJob quick = new PathfinderJob(client.world, client.player, client.player.getBlockPos(), target);
        quick.step(PATH_BUDGET_PER_TICK * 6);
        if (!quick.isDone()) {
            return; // keep the current path; retry later
        }
        if (quick.hasFailed() || quick.getResult() == null) {
            skipTarget(client, "Route became blocked; trying another target...");
            return;
        }
        PathfinderJob.PathResult result = quick.getResult();
        path = result.positions();
        pathUsesMining = result.usesMining();
        pathIndex = Math.min(path.size() - 1, 1);
        stuckTicks = 0;
        bestStepDistance = Double.MAX_VALUE;
    }

    private void tickTargetMining(MinecraftClient client) {
        releaseMovement(client);
        status = "Mining " + selectedBlock.getName().getString() + "...";
        MiningResult result = mineBlock(client, target, true);
        if (result == MiningResult.COMPLETE) {
            finishTarget(client);
        } else if (result == MiningResult.UNBREAKABLE) {
            skipTarget(client, "That match cannot be mined; trying another...");
        } else if (result == MiningResult.OUT_OF_REACH) {
            path = null;
            pathfinder = new PathfinderJob(client.world, client.player, client.player.getBlockPos(), target);
            status = "Target out of reach; replanning...";
        }
    }

    /**
     * Breaks {@code position} using the vanilla interaction manager. Returns
     * COMPLETE when the block is gone, UNBREAKABLE for fluid/unbreakable
     * blocks, OUT_OF_REACH when the player is too far, or IN_PROGRESS while
     * the block is being destroyed.
     */
    private MiningResult mineBlock(MinecraftClient client, BlockPos position, boolean targetBlock) {
        ClientWorld world = client.world;
        BlockState state = world.getBlockState(position);
        if (targetBlock ? !state.isOf(selectedBlock)
            : state.getCollisionShape(world, position).isEmpty() && state.getFluidState().isEmpty()) {
            clearBreaking(client);
            return MiningResult.COMPLETE;
        }
        if (!state.getFluidState().isEmpty() || state.getHardness(world, position) < 0.0F) {
            clearBreaking(client);
            return MiningResult.UNBREAKABLE;
        }

        Vec3d eyes = client.player.getEyePos();
        double centerX = position.getX() + 0.5;
        double centerY = position.getY() + 0.5;
        double centerZ = position.getZ() + 0.5;
        if (eyes.squaredDistanceTo(centerX, centerY, centerZ) > 25.0) {
            return MiningResult.OUT_OF_REACH;
        }

        lookAt(client, centerX, centerY, centerZ);
        Direction face = faceToward(eyes, position);
        if (!position.equals(breakingPosition)) {
            clearBreaking(client);
            breakingPosition = position.toImmutable();
            client.interactionManager.attackBlock(position, face);
        }
        breakingTicks++;
        if (breakingTicks > MAX_BREAK_TICKS) {
            clearBreaking(client);
            return MiningResult.UNBREAKABLE;
        }
        client.interactionManager.updateBlockBreakingProgress(position, face);
        if ((ticks & 3) == 0) {
            client.player.swingHand(Hand.MAIN_HAND);
        }
        return MiningResult.IN_PROGRESS;
    }

    private BlockPos firstObstruction(MinecraftClient client, BlockPos feet) {
        ClientWorld world = client.world;
        BlockPos current = client.player.getBlockPos();

        if (feet.getY() > current.getY()) {
            BlockPos overhead = current.up(2);
            if (isObstruction(world, overhead)) {
                return overhead;
            }
        }

        if (feet.getY() < current.getY()) {
            for (int y = current.getY(); y >= feet.getY(); y--) {
                BlockPos column = new BlockPos(feet.getX(), y, feet.getZ());
                if (isObstruction(world, column)) {
                    return column;
                }
            }
        }

        if (isObstruction(world, feet)) {
            return feet;
        }
        BlockPos head = feet.up();
        return isObstruction(world, head) ? head : null;
    }

    private static boolean isObstruction(ClientWorld world, BlockPos position) {
        BlockState state = world.getBlockState(position);
        return !state.getCollisionShape(world, position).isEmpty() || !state.getFluidState().isEmpty();
    }

    private void watchForStall(MinecraftClient client, double distance) {
        if (distance + 0.02 < bestStepDistance) {
            bestStepDistance = distance;
            stuckTicks = 0;
            return;
        }
        stuckTicks++;
        if (stuckTicks > 80) {
            clearBreaking(client);
            releaseMovement(client);
            path = null;
            pathfinder = new PathfinderJob(client.world, client.player, client.player.getBlockPos(), target);
            status = "Route blocked; replanning...";
            stuckTicks = 0;
            bestStepDistance = Double.MAX_VALUE;
        }
    }

    private void finishTarget(MinecraftClient client) {
        clearBreaking(client);
        releaseMovement(client);
        target = null;
        path = null;
        pathIndex = 0;
        status = "Looking for the next block...";
        waitTicks = 5;
    }

    private void skipTarget(MinecraftClient client, String newStatus) {
        if (target != null) {
            ignoredTargets.add(target.asLong());
        }
        clearBreaking(client);
        releaseMovement(client);
        target = null;
        path = null;
        pathIndex = 0;
        status = newStatus;
        waitTicks = 10;
    }

    private void beginScan(MinecraftClient client) {
        if (client.player == null || client.world == null || selectedBlock == null) {
            return;
        }
        target = null;
        path = null;
        pathfinder = null;
        scanner = new BlockScanner(client.player.getBlockPos(), selectedBlock, config.getSearchRadius(), ignoredTargets);
        status = "Searching loaded blocks... 0%";
    }

    private void resetWork(MinecraftClient client, String newStatus) {
        clearBreaking(client);
        releaseMovement(client);
        activeWorld = client.world;
        selectedBlock = config.getSelectedBlock().orElse(null);
        scanner = null;
        pathfinder = null;
        target = null;
        path = null;
        pathIndex = 0;
        waitTicks = 0;
        stuckTicks = 0;
        ignoredTargets.clear();
        bestStepDistance = Double.MAX_VALUE;
        replanTimer = REPLAN_INTERVAL;
        status = newStatus;
    }

    private void stop(MinecraftClient client, String newStatus) {
        clearBreaking(client);
        releaseMovement(client);
        scanner = null;
        pathfinder = null;
        target = null;
        path = null;
        ignoredTargets.clear();
        status = newStatus;
    }

    private void clearBreaking(MinecraftClient client) {
        if (breakingPosition != null && client.interactionManager != null) {
            client.interactionManager.cancelBlockBreaking();
        }
        breakingPosition = null;
        breakingTicks = 0;
    }

    private void releaseMovement(MinecraftClient client) {
        if (!controllingMovement) {
            return;
        }
        client.options.forwardKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        if (client.player != null) {
            client.player.setSprinting(false);
        }
        controllingMovement = false;
    }

    /**
     * Turns the player toward the given point and mirrors the look change to
     * the server so block breaking works correctly in multiplayer.
     */
    private void lookAt(MinecraftClient client, double x, double y, double z) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        double dx = x - player.getX();
        double dy = y - player.getEyeY();
        double dz = z - player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        player.setYaw(yaw);
        player.setPitch(Math.max(-90.0F, Math.min(90.0F, pitch)));
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                yaw, pitch, player.isOnGround(), player.horizontalCollision));
        }
    }

    /** Chooses the face of the block closest to the player's eyes. */
    private static Direction faceToward(Vec3d eyes, BlockPos position) {
        double dx = position.getX() + 0.5 - eyes.x;
        double dy = position.getY() + 0.5 - eyes.y;
        double dz = position.getZ() + 0.5 - eyes.z;
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);
        if (ax >= ay && ax >= az) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        if (ay >= az) {
            return dy > 0 ? Direction.DOWN : Direction.UP;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private enum MiningResult {
        COMPLETE,
        IN_PROGRESS,
        OUT_OF_REACH,
        UNBREAKABLE
    }
}
