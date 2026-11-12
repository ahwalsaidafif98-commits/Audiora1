package id.audiora.controller;

import net.fabricmc.api.ClientModInitializer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Initial Audiora client bootstrap.
 * Creates a persistent starter configuration; sound interception and GUI
 * controls are intentionally not claimed as implemented in this first build.
 */
public final class AudioraController implements ClientModInitializer {
    private static final String[] CATEGORIES = {
        "combat", "inventory", "player", "items",
        "birds", "insects", "nature", "ui"
    };

    @Override
    public void onInitializeClient() {
        Path config = Path.of("config", "audiora-sound-controller.json");
        try {
            Files.createDirectories(config.getParent());
            if (Files.notExists(config)) {
                Files.writeString(config, defaultConfig());
            }
        } catch (IOException exception) {
            System.err.println("[Audiora] Could not create configuration: " + exception.getMessage());
        }
        System.out.println("[Audiora] Sound Controller initialized.");
    }

    private static String defaultConfig() {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"masterEnabled\": true,\n  \"masterVolume\": 1.0,\n  \"categories\": {\n");
        for (int i = 0; i < CATEGORIES.length; i++) {
            json.append("    \"").append(CATEGORIES[i]).append("\": { \"enabled\": true, \"volume\": 1.0 }");
            json.append(i == CATEGORIES.length - 1 ? "\n" : ",\n");
        }
        json.append("  }\n}\n");
        return json.toString();
    }
}
