package id.audiora.controller;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;

public final class AudioraCombo {
    private static final int COMBO_TIMEOUT_TICKS = 50;
    private static int combo;
    private static int displayTicks;
    private static int pendingTargetId = -1;
    private static int pendingTicks;
    private static boolean confirmedCritical;

    private AudioraCombo() {}

    public static void initialize() {
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (player != Minecraft.getInstance().player) return InteractionResult.PASS;
            boolean candidate = target instanceof LivingEntity living && living.isAlive()
                && player.fallDistance > 0.0F
                && !player.onGround()
                && !player.isInWater()
                && !player.isPassenger()
                && !player.isSprinting()
                && !player.hasEffect(MobEffects.BLINDNESS)
                && player.getAttackStrengthScale(0.5F) > 0.9F;
            pendingTargetId = candidate ? target.getId() : -1;
            pendingTicks = candidate ? 6 : 0;
            return InteractionResult.PASS;
        });

        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("audiora_controller", "crit_combo"),
            (graphics, deltaTracker) -> {
                Minecraft client = Minecraft.getInstance();
                if (client.player == null) return;
                int screenWidth = client.getWindow().getGuiScaledWidth();
                int screenHeight = client.getWindow().getGuiScaledHeight();
                if (confirmedCritical && AudioraConfig.hudEnabled[9] && displayTicks > 0) {
                    String criticalLabel = "CRITICAL!";
                    int criticalX = (screenWidth - client.font.width(criticalLabel)) / 2
                        + AudioraConfig.hudX[9];
                    int criticalY = screenHeight / 2 - 28 + AudioraConfig.hudY[9];
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(criticalX, criticalY);
                    graphics.pose().scale(AudioraConfig.hudScaleX(9), AudioraConfig.hudScaleY(9));
                    graphics.text(client.font, criticalLabel, 0, 0, 0xFFFFD54A, true);
                    graphics.pose().popMatrix();
                }
                if (!AudioraConfig.comboEnabled || !AudioraConfig.hudEnabled[7]
                    || displayTicks <= 0 || combo <= 0) return;

                String label = "COMBO " + combo + "X";
                int labelWidth = client.font.width(label);
                int x = (screenWidth - labelWidth) / 2 + AudioraConfig.comboOffsetX + AudioraConfig.hudX[7];
                int y = screenHeight / 2 + AudioraConfig.comboOffsetY + AudioraConfig.hudY[7];
                graphics.pose().pushMatrix();
                graphics.pose().translate(x, y);
                graphics.pose().scale(AudioraConfig.hudScaleX(7), AudioraConfig.hudScaleY(7));
                graphics.text(client.font, label, 0, 14,
                    combo >= 5 ? 0xFFFFD54A : 0xFFFFFFFF, true);
                int barWidth = Math.max(0, Math.round(labelWidth * (displayTicks / (float)COMBO_TIMEOUT_TICKS)));
                int barY = 14 + client.font.lineHeight + 2;
                if (barWidth > 0) graphics.fill(0, barY, barWidth, barY + 2,
                    combo >= 5 ? 0xFFFFD54A : 0xFFFFFFFF);
                graphics.pose().popMatrix();
            }
        );
    }

    /** Called only after the client receives a real health decrease for the attacked entity. */
    public static void onConfirmedDamage(int entityId) {
        if (pendingTicks > 0 && pendingTargetId == entityId) {
            combo++;
            displayTicks = COMBO_TIMEOUT_TICKS;
            confirmedCritical = true;
        } else {
            combo = 0;
            displayTicks = 0;
            confirmedCritical = false;
        }
        pendingTargetId = -1;
        pendingTicks = 0;
    }

    public static void tick() {
        if (pendingTicks > 0 && --pendingTicks == 0) pendingTargetId = -1;
        if (displayTicks > 0 && --displayTicks == 0) {
            combo = 0;
            confirmedCritical = false;
        }
    }
}
