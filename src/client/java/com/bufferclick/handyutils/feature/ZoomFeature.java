package com.bufferclick.handyutils.feature;

import com.bufferclick.handyutils.config.HandyConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Smooth zoom feature - hold C to zoom.
 * Very popular and genuinely useful, client-only, no server needed.
 */
public class ZoomFeature {
    public static KeyMapping ZOOM_KEY;
    public static boolean isZooming = false;
    public static float zoomProgress = 0f; // 0..1 smoothed
    public static float targetZoomDivisor;

    public static void register() {
        ZOOM_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.handyutils.zoom",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category.handyutils"
        ));
        targetZoomDivisor = HandyConfig.get().zoomDivisor;
        System.out.println("[HandyUtils] Zoom feature registered");
    }

    public static void tick(Minecraft client) {
        boolean pressed = ZOOM_KEY != null && ZOOM_KEY.isDown() && HandyConfig.get().enableZoom;
        
        // Scroll wheel zoom adjustment while zooming
        if (pressed && client.player != null) {
            // If player scrolls, adjust divisor (we detect via listening elsewhere or via mouse scrolled callback)
        }

        isZooming = pressed;
        
        // Smooth transition
        float speed = HandyConfig.get().zoomSmoothSpeed;
        if (isZooming) {
            zoomProgress += (1.0f - zoomProgress) * speed;
            if (zoomProgress > 0.99f) zoomProgress = 1f;
        } else {
            zoomProgress += (0f - zoomProgress) * speed;
            if (zoomProgress < 0.01f) zoomProgress = 0f;
        }
    }

    public static float getZoomMultiplier(float tickDelta) {
        if (zoomProgress <= 0f) return 1f;
        float divisor = HandyConfig.get().zoomDivisor;
        // Lerp between 1 and 1/divisor
        float progress = zoomProgress; // could use tickDelta for extra smoothness
        float target = 1f / divisor;
        return 1f + (target - 1f) * progress;
    }

    public static boolean shouldZoom() {
        return zoomProgress > 0.01f;
    }

    // Called from mouse scroll mixin/callback to adjust zoom
    public static void adjustZoom(double scrollDelta) {
        if (!isZooming) return;
        float current = HandyConfig.get().zoomDivisor;
        if (scrollDelta > 0) {
            current += 0.5f;
        } else {
            current -= 0.5f;
        }
        current = Math.max(1.5f, Math.min(12f, current));
        HandyConfig.get().zoomDivisor = current;
        HandyConfig.save();
        targetZoomDivisor = current;
    }
}
