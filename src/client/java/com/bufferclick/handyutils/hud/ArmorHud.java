package com.bufferclick.handyutils.hud;

import com.bufferclick.handyutils.config.HandyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

/**
 * Armor HUD - shows armor durability and status near hotbar.
 * Very useful for PvP, mining, long adventures.
 */
public class ArmorHud {

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!HandyConfig.get().enableArmorHud) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        Player player = mc.player;
        if (player == null) return;

        // Only render if player has armor or tool is damageable
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // Position: above hotbar, center offset
        int baseX = screenWidth / 2 + 90 + 5;
        int baseY = screenHeight - 18 - 30;

        // Render armor from feet to head (boots first)
        var armor = player.getInventory().armor;
        int offsetY = 0;
        for (int i = 0; i < armor.size(); i++) {
            ItemStack stack = armor.get(i);
            if (stack.isEmpty()) continue;

            int x = baseX;
            int y = baseY - offsetY * 18;

            // Render item icon
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(mc.font, stack, x, y);

            // Render durability text if damageable
            if (stack.isDamageableItem()) {
                int max = stack.getMaxDamage();
                int dmg = stack.getDamageValue();
                int remaining = max - dmg;
                float percent = (float) remaining / max;
                String color = percent > 0.5f ? "§a" : percent > 0.25f ? "§e" : "§c";
                String txt = color + remaining;

                // Only show if low or always? Show low durability below 30%
                if (percent < 0.4f || HandyConfig.get().infoHudShowFps) {
                    graphics.drawString(mc.font, Component.literal(txt), x + 18, y + 5, 0xFFFFFF);
                }
            }

            offsetY++;
        }

        // Render main hand tool durability warning
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.isDamageableItem()) {
            int max = mainHand.getMaxDamage();
            int damage = mainHand.getDamageValue();
            int remaining = max - damage;
            float percent = (float) remaining / (float) max;
            if (percent < 0.15f) {
                // Warning near crosshair
                int cx = screenWidth / 2;
                int cy = screenHeight / 2 + 15;
                String warn = String.format("§c⚠ Low Durability: %d", remaining);
                graphics.drawCenteredString(mc.font, Component.literal(warn), cx, cy, 0xFF5555);
            }
        }
    }
}
