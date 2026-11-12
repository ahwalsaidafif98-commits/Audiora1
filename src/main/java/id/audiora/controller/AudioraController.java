package id.audiora.controller;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class AudioraController implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("audiora_controller", "main"));
    private static KeyMapping openSettings;

    @Override
    public void onInitializeClient() {
        AudioraConfig.load();
        openSettings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.audiora_controller.open_settings",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_O,
            CATEGORY
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettings.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    client.gui.setScreen(new AudioraScreen());
                }
            }
        });
        System.out.println("[Audiora] Sound Controller initialized. Press O to open settings.");
    }
}