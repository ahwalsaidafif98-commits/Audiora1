package id.audiora.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Plays Audiora's own inventory sounds. No third-party sound library is required.
 * Settings are stored in config/audiora/ui.json.
 */
public final class AudioraUiSounds {
    private static final Path FILE = Path.of("config", "audiora", "ui.json");
    private static String openSound = "audiora_controller:inv.buka";
    private static String closeSound = "audiora_controller:inv.tutup";
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
            String rawId = configuredId == null ? "" : configuredId.trim();
            // Migrate IDs written by the old external Sounds-mod configuration.
            if (rawId.equals("custominv.inv.buka")) rawId = "audiora_controller:inv.buka";
            if (rawId.equals("custominv.inv.tutup")) rawId = "audiora_controller:inv.tutup";
            Identifier id = Identifier.tryParse(rawId);
            if (id == null) return;
            SoundEvent event = id.equals(AudioraSounds.INVENTORY_OPEN_ID)
                ? AudioraSounds.INVENTORY_OPEN
                : id.equals(AudioraSounds.INVENTORY_CLOSE_ID)
                    ? AudioraSounds.INVENTORY_CLOSE
                    : null;
            if (event == null) {
                System.err.println("[Audiora] Unknown inventory sound ID: " + id);
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
            if (!Files.exists(FILE)) saveDefaults();
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            enabled = !root.has("enabled") || root.get("enabled").getAsBoolean();
            if (root.has("open")) openSound = root.get("open").getAsString();
            if (root.has("close")) closeSound = root.get("close").getAsString();
        } catch (Exception exception) {
            System.err.println("[Audiora] Could not load ui.json; using defaults: " + exception.getMessage());
        }
    }

    private static void saveDefaults() throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", true);
        root.addProperty("open", "audiora_controller:inv.buka");
        root.addProperty("close", "audiora_controller:inv.tutup");
        Files.writeString(FILE, root.toString());
    }
}