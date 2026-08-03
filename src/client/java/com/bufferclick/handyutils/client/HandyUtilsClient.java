package com.bufferclick.handyutils.client;

import com.bufferclick.handyutils.HandyUtilsMod;
import com.bufferclick.handyutils.config.HandyConfig;
import com.bufferclick.handyutils.feature.DeathTracker;
import com.bufferclick.handyutils.feature.FullbrightFeature;
import com.bufferclick.handyutils.feature.InventorySorter;
import com.bufferclick.handyutils.feature.ShulkerPreview;
import com.bufferclick.handyutils.feature.ZoomFeature;
import com.bufferclick.handyutils.hud.ArmorHud;
import com.bufferclick.handyutils.hud.InfoHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.network.chat.Component;

/**
 * HandyUtils - Useful client-side mod for 1.21.11
 * Combines the most wanted QoL features into one lightweight mod:
 * - Smooth Zoom (C)
 * - Fullbright (H)
 * - Info HUD (FPS, Ping, Coords, Biome, Light)
 * - Armor HUD & Durability Warnings
 * - Shulker Box Preview (Shift hover)
 * - Inventory Sorting (Middle-click / R)
 * - Death Tracker
 *
 * All features are client-only, work on any server.
 */
public class HandyUtilsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HandyUtilsMod.LOGGER.info("[HandyUtils] Initializing client v1.0.0 for 1.21.11");

        // Load config
        HandyConfig.load();

        // Register features
        ZoomFeature.register();
        FullbrightFeature.register();
        InventorySorter.register();
        ShulkerPreview.register();

        // Register HUD elements via new HudElementRegistry API (1.21.11)
        try {
            Identifier infoHudId = HandyUtilsMod.id("info_hud");
            Identifier armorHudId = HandyUtilsMod.id("armor_hud");

            // Info HUD before chat - will appear top-left
            HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, infoHudId, (graphics, deltaTracker) -> {
                try {
                    InfoHud.render(graphics, deltaTracker);
                } catch (Exception e) {
                    HandyUtilsMod.LOGGER.error("Error rendering InfoHud", e);
                }
            });

            // Armor HUD after hotbar
            HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, armorHudId, (graphics, deltaTracker) -> {
                try {
                    ArmorHud.render(graphics, deltaTracker);
                } catch (Exception e) {
                    HandyUtilsMod.LOGGER.error("Error rendering ArmorHud", e);
                }
            });

            HandyUtilsMod.LOGGER.info("[HandyUtils] HUD registered");
        } catch (Exception e) {
            HandyUtilsMod.LOGGER.error("[HandyUtils] Failed to register HUD, falling back to legacy if needed", e);
            // Fallback for older Fabric API could use HudRenderCallback
        }

        // Client tick - handle keybinds & trackers
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null) return;
            try {
                ZoomFeature.tick(client);
                FullbrightFeature.tick(client);
                InventorySorter.tick(client);
                DeathTracker.tick(client);
            } catch (Exception ex) {
                HandyUtilsMod.LOGGER.warn("[HandyUtils] Tick error", ex);
            }
        });

        // Register client commands (/handy)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("handy")
                    .executes(context -> {
                        context.getSource().sendFeedback(Component.literal("§e[HandyUtils] §7Commands:"));
                        context.getSource().sendFeedback(Component.literal("§7/handy hud - toggle HUD"));
                        context.getSource().sendFeedback(Component.literal("§7/handy fullbright - toggle fullbright"));
                        context.getSource().sendFeedback(Component.literal("§7/handy death - show last death"));
                        context.getSource().sendFeedback(Component.literal("§7/handy zoom <1-12> - set zoom strength"));
                        return 1;
                    })
                    .then(ClientCommandManager.literal("hud").executes(ctx -> {
                        HandyConfig.get().enableInfoHud = !HandyConfig.get().enableInfoHud;
                        HandyConfig.save();
                        ctx.getSource().sendFeedback(Component.literal("§e[HandyUtils] HUD: " + (HandyConfig.get().enableInfoHud ? "§aON" : "§cOFF")));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("fullbright").executes(ctx -> {
                        FullbrightFeature.toggle(Minecraft.getInstance());
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("death").executes(ctx -> {
                        ctx.getSource().sendFeedback(DeathTracker.getDeathMessage());
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("zoom")
                            .then(ClientCommandManager.argument("strength", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(1.0f, 12.0f))
                                    .executes(ctx -> {
                                        float str = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "strength");
                                        HandyConfig.get().zoomDivisor = str;
                                        HandyConfig.save();
                                        ctx.getSource().sendFeedback(Component.literal("§e[HandyUtils] Zoom set to: " + str + "x"));
                                        return 1;
                                    })
                            )
                    )
            );
        });

        HandyUtilsMod.LOGGER.info("[HandyUtils] Client initialized! Press C to zoom, H for fullbright, R to sort, Middle-click chests to sort. Enjoy :)");
    }
}
