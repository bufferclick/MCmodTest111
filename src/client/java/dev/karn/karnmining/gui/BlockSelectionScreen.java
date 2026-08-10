package dev.karn.karnmining.gui;

import dev.karn.karnmining.KarnMiningClient;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Searchable, page-scrollable grid of every obtainable block in the registry,
 * styled like the vanilla creative inventory. The list is generated from
 * {@link Registries#BLOCK} at runtime, so it always matches the current game.
 */
public final class BlockSelectionScreen extends Screen {
    private static final int CELL = 20;      // grid pitch
    private static final int CELL_SIZE = 18; // widget size (icon 16 + border)

    private final Screen parent;
    private final List<Block> allBlocks;

    private final List<BlockCell> cells = new ArrayList<>();
    private List<Block> filteredBlocks;
    private TextFieldWidget searchField;
    private ButtonWidget previousButton;
    private ButtonWidget pageButton;
    private ButtonWidget nextButton;
    private String query = "";
    private int page;
    private int columns;
    private int rows;
    private int gridLeft;
    private int gridTop;
    private int gridWidth;

    public BlockSelectionScreen(Screen parent) {
        super(Text.literal("Choose a Block"));
        this.parent = parent;
        // Every registered block that has an item (i.e. is obtainable), sorted
        // by registry id so the list is stable across languages and sessions.
        this.allBlocks = Registries.BLOCK.stream()
            .filter(block -> block.asItem() != Items.AIR)
            .sorted(Comparator.comparing(block -> Registries.BLOCK.getId(block).toString()))
            .toList();
        this.filteredBlocks = allBlocks;
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(440, width - 24);
        gridLeft = (width - contentWidth) / 2;
        gridWidth = contentWidth;
        gridTop = 58;
        rows = Math.max(3, Math.min(10, (height - 120) / CELL));
        columns = Math.max(1, gridWidth / CELL);

        searchField = new TextFieldWidget(textRenderer, gridLeft, 28, gridWidth - 76, 20, Text.literal("Search blocks"));
        searchField.setMaxLength(80);
        searchField.setText(query);
        searchField.setChangedListener(text -> applySearch());
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Search"), button -> applySearch())
            .dimensions(gridLeft + gridWidth - 72, 28, 72, 20)
            .build());

        int navigationY = gridTop + rows * CELL + 6;

        previousButton = ButtonWidget.builder(Text.literal("<"), button -> {
                page--;
                layout();
            })
            .dimensions(width / 2 - 104, navigationY, 40, 20)
            .build();
        addDrawableChild(previousButton);

        pageButton = ButtonWidget.builder(Text.literal("Page 1 / 1"), button -> { })
            .dimensions(width / 2 - 60, navigationY, 120, 20)
            .build();
        pageButton.active = false;
        addDrawableChild(pageButton);

        nextButton = ButtonWidget.builder(Text.literal(">"), button -> {
                page++;
                layout();
            })
            .dimensions(width / 2 + 64, navigationY, 40, 20)
            .build();
        addDrawableChild(nextButton);

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(width / 2 - 50, height - 26, 100, 20)
            .build());

        cells.clear();
        for (int index = 0; index < allBlocks.size(); index++) {
            Block block = allBlocks.get(index);
            BlockCell cell = new BlockCell(index, block, button -> select(block));
            cells.add(cell);
            addDrawableChild(cell);
        }

        page = 0;
        layout();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);

        Text info = Text.literal(filteredBlocks.size() + " / " + allBlocks.size() + " blocks");
        Optional<Block> selected = KarnMiningClient.config().getSelectedBlock();
        if (selected.isPresent()) {
            info = Text.literal("").append(info).append("  -  Selected: ")
                .append(selected.get().getName().formatted(Formatting.GREEN));
        }
        context.drawCenteredTextWithShadow(textRenderer, info, width / 2, height - 40, 0xA0A0A0);

        Block hovered = hoveredBlock(mouseX, mouseY);
        if (hovered != null) {
            context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(hovered.getName().getString() + "  (")
                    .append(Registries.BLOCK.getId(hovered).toString()).append(")"),
                width / 2, gridTop - 12, 0xFFFFFF);
        }

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void select(Block block) {
        KarnMiningClient.config().setSelectedBlock(Registries.BLOCK.getId(block));
        KarnMiningClient.automation().onConfigurationChanged(client);
        client.setScreen(parent);
    }

    private void applySearch() {
        query = searchField.getText().trim();
        String normalized = query.toLowerCase(Locale.ROOT);
        String underscored = normalized.replace(' ', '_');
        filteredBlocks = normalized.isEmpty() ? allBlocks : allBlocks.stream()
            .filter(block -> matches(block, normalized, underscored))
            .toList();
        page = 0;
        layout();
    }

    private static boolean matches(Block block, String query, String underscored) {
        Identifier id = Registries.BLOCK.getId(block);
        String idString = id.toString().toLowerCase(Locale.ROOT);
        String name = block.getName().getString().toLowerCase(Locale.ROOT);
        return idString.contains(query)
            || idString.contains(underscored)
            || name.contains(query)
            || name.contains(underscored.replace('_', ' '));
    }

    /** Positions every cell for the current search results and page. */
    private void layout() {
        int perPage = rows * columns;
        int pageCount = Math.max(1, (filteredBlocks.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pageCount - 1));
        int start = page * perPage;
        int end = Math.min(filteredBlocks.size(), start + perPage);
        Block configured = KarnMiningClient.config().getSelectedBlock().orElse(null);

        for (BlockCell cell : cells) {
            int index = cell.index;
            boolean visible = index >= start && index < end;
            if (visible) {
                int position = index - start;
                int column = position % columns;
                int row = position / columns;
                cell.setX(gridLeft + 2 + column * CELL);
                cell.setY(gridTop + 2 + row * CELL);
            }
            cell.visible = visible;
            cell.selected = visible && filteredBlocks.get(index) == configured;
            if (!visible) {
                cell.setY(-10_000); // keep it out of hit-testing
            }
        }

        previousButton.active = page > 0;
        nextButton.active = page + 1 < pageCount;
        pageButton.setMessage(Text.literal("Page " + (page + 1) + " / " + pageCount));
    }

    private Block hoveredBlock(int mouseX, int mouseY) {
        for (BlockCell cell : cells) {
            if (cell.visible && cell.isMouseOver(mouseX, mouseY)) {
                return cell.block;
            }
        }
        return null;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    /**
     * A vanilla button showing the block's item icon; the configured block is
     * outlined in green. The icon is drawn through {@link #drawIcon}, the
     * content hook the game calls for pressable widgets.
     */
    private static final class BlockCell extends ButtonWidget {
        private final int index;
        private final Block block;
        private final ItemStack stack;
        private boolean selected;

        private BlockCell(int index, Block block, PressAction onPress) {
            super(0, -10_000, CELL_SIZE, CELL_SIZE, Text.literal(""), onPress, DEFAULT_NARRATION_SUPPLIER);
            this.index = index;
            this.block = block;
            this.stack = block.asItem().getDefaultStack();
        }

        @Override
        public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            context.drawItem(stack, getX() + 1, getY() + 1);
            if (selected) {
                int x = getX();
                int y = getY();
                int w = getWidth();
                int h = getHeight();
                context.fill(x, y, x + w, y + 1, 0xFF55FF55);
                context.fill(x, y + h - 1, x + w, y + h, 0xFF55FF55);
                context.fill(x, y, x + 1, y + h, 0xFF55FF55);
                context.fill(x + w - 1, y, x + w, y + h, 0xFF55FF55);
            }
        }
    }
}
