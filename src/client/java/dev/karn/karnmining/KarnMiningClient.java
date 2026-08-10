package dev.karn.karnmining;

import dev.karn.karnmining.automation.AutomationController;
import dev.karn.karnmining.config.KarnMiningConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KarnMiningClient implements ClientModInitializer {
    public static final String MOD_ID = "karnmining";

    private static KarnMiningConfig config;
    private static AutomationController automation;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        config = KarnMiningConfig.load();
        automation = new AutomationController(config);

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.karnmining.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                if (client.player != null && client.options.sneakKey.isPressed()) {
                    if (automation.toggle(client)) {
                        Text message = Text.literal(automation.isEnabled() ? "KM is Enabled" : "KM is Disabled")
                            .formatted(automation.isEnabled() ? Formatting.GREEN : Formatting.RED);
                        client.player.sendMessage(message, false);
                    }
                }
            }
            automation.tick(client);
        });
    }

    public static KarnMiningConfig config() {
        if (config == null) {
            config = KarnMiningConfig.load();
        }
        return config;
    }

    public static AutomationController automation() {
        if (automation == null) {
            automation = new AutomationController(config());
        }
        return automation;
    }

    public static KeyBinding toggleKey() {
        return toggleKey;
    }
}
