package com.bufferclick.handyutils.mixin;

import com.bufferclick.handyutils.config.HandyConfig;
import com.bufferclick.handyutils.feature.InventorySorter;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Middle-click to sort container - a tiny QoL that players love.
 * Also handles R key sorting inside containers.
 */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void handyutils$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!HandyConfig.get().enableInventorySorter) return;

        // Middle click (button 2) sorts container
        if (button == 2) {
            // Check if it's inside container area - for simplicity always allow middle click to sort
            try {
                @SuppressWarnings("unchecked")
                AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
                InventorySorter.sortContainer(self);
                cir.setReturnValue(true);
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
