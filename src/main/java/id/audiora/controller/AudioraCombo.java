package id.audiora.controller;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;

public final class AudioraCombo {
    private static int combo = 0;
    private static int displayTicks = 0;

    private AudioraCombo() {}

    public static void initialize() {
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (player != Minecraft.getInstance().player) return InteractionResult.PASS;

            boolean criticalCandidate =
                target instanceof LivingEntity living && living.isAlive()
                && player.fallDistance > 0.0F
                && !player.onGround()
                && !player.isInWater()
                && !player.isPassenger()
                && !player.isSprinting()
                && !player.hasEffect(MobEffects.BLINDNESS)
                && player.getAttackStrengthScale(0.5F) > 0.9F;

            if (criticalCandidate) {
                combo++;
                displayTicks = 50;
            } else {
                combo = 0;
                displayTicks = 0;
            }
            return InteractionResult.PASS;
        });

        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("audiora_controller", "crit_combo"),
            (graphics, deltaTracker) -> {
                Minecraft client = Minecraft.getInstance();
                if (!AudioraConfig.comboEnabled || client.player == null || displayTicks <= 0 || combo <= 0) return;
                String label = "COMBO " + combo + "X";
                int x = (client.getWindow().getGuiScaledWidth() - client.font.width(label)) / 2;
                int y = client.getWindow().getGuiScaledHeight() / 2 + 24;
                graphics.text(client.font, label, x, y, combo >= 5 ? 0xFFFFD54A : 0xFFFFFFFF, true);
            }
        );
    }

    public static void tick() {
        if (displayTicks > 0) {
            displayTicks--;
            if (displayTicks == 0) combo = 0;
        }
    }
}
