package com.bufferclick.handyutils.mixin;

import com.bufferclick.handyutils.feature.FullbrightFeature;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Fullbright implementation for 1.21.11 where gamma is clamped.
 * Forces lightmap to be bright when fullbright enabled.
 * This mixin will work if the target method exists; it's best-effort.
 */
@Mixin(LightTexture.class)
public class LightTextureMixin {

    // In some versions LightTexture has method updateLightTexture that takes darkness factor
    // We attempt to modify arguments to force max brightness
    @ModifyArg(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;clampColor(F)F"), index = 0)
    private float handyutils$modifyGamma(float original) {
        if (FullbrightFeature.isFullbrightEnabled()) {
            return 15.0f; // Force max
        }
        return original;
    }
}
