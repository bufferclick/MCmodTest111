package dev.karn.karnmining.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/** Persistent, intentionally small client configuration. */
public final class KarnMiningConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("karnmining.json");

    private String selectedBlock;
    private int searchRadius = 48;

    public static KarnMiningConfig load() {
        if (!Files.isRegularFile(PATH)) {
            return new KarnMiningConfig();
        }

        try (Reader reader = Files.newBufferedReader(PATH)) {
            KarnMiningConfig config = GSON.fromJson(reader, KarnMiningConfig.class);
            if (config == null) {
                return new KarnMiningConfig();
            }
            config.searchRadius = clampRadius(config.searchRadius);
            if (config.getSelectedBlock().isEmpty()) {
                config.selectedBlock = null;
            }
            return config;
        } catch (IOException | JsonParseException exception) {
            System.err.println("[KarnMining] Could not read config: " + exception.getMessage());
            return new KarnMiningConfig();
        }
    }

    public void save() {
        searchRadius = clampRadius(searchRadius);
        Path temporary = PATH.resolveSibling(PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(this, writer);
            }
            try {
                Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            System.err.println("[KarnMining] Could not save config: " + exception.getMessage());
        }
    }

    public Optional<Block> getSelectedBlock() {
        if (selectedBlock == null || selectedBlock.isBlank()) {
            return Optional.empty();
        }
        Identifier id = Identifier.tryParse(selectedBlock);
        if (id == null || !Registries.BLOCK.containsId(id)) {
            return Optional.empty();
        }
        return Registries.BLOCK.getOptionalValue(id);
    }

    public Optional<Identifier> getSelectedBlockId() {
        return getSelectedBlock().map(Registries.BLOCK::getId);
    }

    public void setSelectedBlock(Identifier id) {
        selectedBlock = id.toString();
        save();
    }

    public int getSearchRadius() {
        return searchRadius;
    }

    public void setSearchRadius(int searchRadius) {
        this.searchRadius = clampRadius(searchRadius);
        save();
    }

    private static int clampRadius(int radius) {
        return Math.max(24, Math.min(64, radius));
    }
}
