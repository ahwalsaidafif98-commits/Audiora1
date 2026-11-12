package id.audiora.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AudioraConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of("config", "audiora-sound-controller.json");
    public boolean masterEnabled = true;
    public float masterVolume = 1.0f;
    public Map<String, Category> categories = defaults();

    public static final class Category {
        public boolean enabled = true;
        public float volume = 1.0f;
        public Category() {}
        public Category(boolean enabled, float volume) {
            this.enabled = enabled;
            this.volume = clamp(volume);
        }
    }

    private static Map<String, Category> defaults() {
        Map<String, Category> result = new LinkedHashMap<>();
        for (String name : new String[]{"combat","inventory","player","items","birds","insects","nature","ui"})
            result.put(name, new Category(true, 1.0f));
        return result;
    }

    public static AudioraConfig load() {
        try {
            Files.createDirectories(FILE.getParent());
            if (Files.exists(FILE)) {
                AudioraConfig value = GSON.fromJson(Files.readString(FILE), AudioraConfig.class);
                if (value != null) {
                    if (value.categories == null) value.categories = defaults();
                    defaults().forEach(value.categories::putIfAbsent);
                    value.masterVolume = clamp(value.masterVolume);
                    value.categories.values().forEach(c -> c.volume = clamp(c.volume));
                    return value;
                }
            }
        } catch (Exception e) {
            System.err.println("[Audiora] Config load failed: " + e.getMessage());
        }
        return new AudioraConfig();
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[Audiora] Config save failed: " + e.getMessage());
        }
    }

    public static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
