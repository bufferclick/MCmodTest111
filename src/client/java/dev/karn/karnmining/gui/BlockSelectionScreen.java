package dev.karn.karnmining.gui;

import dev.karn.karnmining.KarnMiningClient;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Search-and-page selector containing every block in the block registry. */
public final class BlockSelectionScreen extends Screen {
    private final Screen parent;
    private final List<Block> allBlocks;

    private List<Block> filteredBlocks;
    private TextFieldWidget searchField;
    private String query = "";
    private int page;
    private int rows;

    public BlockSelectionScreen(Screen parent) {
        super(Text.literal("Choose a Block"));
        this.parent = parent;
        this.allBlocks = Registries.BLOCK.stream()
            .sorted(Comparator.comparing(block -> Registries.BLOCK.getId(block).toString()))
            .toList();
        this.filteredBlocks = allBlocks;
    }

    @Override
    protected void init() {
        rebuildWidgets(true);
    }

    private void rebuildWidgets(boolean focusSearch) {
        clearChildren();
        rows = Math.max(3, Math.min(10, (height - 130) / 22));
        int contentWidth = Math.min(440, width - 24);
        int left = (width - contentWidth) / 2;

        searchField = new TextFieldWidget(textRenderer, left, 38, contentWidth - 76, 20, Text.literal("Search blocks"));
        searchField.setMaxLength(80);
        searchField.setText(query);
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Search"), button -> applySearch())
            .dimensions(left + contentWidth - 72, 38, 72, 20)
            .build());

        int perPage = rows * 2;
        int pageCount = Math.max(1, (filteredBlocks.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pageCount - 1));
        int start = page * perPage;
        int columnGap = 4;
        int columnWidth = (contentWidth - columnGap) / 2;

        for (int index = 0; index < perPage && start + index < filteredBlocks.size(); index++) {
            Block block = filteredBlocks.get(start + index);
            Identifier id = Registries.BLOCK.getId(block);
            int column = index % 2;
            int row = index / 2;
            int x = left + column * (columnWidth + columnGap);
            int y = 68 + row * 22;
            String name = block.getName().getString();
            String shortened = textRenderer.trimToWidth(name, columnWidth - 12);
            if (!shortened.equals(name) && shortened.length() > 1) {
                shortened = shortened.substring(0, shortened.length() - 1) + "…";
            }
            ButtonWidget entry = ButtonWidget.builder(Text.literal(shortened), button -> select(block))
                .dimensions(x, y, columnWidth, 20)
                .tooltip(Tooltip.of(Text.literal(name + "\n" + id).formatted(Formatting.GRAY)))
                .build();
            addDrawableChild(entry);
        }

        int navigationY = height - 50;
        ButtonWidget previous = ButtonWidget.builder(Text.literal("<"), button -> {
                page--;
                rebuildWidgets(false);
            })
            .dimensions(width / 2 - 104, navigationY, 40, 20)
            .build();
        previous.active = page > 0;
        addDrawableChild(previous);

        ButtonWidget indicator = ButtonWidget.builder(Text.literal("Page " + (page + 1) + " / " + pageCount), button -> { })
            .dimensions(width / 2 - 60, navigationY, 120, 20)
            .build();
        indicator.active = false;
        addDrawableChild(indicator);

        ButtonWidget next = ButtonWidget.builder(Text.literal(">"), button -> {
                page++;
                rebuildWidgets(false);
            })
            .dimensions(width / 2 + 64, navigationY, 40, 20)
            .build();
        next.active = page + 1 < pageCount;
        addDrawableChild(next);

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(width / 2 - 100, height - 26, 200, 20)
            .build());

        if (focusSearch) {
            setInitialFocus(searchField);
        }
    }

    private void applySearch() {
        query = searchField.getText().strip();
        String normalized = query.toLowerCase(Locale.ROOT);
        filteredBlocks = normalized.isEmpty() ? allBlocks : allBlocks.stream()
            .filter(block -> {
                Identifier id = Registries.BLOCK.getId(block);
                return id.toString().toLowerCase(Locale.ROOT).contains(normalized)
                    || block.getName().getString().toLowerCase(Locale.ROOT).contains(normalized);
            })
            .toList();
        page = 0;
        rebuildWidgets(true);
    }

    private void select(Block block) {
        KarnMiningClient.config().setSelectedBlock(Registries.BLOCK.getId(block));
        KarnMiningClient.automation().onConfigurationChanged(client);
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
            Text.literal(filteredBlocks.size() + " of " + allBlocks.size() + " registered blocks"),
            width / 2, 59, 0xA0A0A0);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
