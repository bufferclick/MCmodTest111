package com.bufferclick.handyutils.mixin;

import com.bufferclick.handyutils.feature.ZoomFeature;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to implement smooth zoom by modifying FOV.
 * Tested for 1.21.11 official mappings:
 * GameRenderer class = net.minecraft.client.renderer.GameRenderer
 * getFov method signature: private float getFov(Camera camera, float tickDelta, boolean changingFov)
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void handyutils$onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        try {
            if (ZoomFeature.shouldZoom()) {
                float original = cir.getReturnValue();
                float multiplier = ZoomFeature.getZoomMultiplier(tickDelta);
                float zoomed = original * multiplier;
                cir.setReturnValue(zoomed);
            }
        } catch (Exception ignored) {
            // Fallback - don't break rendering if zoom fails
        }
    }
}
