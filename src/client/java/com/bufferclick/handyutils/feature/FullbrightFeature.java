package com.bufferclick.handyutils.feature;

import com.bufferclick.handyutils.HandyUtilsMod;
import com.bufferclick.handyutils.config.HandyConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.lwjgl.glfw.GLFW;

/**
 * Fullbright toggle - extremely useful for mining, building, caving.
 * Works by setting gamma high and also via LightTexture mixin for 1.21.11 where vanilla clamps gamma.
 */
public class FullbrightFeature {
    public static KeyMapping FULLBRIGHT_KEY;
    public static boolean enabled = false;
    public static double originalGamma = 0.5;

    public static void register() {
        FULLBRIGHT_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.handyutils.fullbright",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.handyutils"
        ));
        enabled = HandyConfig.get().enableFullbright;
    }

    public static void tick(Minecraft client) {
        if (FULLBRIGHT_KEY != null && FULLBRIGHT_KEY.consumeClick()) {
            toggle(client);
        }
        // Apply gamma constantly while enabled (vanilla may clamp in file but not in memory)
        if (enabled) {
            applyFullbright(client);
        }
    }

    public static void toggle(Minecraft client) {
        enabled = !enabled;
        HandyConfig.get().enableFullbright = enabled;
        HandyConfig.save();
        
        if (enabled) {
            // Save original gamma
            try {
                Options opts = client.options;
                // In Mojang mappings, gamma is OptionInstance<Double> at method gamma()
                originalGamma = opts.gamma().get();
                HandyConfig.get().defaultGamma = originalGamma;
            } catch (Exception e) {
                originalGamma = 0.5;
            }
            applyFullbright(client);
            if (client.player != null) {
                client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§e[HandyUtils] §aFullbright §aON§r §7(Gamma boosted)"), true);
            }
        } else {
            restoreGamma(client);
            if (client.player != null) {
                client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§e[HandyUtils] §cFullbright OFF"), true);
            }
        }
        HandyUtilsMod.LOGGER.info("[HandyUtils] Fullbright toggled: {}", enabled);
    }

    private static void applyFullbright(Minecraft client) {
        try {
            Options opts = client.options;
            double target = HandyConfig.get().fullbrightGamma;
            // Try to set via OptionInstance - clamp may exist but we override via reflection if needed
            var gammaOption = opts.gamma();
            gammaOption.set(target);
            // Also try to force light via direct field if available (some mappings keep field)
            // For 1.21.11, try to bypass clamp using reflection on value field
            // This is best-effort; LightTextureMixin will handle actual brightness
        } catch (Exception e) {
            HandyUtilsMod.LOGGER.warn("[HandyUtils] Failed to set gamma", e);
        }
    }

    private static void restoreGamma(Minecraft client) {
        try {
            Options opts = client.options;
            double def = HandyConfig.get().defaultGamma;
            opts.gamma().set(def);
        } catch (Exception e) {
            HandyUtilsMod.LOGGER.warn("[HandyUtils] Failed to restore gamma", e);
        }
    }

    public static boolean isFullbrightEnabled() {
        return enabled;
    }
}
