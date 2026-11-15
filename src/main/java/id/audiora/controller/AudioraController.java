package id.audiora.controller;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;

public final class AudioraController implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("audiora_controller", "main"));
    private static KeyMapping openSettings;
    private static boolean wasOnGround;

    @Override
    public void onInitializeClient() {
        AudioraConfig.load();
        AudioraAmbience.initialize();
        AudioraCombo.initialize();
        AudioraInfoHud.initialize();
        openSettings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.audiora_controller.open_settings",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_O,
            CATEGORY
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AudioraAmbience.tick(client);
            AudioraCombo.tick();
            AudioraInfoHud.tick(client);
            tickJumpSound(client);
            while (openSettings.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    client.gui.setScreen(new AudioraScreen());
                }
            }
        });
        System.out.println("[Audiora] Sound controller initialized. Press O to open settings.");
    }

    private static void tickJumpSound(Minecraft client) {
        if (client.player == null) {
            wasOnGround = false;
            return;
        }
        boolean onGround = client.player.onGround();
        if (wasOnGround && !onGround && client.player.getDeltaMovement().y > 0.0) {
            if (AudioraConfig.masterEnabled && AudioraConfig.scaleForSound(AudioraSounds.JUMP_ID.toString()) > 0.0f) {
                client.getSoundManager().play(SimpleSoundInstance.forUI(AudioraSounds.JUMP, 1.0F));
            }
        }
        wasOnGround = onGround;
    }
}
