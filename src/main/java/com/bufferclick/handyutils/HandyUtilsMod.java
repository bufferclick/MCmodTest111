package com.bufferclick.handyutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.resources.Identifier;

public class HandyUtilsMod {
    public static final String MOD_ID = "handyutils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    // Called from common initializer (fabric mod json main)
    public void onInitialize() {
        LOGGER.info("[HandyUtils] Common init - client-only mod, nothing to do here");
    }

    // Fabric Loader will call onInitialize via ModInitializer interface but we provide method
    // To make it compatible we also implement via entrypoint wrapper
    public static class MainEntrypoint implements net.fabricmc.api.ModInitializer {
        @Override
        public void onInitialize() {
            new HandyUtilsMod().onInitialize();
        }
    }
}
