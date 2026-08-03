package com.bufferclick.handyutils.feature;

import com.bufferclick.handyutils.config.HandyConfig;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Shulker Box Preview - shows contents when holding Shift.
 * Very popular QoL, client-only. Uses new data components in 1.21.11.
 * Vanilla already shows first 5 items, we show all with counts and highlight.
 */
public class ShulkerPreview {

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!HandyConfig.get().enableShulkerPreview) return;
            try {
                if (!isShulkerBox(stack)) return;

                ItemContainerContents container = stack.get(DataComponents.CONTAINER);
                if (container == null) return;

                var items = container.nonEmptyItems().toList();
                if (items.isEmpty()) {
                    lines.add(Component.literal("§7Empty"));
                    return;
                }

                lines.add(Component.literal("§8§m                    §r"));
                lines.add(Component.literal("§eContents: §7(" + items.size() + " stacks)"));

                // If player holding shift -> show all, else show first 5 like vanilla plus hint
                boolean shiftHeld = hasShiftDown();
                int limit = shiftHeld ? items.size() : Math.min(5, items.size());

                for (int i = 0; i < limit; i++) {
                    ItemStack inner = items.get(i);
                    String name = inner.getHoverName().getString();
                    int count = inner.getCount();
                    lines.add(Component.literal(String.format(" §7- §f%s §8x%d", name, count)));
                }

                if (!shiftHeld && items.size() > 5) {
                    lines.add(Component.literal("§7... and §f" + (items.size() - 5) + " §7more §8[Hold Shift]"));
                }

                if (shiftHeld) {
                    lines.add(Component.literal("§8§m                    §r"));
                }

            } catch (Exception ignored) {
            }
        });
    }

    private static boolean isShulkerBox(ItemStack stack) {
        String id = stack.getItem().toString().toLowerCase();
        // Check if item description contains shulker_box or registry key
        // Using string to avoid direct block tags - robust across mappings
        if (id.contains("shulker_box")) return true;
        // Also check via DataComponents - shulker boxes have CONTAINER component usually
        // But other containers (chest) creative pick also have container - we still preview them, which is useful!
        // So we preview any item that has container component
        return stack.has(DataComponents.CONTAINER);
    }

    private static boolean hasShiftDown() {
        try {
            return net.minecraft.client.gui.screens.Screen.hasShiftDown();
        } catch (Exception e) {
            return false;
        }
    }
}
