package id.audiora.controller;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class AudioraController implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("audiora_controller", "main"));
    private static KeyMapping openSettings;
    private static boolean wasOnGround;
    private static int lastSelectedSlot = -1;
    private static int netheriteStepCooldown;

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
            tickHotbarItemSound(client);
            tickNetheriteFootsteps(client);
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
            playAudioraSound(client, AudioraSounds.JUMP, AudioraSounds.JUMP_ID, 1.0F);
        }
        wasOnGround = onGround;
    }

    private static void tickHotbarItemSound(Minecraft client) {
        if (client.player == null) {
            lastSelectedSlot = -1;
            return;
        }
        int selectedSlot = client.player.getInventory().getSelectedSlot();
        if (lastSelectedSlot == -1) {
            lastSelectedSlot = selectedSlot;
            return;
        }
        if (selectedSlot == lastSelectedSlot) return;
        lastSelectedSlot = selectedSlot;

        ItemStack stack = client.player.getInventory().getSelectedItem();
        if (stack.isEmpty()) return;
        String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        SoundEvent sound = null;
        Identifier soundId = null;

        if (item.endsWith("_sword")) {
            sound = AudioraSounds.TOOL_SWORD; soundId = AudioraSounds.TOOL_SWORD_ID;
        } else if (item.endsWith("_pickaxe")) {
            sound = AudioraSounds.TOOL_PICKAXE; soundId = AudioraSounds.TOOL_PICKAXE_ID;
        } else if (item.endsWith("_axe")) {
            sound = AudioraSounds.TOOL_AXE; soundId = AudioraSounds.TOOL_AXE_ID;
        } else if (item.endsWith("_shovel")) {
            sound = AudioraSounds.TOOL_SHOVEL; soundId = AudioraSounds.TOOL_SHOVEL_ID;
        } else if (item.endsWith("_hoe")) {
            sound = AudioraSounds.TOOL_HOE; soundId = AudioraSounds.TOOL_HOE_ID;
        } else if (item.equals("spear") || item.endsWith("_spear")) {
            sound = AudioraSounds.TOOL_SPEAR; soundId = AudioraSounds.TOOL_SPEAR_ID;
        } else if (item.equals("mace")) {
            sound = AudioraSounds.TOOL_MACE; soundId = AudioraSounds.TOOL_MACE_ID;
        } else if (item.equals("trident")) {
            sound = AudioraSounds.TOOL_TRIDENT; soundId = AudioraSounds.TOOL_TRIDENT_ID;
        }

        if (sound != null) playAudioraSound(client, sound, soundId, 0.8F);
    }

    private static void tickNetheriteFootsteps(Minecraft client) {
        if (client.player == null) {
            netheriteStepCooldown = 0;
            return;
        }
        if (netheriteStepCooldown > 0) netheriteStepCooldown--;
        if (netheriteStepCooldown > 0 || !client.player.onGround()
            || client.player.getDeltaMovement().horizontalDistanceSqr() < 0.0004
            || !hasFullNetheriteArmor(client)) return;

        playAudioraSound(client, AudioraSounds.NETHERITE_STEP, AudioraSounds.NETHERITE_STEP_ID, 0.75F);
        netheriteStepCooldown = 10;
    }

    private static boolean hasFullNetheriteArmor(Minecraft client) {
        return isNetherite(client.player.getItemBySlot(EquipmentSlot.HEAD), "helmet")
            && isNetherite(client.player.getItemBySlot(EquipmentSlot.CHEST), "chestplate")
            && isNetherite(client.player.getItemBySlot(EquipmentSlot.LEGS), "leggings")
            && isNetherite(client.player.getItemBySlot(EquipmentSlot.FEET), "boots");
    }

    private static boolean isNetherite(ItemStack stack, String piece) {
        return !stack.isEmpty()
            && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("netherite_" + piece);
    }

    private static void playAudioraSound(Minecraft client, SoundEvent sound, Identifier id, float volume) {
        if (AudioraConfig.scaleForSound(id.toString()) > 0.0F) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(sound, volume));
        }
    }
}
