package id.audiora.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AudioraConfig {
    public static final String[] CATEGORIES = {
        "combat", "inventory", "player", "items",
        "birds", "insects", "nature", "ui"
    };
    private static final Path FILE = Path.of("config", "audiora-sound-controller.json");
    public static boolean masterEnabled = true;
    public static int masterVolume = 100;
    public static final boolean[] enabled = new boolean[CATEGORIES.length];
    public static final int[] volume = new int[CATEGORIES.length];

    static {
        for (int i = 0; i < CATEGORIES.length; i++) {
            enabled[i] = true;
            volume[i] = 100;
        }
        load();
    }

    private AudioraConfig() {}

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            masterEnabled = !root.has("masterEnabled") || root.get("masterEnabled").getAsBoolean();
            masterVolume = clamp(root.has("masterVolume") ? Math.round(root.get("masterVolume").getAsFloat() * 100) : 100);
            JsonObject categories = root.has("categories") ? root.getAsJsonObject("categories") : new JsonObject();
            for (int i = 0; i < CATEGORIES.length; i++) {
                JsonObject item = categories.has(CATEGORIES[i]) ? categories.getAsJsonObject(CATEGORIES[i]) : new JsonObject();
                enabled[i] = !item.has("enabled") || item.get("enabled").getAsBoolean();
                volume[i] = clamp(item.has("volume") ? Math.round(item.get("volume").getAsFloat() * 100) : 100);
            }
        } catch (Exception exception) {
            System.err.println("[Audiora] Config invalid, using defaults: " + exception.getMessage());
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("masterEnabled", masterEnabled);
        root.addProperty("masterVolume", masterVolume / 100.0f);
        JsonObject categories = new JsonObject();
        for (int i = 0; i < CATEGORIES.length; i++) {
            JsonObject item = new JsonObject();
            item.addProperty("enabled", enabled[i]);
            item.addProperty("volume", volume[i] / 100.0f);
            categories.add(CATEGORIES[i], item);
        }
        root.add("categories", categories);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, root.toString());
        } catch (IOException exception) {
            System.err.println("[Audiora] Could not save config: " + exception.getMessage());
        }
    }

    public static void reset() {
        masterEnabled = true;
        masterVolume = 100;
        for (int i = 0; i < CATEGORIES.length; i++) {
            enabled[i] = true;
            volume[i] = 100;
        }
        save();
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}