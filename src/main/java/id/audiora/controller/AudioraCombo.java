package id.audiora.controller;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;

public final class AudioraCombo {
    private static final int COMBO_TIMEOUT_TICKS = 50;
    private static int combo;
    private static int displayTicks;
    private static int pendingTargetId = -1;
    private static int pendingTicks;

    private AudioraCombo() {}

    public static void initialize() {
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (player != Minecraft.getInstance().player) return InteractionResult.PASS;
            boolean validTarget = target instanceof LivingEntity living && living.isAlive();
            pendingTargetId = validTarget ? target.getId() : -1;
            pendingTicks = validTarget ? 6 : 0;
            return InteractionResult.PASS;
        });

        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("audiora_controller", "combo"),
            (graphics, deltaTracker) -> {
                Minecraft client = Minecraft.getInstance();
                if (client.player == null || client.gui.screen() != null) return;
                if (!AudioraConfig.comboEnabled || !AudioraConfig.hudEnabled[7]
                    || displayTicks <= 0 || combo <= 0) return;

                int screenWidth = client.getWindow().getGuiScaledWidth();
                int screenHeight = client.getWindow().getGuiScaledHeight();
                String label = "COMBO " + combo + "X";
                int labelWidth = client.font.width(label);
                int x = (screenWidth - labelWidth) / 2 + AudioraConfig.comboOffsetX + AudioraConfig.hudX[7];
                int y = screenHeight / 2 + AudioraConfig.comboOffsetY + AudioraConfig.hudY[7];
                graphics.pose().pushMatrix();
                graphics.pose().translate(x, y);
                graphics.pose().scale(AudioraConfig.hudScaleX(7), AudioraConfig.hudScaleY(7));
                graphics.text(client.font, label, 0, 14,
                    combo >= 5 ? 0xFFFFD54A : 0xFFFFFFFF, true);
                int barWidth = Math.max(0, Math.round(labelWidth * (displayTicks / (float) COMBO_TIMEOUT_TICKS)));
                int barY = 14 + client.font.lineHeight + 2;
                if (barWidth > 0) graphics.fill(0, barY, barWidth, barY + 2,
                    combo >= 5 ? 0xFFFFD54A : 0xFFFFFFFF);
                graphics.pose().popMatrix();
            }
        );
    }

    /** Increases the combo only after the attacked entity actually loses health. */
    public static void onConfirmedDamage(int entityId) {
        if (pendingTicks > 0 && pendingTargetId == entityId) {
            combo++;
            displayTicks = COMBO_TIMEOUT_TICKS;
        } else {
            combo = 0;
            displayTicks = 0;
        }
        pendingTargetId = -1;
        pendingTicks = 0;
    }

    public static void tick() {
        if (pendingTicks > 0 && --pendingTicks == 0) pendingTargetId = -1;
        if (displayTicks > 0 && --displayTicks == 0) combo = 0;
    }
}
