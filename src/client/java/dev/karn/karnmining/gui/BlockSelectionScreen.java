package dev.karn.karnmining.gui;

import dev.karn.karnmining.KarnMiningClient;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Searchable, scrollable grid of every obtainable block in the registry,
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
    private String query = "";
    private int scrollRows;
    private int columns;
    private int rowsVisible;
    private int listTop;
    private int listBottom;
    private int listLeft;
    private int listWidth;

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
        listLeft = (width - contentWidth) / 2;
        listWidth = contentWidth;
        listTop = 58;
        listBottom = height - 44;
        columns = Math.max(1, listWidth / CELL);
        rowsVisible = Math.max(1, (listBottom - listTop) / CELL);

        searchField = new TextFieldWidget(textRenderer, listLeft, 28, listWidth - 76, 20, Text.literal("Search blocks"));
        searchField.setMaxLength(80);
        searchField.setText(query);
        searchField.setChangedListener(text -> applySearch(false));
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Search"), button -> applySearch(true))
            .dimensions(listLeft + listWidth - 72, 28, 72, 20)
            .build());

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

        layout();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= listTop && mouseY <= listBottom && mouseX >= listLeft && mouseX <= listLeft + listWidth) {
            scrollRows -= (int) Math.round(verticalAmount * 2.0);
            layout();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchField.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            applySearch(true);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);

        MutableText info = Text.literal(filteredBlocks.size() + " / " + allBlocks.size() + " blocks");
        Optional<Block> selected = KarnMiningClient.config().getSelectedBlock();
        if (selected.isPresent()) {
            info.append("   -   Selected: ").append(selected.get().getName().formatted(Formatting.GREEN));
        }
        context.drawCenteredTextWithShadow(textRenderer, info, width / 2, height - 40, 0xA0A0A0);

        drawScrollbar(context);

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void drawScrollbar(DrawContext context) {
        int totalRows = Math.max(1, (filteredBlocks.size() + columns - 1) / columns);
        int maxScroll = totalRows - rowsVisible;
        if (maxScroll <= 0) {
            return;
        }
        int barRight = listLeft + listWidth - 2;
        int barLeft = barRight - 5;
        context.fill(barLeft, listTop, barRight, listBottom, 0xFF202020);
        int track = listBottom - listTop;
        int handleHeight = Math.max(18, track * rowsVisible / totalRows);
        int handleY = listTop + (track - handleHeight) * scrollRows / maxScroll;
        context.fill(barLeft, handleY, barRight, handleY + handleHeight, 0xFFCCCCCC);
    }

    private void select(Block block) {
        KarnMiningClient.config().setSelectedBlock(Registries.BLOCK.getId(block));
        KarnMiningClient.automation().onConfigurationChanged(client);
        client.setScreen(parent);
    }

    private void applySearch(boolean refocus) {
        query = searchField.getText().trim();
        String normalized = query.toLowerCase(Locale.ROOT);
        String underscored = normalized.replace(' ', '_');
        filteredBlocks = normalized.isEmpty() ? allBlocks : allBlocks.stream()
            .filter(block -> matches(block, normalized, underscored))
            .toList();
        scrollRows = 0;
        layout();
        if (refocus) {
            setInitialFocus(searchField);
        }
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

    /** Positions every cell for the current query and scroll offset. */
    private void layout() {
        int totalRows = Math.max(1, (filteredBlocks.size() + columns - 1) / columns);
        int maxScroll = Math.max(0, totalRows - rowsVisible);
        scrollRows = Math.max(0, Math.min(scrollRows, maxScroll));
        Block configured = KarnMiningClient.config().getSelectedBlock().orElse(null);

        for (BlockCell cell : cells) {
            int index = cell.index;
            boolean visible = index < filteredBlocks.size();
            if (visible) {
                int column = index % columns;
                int row = index / columns - scrollRows;
                visible = row >= 0 && row < rowsVisible;
                if (visible) {
                    cell.setX(listLeft + 2 + column * CELL);
                    cell.setY(listTop + 2 + row * CELL);
                }
            }
            cell.visible = visible;
            cell.selected = visible && filteredBlocks.get(index) == configured;
            if (!visible) {
                cell.setY(-10_000); // keep it out of hit-testing
            }
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    /**
     * A vanilla button showing the block's item icon; the configured block is
     * outlined in green.
     */
    private static final class BlockCell extends ButtonWidget {
        private final int index;
        private final Block block;
        private final ItemStack stack;
        private boolean selected;

        private BlockCell(int index, Block block, PressAction onPress) {
            super(0, -10_000, CELL_SIZE, CELL_SIZE, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
            this.index = index;
            this.block = block;
            this.stack = block.asItem().getDefaultStack();
            setTooltip(Tooltip.of(Text.literal(block.getName().getString() + "\n" + Registries.BLOCK.getId(block))
                .formatted(Formatting.GRAY)));
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            super.renderWidget(context, mouseX, mouseY, deltaTicks);
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
