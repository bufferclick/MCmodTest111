package dev.karn.karnmining.gui;

import dev.karn.karnmining.KarnMiningClient;
import dev.karn.karnmining.automation.AutomationController;
import dev.karn.karnmining.config.KarnMiningConfig;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;

/**
 * Vanilla-styled configuration menu. Opened with Crouch + G (rebindable
 * through the Controls screen); every widget uses the standard Minecraft
 * button/text styling and fonts.
 */
public final class KarnMiningConfigScreen extends Screen {
    private static final int[] RADII = {24, 32, 48, 64};

    private final Screen parent;

    public KarnMiningConfigScreen(Screen parent) {
        super(Text.literal("KarnMining Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        KarnMiningConfig config = KarnMiningClient.config();
        int center = width / 2;
        int buttonWidth = Math.min(280, width - 40);
        int left = center - buttonWidth / 2;
        int y = 92;

        addDrawableChild(ButtonWidget.builder(toggleLabel(), button -> {
                KarnMiningClient.toggleWithMessage(client, false);
                button.setMessage(toggleLabel());
            })
            .dimensions(left, y, buttonWidth, 20)
            .build());
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.literal("Choose Block..."), button ->
                client.setScreen(new BlockSelectionScreen(this)))
            .dimensions(left, y, buttonWidth, 20)
            .build());
        y += 24;

        addDrawableChild(ButtonWidget.builder(sneakLabel(config), button -> {
                config.setRequiresSneak(!config.requiresSneak());
                button.setMessage(sneakLabel(config));
            })
            .dimensions(left, y, buttonWidth, 20)
            .build());
        y += 24;

        addDrawableChild(ButtonWidget.builder(radiusText(config), button -> {
                int next = nextRadius(config.getSearchRadius());
                config.setSearchRadius(next);
                button.setMessage(radiusText(config));
                KarnMiningClient.automation().onConfigurationChanged(client);
            })
            .dimensions(left, y, buttonWidth, 20)
            .build());
        y += 24;

        Text keys = Text.literal("Keys: Toggle ")
            .append(KarnMiningClient.activationKey().getBoundKeyLocalizedText())
            .append("  /  Menu ")
            .append(KarnMiningClient.configMenuKey().getBoundKeyLocalizedText())
            .append("  (Controls...)");
        addDrawableChild(ButtonWidget.builder(keys, button ->
                client.setScreen(new KeybindsScreen(this, client.options)))
            .dimensions(left, y, buttonWidth, 20)
            .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
            .dimensions(center - 100, height - 28, 200, 20)
            .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0xFFFFFF);

        Optional<Block> selected = KarnMiningClient.config().getSelectedBlock();
        if (selected.isPresent()) {
            Block block = selected.get();
            context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Selected: ").append(block.getName()).formatted(Formatting.GREEN),
                width / 2, 42, 0xFFFFFF);
            context.drawItem(block.asItem().getDefaultStack(), width / 2 - 8, 58);
            context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(Registries.BLOCK.getId(block).toString()).formatted(Formatting.GRAY),
                width / 2, 78, 0xFFFFFF);
        } else {
            context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Selected: None — choose a block to start mining.").formatted(Formatting.YELLOW),
                width / 2, 44, 0xFFFFFF);
        }

        String status = KarnMiningClient.automation().getStatus();
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Status: " + status),
            width / 2, 220, 0xA0A0A0);

        context.drawCenteredTextWithShadow(textRenderer,
            Text.literal("Warning: automation may violate server rules and can result in a ban.")
                .formatted(Formatting.RED),
            width / 2, height - 46, 0xFFFFFF);

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void close() {
        KarnMiningClient.config().save();
        if (parent != null) {
            client.setScreen(parent);
        } else {
            client.setScreen(null);
        }
    }

    private static Text toggleLabel() {
        AutomationController automation = KarnMiningClient.automation();
        return Text.literal(automation.isEnabled() ? "Disable KarnMining" : "Enable KarnMining");
    }

    private static Text sneakLabel(KarnMiningConfig config) {
        String key = KarnMiningClient.activationKey().getBoundKeyLocalizedText().getString();
        return Text.literal(config.requiresSneak()
            ? "Activation: Sneak + " + key
            : "Activation: " + key + " (no sneak)");
    }

    private static Text radiusText(KarnMiningConfig config) {
        return Text.literal("Search Radius: " + config.getSearchRadius() + " blocks");
    }

    private static int nextRadius(int current) {
        for (int i = 0; i < RADII.length; i++) {
            if (RADII[i] == current) {
                return RADII[(i + 1) % RADII.length];
            }
            if (RADII[i] > current) {
                return RADII[i];
            }
        }
        return RADII[0];
    }
}
