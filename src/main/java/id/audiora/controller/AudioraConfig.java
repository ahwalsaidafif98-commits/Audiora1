package id.audiora.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class AudioraConfig {
    public static final String[] CATEGORIES = {
        "inventory", "player", "items",
        "birds", "insects", "nature", "ui"
    };
    private static final Path FILE = Path.of("config", "audiora-sound-controller.json");
    private static final boolean[] lastCategoryMuted = new boolean[CATEGORIES.length];
    private static boolean lastMasterMuted;
    public static boolean masterEnabled = true;
    public static int masterVolume = 100;
    public static boolean comboEnabled = true;
    public static int comboOffsetX = 0;
    public static int comboOffsetY = 24;
    public static int mobInfoOffsetX = 0;
    public static int mobInfoOffsetY = 12;
    public static int itemInfoOffsetX = 0;
    public static int itemInfoOffsetY = 0;
    public static final String[] HUD_IDS = {"coordinates", "biome", "armor", "held_item", "target", "pickup", "ore_tracker", "combo", "damage_number", "critical_hit", "potion_effects"};
    public static final int[] hudX = new int[HUD_IDS.length];
    public static final int[] hudY = new int[HUD_IDS.length];
    public static final int[] hudScale = new int[HUD_IDS.length];
    public static final boolean[] enabled = new boolean[CATEGORIES.length];
    public static final int[] volume = new int[CATEGORIES.length];

    static {
        for (int i = 0; i < CATEGORIES.length; i++) {
            enabled[i] = true;
            volume[i] = 100;
        }
        java.util.Arrays.fill(hudScale, 100);
        load();
    }

    private AudioraConfig() {}

    public static void load() {
        if (!Files.exists(FILE)) { save(); return; }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            masterEnabled = !root.has("masterEnabled") || root.get("masterEnabled").getAsBoolean();
            masterVolume = clamp(root.has("masterVolume") ? Math.round(root.get("masterVolume").getAsFloat() * 100) : 100);
            comboEnabled = !root.has("comboEnabled") || root.get("comboEnabled").getAsBoolean();
            comboOffsetX = root.has("comboOffsetX") ? root.get("comboOffsetX").getAsInt() : 0;
            comboOffsetY = root.has("comboOffsetY") ? root.get("comboOffsetY").getAsInt() : 24;
            mobInfoOffsetX = root.has("mobInfoOffsetX") ? root.get("mobInfoOffsetX").getAsInt() : 0;
            mobInfoOffsetY = root.has("mobInfoOffsetY") ? root.get("mobInfoOffsetY").getAsInt() : 12;
            itemInfoOffsetX = root.has("itemInfoOffsetX") ? root.get("itemInfoOffsetX").getAsInt() : 0;
            itemInfoOffsetY = root.has("itemInfoOffsetY") ? root.get("itemInfoOffsetY").getAsInt() : 0;
            JsonObject hudLayout = root.has("hudLayout") ? root.getAsJsonObject("hudLayout") : new JsonObject();
            for (int i = 0; i < HUD_IDS.length; i++) {
                JsonObject layout = hudLayout.has(HUD_IDS[i]) ? hudLayout.getAsJsonObject(HUD_IDS[i]) : new JsonObject();
                hudX[i] = layout.has("x") ? layout.get("x").getAsInt() : 0;
                hudY[i] = layout.has("y") ? layout.get("y").getAsInt() : 0;
                hudScale[i] = Math.max(50, Math.min(200, layout.has("scale") ? layout.get("scale").getAsInt() : 100));
            }
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
        root.addProperty("comboEnabled", comboEnabled);
        root.addProperty("comboOffsetX", comboOffsetX);
        root.addProperty("comboOffsetY", comboOffsetY);
        root.addProperty("mobInfoOffsetX", mobInfoOffsetX);
        root.addProperty("mobInfoOffsetY", mobInfoOffsetY);
        root.addProperty("itemInfoOffsetX", itemInfoOffsetX);
        root.addProperty("itemInfoOffsetY", itemInfoOffsetY);
        JsonObject hudLayout = new JsonObject();
        for (int i = 0; i < HUD_IDS.length; i++) {
            JsonObject layout = new JsonObject();
            layout.addProperty("x", hudX[i]);
            layout.addProperty("y", hudY[i]);
            layout.addProperty("scale", hudScale[i]);
            hudLayout.add(HUD_IDS[i], layout);
        }
        root.add("hudLayout", hudLayout);
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
        masterEnabled = true; masterVolume = 100; comboEnabled = true;
        comboOffsetX = 0; comboOffsetY = 24;
        mobInfoOffsetX = 0; mobInfoOffsetY = 12;
        itemInfoOffsetX = 0; itemInfoOffsetY = 0;
        for (int i = 0; i < CATEGORIES.length; i++) { enabled[i] = true; volume[i] = 100; }
        java.util.Arrays.fill(lastCategoryMuted, false);
        lastMasterMuted = false;
        save();
    }

    /**
     * Stops sounds that were already playing when a category is switched off
     * or its volume reaches zero. The volume mixin controls sounds started later;
     * this method handles existing streams/channels that keep their initial gain.
     */
    public static void enforceMutedSounds(Minecraft client) {
        if (client == null) return;

        boolean masterMuted = !masterEnabled || masterVolume <= 0;
        if (masterMuted) {
            if (!lastMasterMuted) stopMatching(client, -1);
            lastMasterMuted = true;
            java.util.Arrays.fill(lastCategoryMuted, true);
            return;
        }

        lastMasterMuted = false;
        for (int i = 0; i < CATEGORIES.length; i++) {
            boolean muted = !enabled[i] || volume[i] <= 0;
            if (muted && !lastCategoryMuted[i]) stopMatching(client, i);
            lastCategoryMuted[i] = muted;
        }
    }

    private static void stopMatching(Minecraft client, int category) {
        for (Identifier id : BuiltInRegistries.SOUND_EVENT.keySet()) {
            if (category < 0 || categoryForSound(id.toString()) == category) {
                client.getSoundManager().stop(id, null);
            }
        }
    }

    public static float scaleForSound(String soundId) {
        if (!masterEnabled || masterVolume <= 0) return 0.0f;
        String id = soundId == null ? "" : soundId.toLowerCase(java.util.Locale.ROOT);
        int category = categoryForSound(id);
        if (category < 0) return masterVolume / 100.0f;
        if (!enabled[category] || volume[category] <= 0) return 0.0f;
        return (masterVolume / 100.0f) * (volume[category] / 100.0f);
    }

    private static int categoryForSound(String id) {
        if (containsAny(id, "ui.", "ui_", "button", "click", "toast", "widget")) return indexOfCategory("ui");
        if (containsAny(id, "inventory", "container", "chest", "barrel", "shulker", "crafting", "inv.")) return indexOfCategory("inventory");
        if (containsAny(id, "bird", "parrot", "chicken", "crow", "owl", "duck")) return indexOfCategory("birds");
        if (containsAny(id, "bee", "cricket", "insect", "cicada", "mosquito", "frog")) return indexOfCategory("insects");
        if (containsAny(id, "wind", "wave", "ocean", "beach", "rain", "thunder", "storm", "water", "ambient", "cave")) return indexOfCategory("nature");
        if (containsAny(id, "item", "pickup", "drop", "throw", "equip", "consume", "break")) return indexOfCategory("items");
        if (containsAny(id, "player", "step", "footstep", "swim", "hurt", "death", "jump", "eat", "burp", "splash", "sword_swing", "armor")) return indexOfCategory("player");
        return -1;
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    public static int indexOfCategory(String category) {
        for (int i = 0; i < CATEGORIES.length; i++) if (CATEGORIES[i].equals(category)) return i;
        return -1;
    }

    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }
}