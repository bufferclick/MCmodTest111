package dev.karn.karnmining;

import dev.karn.karnmining.automation.AutomationController;
import dev.karn.karnmining.config.KarnMiningConfig;
import dev.karn.karnmining.gui.KarnMiningConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Client entrypoint. Registers the two KarnMining keybinds with the vanilla
 * Controls screen and drives the automation controller once per tick.
 *
 * <p>Default bindings:
 * <ul>
 *   <li>Sneak + G — open the configuration menu (G is rebindable).</li>
 *   <li>Sneak + L — toggle KarnMining on/off (L is rebindable; the sneak
 *       requirement can be switched off in the configuration menu).</li>
 * </ul>
 */
public final class KarnMiningClient implements ClientModInitializer {
    public static final String MOD_ID = "karnmining";

    private static KarnMiningConfig config;
    private static AutomationController automation;
    private static KeyBinding activationKey;
    private static KeyBinding configMenuKey;

    @Override
    public void onInitializeClient() {
        config = KarnMiningConfig.load();
        automation = new AutomationController(config);

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

        activationKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.karnmining.activation",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            category
        ));

        configMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.karnmining.config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Configuration menu: Crouch + G (the G key is rebindable).
            while (configMenuKey.wasPressed()) {
                if (client.player != null && client.options.sneakKey.isPressed()) {
                    client.setScreen(new KarnMiningConfigScreen(null));
                }
            }

            // Activation: Sneak + L, or L alone when sneak is not required.
            while (activationKey.wasPressed()) {
                if (client.player != null) {
                    toggleWithMessage(client, true);
                    break;
                }
            }

            automation.tick(client);
        });
    }

    /**
     * Toggles KarnMining and prints the "KM is Enabled"/"KM is Disabled"
     * chat message.
     *
     * @param checkSneak whether the sneak requirement should be enforced
     *                   (true when invoked from the keybind, false from the
     *                   configuration menu)
     */
    public static void toggleWithMessage(MinecraftClient client, boolean checkSneak) {
        if (checkSneak && config.requiresSneak() && !client.options.sneakKey.isPressed()) {
            return;
        }
        if (!automation.toggle(client)) {
            return;
        }
        boolean enabled = automation.isEnabled();
        Text message = Text.literal(enabled ? "KM is Enabled" : "KM is Disabled")
            .formatted(enabled ? Formatting.GREEN : Formatting.RED);
        client.player.sendMessage(message, false);
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

    public static KeyBinding activationKey() {
        return activationKey;
    }

    public static KeyBinding configMenuKey() {
        return configMenuKey;
    }
}
