package id.audiora.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Optional open/close inventory sounds configured in config/audiora/ui.json.
 * Sound events are supplied by the custominv sound namespace.
 */
public final class AudioraUiSounds {
    private static final Path FILE = Path.of("config", "audiora", "ui.json");
    private static String openSound = "custominv.inv.buka";
    private static String closeSound = "custominv.inv.tutup";
    private static boolean enabled = true;
    private static boolean wasInventoryOpen;

    private AudioraUiSounds() {}

    public static void initialize() {
        load();
        wasInventoryOpen = false;
    }

    public static void tick(Minecraft client) {
        if (client.player == null) {
            wasInventoryOpen = false;
            return;
        }
        boolean inventoryOpen = client.gui.screen() instanceof InventoryScreen;
        if (inventoryOpen != wasInventoryOpen) {
            if (enabled) play(inventoryOpen ? openSound : closeSound, client);
            wasInventoryOpen = inventoryOpen;
        }
    }

    private static void play(String configuredId, Minecraft client) {
        try {
            String value = configuredId == null ? "" : configuredId.trim();
            if (value.startsWith("custominv.")) {
                value = "custominv:" + value.substring("custominv.".length());
            }
            Identifier id = Identifier.tryParse(value);
            if (id == null) return;
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(id);
            if (event == null) {
                System.err.println("[Audiora] UI sound event not registered: " + id);
                return;
            }
            client.getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0F));
        } catch (Exception exception) {
            System.err.println("[Audiora] Could not play inventory UI sound: " + exception.getMessage());
        }
    }

    private static void load() {
        try {
            Files.createDirectories(FILE.getParent());
            if (!Files.exists(FILE)) {
                saveDefaults();
            }
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            enabled = !root.has("enabled") || root.get("enabled").getAsBoolean();
            if (root.has("open")) openSound = root.get("open").getAsString();
            if (root.has("close")) closeSound = root.get("close").getAsString();
        } catch (Exception exception) {
            System.err.println("[Audiora] Could not load ui.json; using default inventory sounds: " + exception.getMessage());
        }
    }

    private static void saveDefaults() throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", true);
        root.addProperty("open", "custominv.inv.buka");
        root.addProperty("close", "custominv.inv.tutup");
        Files.writeString(FILE, root.toString());
    }
}
