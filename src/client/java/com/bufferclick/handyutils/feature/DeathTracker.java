package com.bufferclick.handyutils.feature;

import com.bufferclick.handyutils.config.HandyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Tracks player death position and notifies player.
 * Super useful - players constantly lose items after death, this keeps coords.
 */
public class DeathTracker {
    public static BlockPos lastDeathPos = null;
    public static String lastDeathDim = null;
    public static long lastDeathTime = 0;

    public static void onDeath(Minecraft client) {
        if (client.player == null) return;
        lastDeathPos = client.player.blockPosition();
        lastDeathDim = client.level != null ? client.level.dimension().location().toString() : "unknown";
        lastDeathTime = System.currentTimeMillis();

        String msg = String.format("§c☠ You died at §f%d, %d, %d §7in §f%s",
                lastDeathPos.getX(), lastDeathPos.getY(), lastDeathPos.getZ(), lastDeathDim);
        client.player.displayClientMessage(Component.literal("§e[HandyUtils] " + msg), false);
        client.gui.getChat().addMessage(Component.literal("§e[HandyUtils] §7Died at §f" + lastDeathPos.getX() + " " + lastDeathPos.getY() + " " + lastDeathPos.getZ() + " §7(" + lastDeathDim + ") - use /handy death to TP hint"));
    }

    public static Component getDeathMessage() {
        if (lastDeathPos == null) {
            return Component.literal("§e[HandyUtils] No death recorded yet.");
        }
        return Component.literal(String.format("§e[HandyUtils] §7Last death: §f%d %d %d §7in §f%s §7(%.1f min ago)",
                lastDeathPos.getX(), lastDeathPos.getY(), lastDeathPos.getZ(),
                lastDeathDim,
                (System.currentTimeMillis() - lastDeathTime) / 60000.0));
    }

    public static boolean hasDeath() {
        return lastDeathPos != null;
    }

    public static void tick(Minecraft client) {
        if (!HandyConfig.get().enableDeathTracker) return;
        if (client.player == null) return;
        // Detect death by health <= 0 and respawn screen? Simple: if health 0 and lastDeathTime not recent
        if (client.player.getHealth() <= 0.0f && client.player.isDeadOrDying()) {
            // Avoid spam: only if not recorded in last 5 seconds
            if (System.currentTimeMillis() - lastDeathTime > 5000) {
                onDeath(client);
            }
        }
    }
}
