package dev.karn.karnmining.gui;

import dev.karn.karnmining.KarnMiningClient;
import dev.karn.karnmining.config.KarnMiningConfig;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;

/** Vanilla-styled configuration screen exposed through Mod Menu. */
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
        int buttonWidth = Math.min(260, width - 40);
        int left = center - buttonWidth / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("Choose a Block..."), button ->
                client.setScreen(new BlockSelectionScreen(this)))
            .dimensions(left, 86, buttonWidth, 20)
            .build());

        addDrawableChild(ButtonWidget.builder(radiusText(config), button -> {
                int next = nextRadius(config.getSearchRadius());
                config.setSearchRadius(next);
                button.setMessage(radiusText(config));
                KarnMiningClient.automation().onConfigurationChanged(client);
            })
            .dimensions(left, 110, buttonWidth, 20)
            .build());

        Text keyName = KarnMiningClient.toggleKey() == null
            ? Text.literal("L")
            : KarnMiningClient.toggleKey().getBoundKeyLocalizedText();
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Toggle Key: Sneak + ").append(keyName).append(" (Change Key Binds...)"),
                button -> client.setScreen(new KeybindsScreen(this, client.options)))
            .dimensions(left, 134, buttonWidth, 20)
            .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
            .dimensions(center - 100, height - 28, 200, 20)
            .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFF);

        Optional<Block> selected = KarnMiningClient.config().getSelectedBlock();
        Text selection = selected
            .map(block -> Text.literal("Selected: ").append(block.getName()).formatted(Formatting.GREEN))
            .orElseGet(() -> Text.literal("Selected: None").formatted(Formatting.YELLOW));
        context.drawCenteredTextWithShadow(textRenderer, selection, width / 2, 46, 0xFFFFFF);
        selected.ifPresent(block -> context.drawItem(block.asItem().getDefaultStack(), width / 2 - 8, 63));

        String status = KarnMiningClient.automation().getStatus();
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Status: " + status), width / 2, 164, 0xA0A0A0);
        context.drawCenteredTextWithShadow(textRenderer,
            Text.literal("Warning: automation may get you banned on servers.").formatted(Formatting.RED),
            width / 2, height - 46, 0xFFFFFF);

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void close() {
        KarnMiningClient.config().save();
        client.setScreen(parent);
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
