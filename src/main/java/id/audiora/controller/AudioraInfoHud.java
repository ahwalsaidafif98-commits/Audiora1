package id.audiora.controller;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class AudioraInfoHud {
    private static final Map<String, Integer> previousCounts = new HashMap<>();
    private static String pickupName = "";
    private static int pickupAmount;
    private static ItemStack pickupStack = ItemStack.EMPTY;
    private static final Map<String, Integer> oreCounts = new HashMap<>();
    private static long pickupUntil;
    private static float lastTargetHealth = -1.0F;
    private static int lastTargetId = -1;
    private static float recentDamage;
    private static int damageTicks;
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;

    private AudioraInfoHud() {}

    public static void initialize() {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("audiora_controller", "mob_item_info"),
            AudioraInfoHud::extract
        );
    }

    public static void tick(Minecraft client) {
        if (client.level != lastLevel) {
            lastLevel = client.level;
            previousCounts.clear();
            oreCounts.clear();
            pickupName = "";
            pickupAmount = 0;
            pickupStack = ItemStack.EMPTY;
            pickupUntil = 0L;
            lastTargetHealth = -1.0F;
            lastTargetId = -1;
            recentDamage = 0.0F;
            damageTicks = 0;
        }
        if (client.player == null) {
            previousCounts.clear();
            return;
        }

        if (damageTicks > 0) damageTicks--;

        Inventory inventory = client.player.getInventory();
        Map<String, Integer> current = new HashMap<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            String key = stack.getItem().toString();
            current.merge(key, stack.getCount(), Integer::sum);
        }

        if (!previousCounts.isEmpty()) {
            for (Map.Entry<String, Integer> entry : current.entrySet()) {
                int gained = entry.getValue() - previousCounts.getOrDefault(entry.getKey(), 0);
                if (gained > 0) {
                    ItemStack found = findStack(inventory, entry.getKey());
                    if (!found.isEmpty()) {
                        pickupName = found.getHoverName().getString();
                        pickupStack = found.copy();
                        pickupStack.setCount(1);
                        pickupAmount = gained;
                        String itemPath = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(found.getItem()).getPath();
                        pickupUntil = System.currentTimeMillis() + 2200L;
                    }
                }
            }
        }
        previousCounts.clear();
        previousCounts.putAll(current);
    }

    private static ItemStack findStack(Inventory inventory, String itemId) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem().toString().equals(itemId)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null) return;

        // Compact information HUDs requested for Audiora 1.0.3.
        int left = 6;
        int top = 6;
        var player = client.player;
        String coordinates = String.format(java.util.Locale.ROOT, "XYZ: %.1f / %.1f / %.1f",
            player.getX(), player.getY(), player.getZ());
        if (AudioraConfig.hudEnabled[0]) drawScaledText(graphics, client, coordinates, left + AudioraConfig.hudX[0], top + AudioraConfig.hudY[0], 0xFFFFFFFF, 0);

        String biome = client.level.getBiome(player.blockPosition()).unwrapKey()
            .map(key -> key.toString().replace("ResourceKey[minecraft:", "").replace("]", "").replace('_', ' '))
            .orElse("unknown");
        if (AudioraConfig.hudEnabled[1]) drawScaledText(graphics, client, "Biome: " + biome, left + AudioraConfig.hudX[1], top + 12 + AudioraConfig.hudY[1], 0xFFB8D8FF, 1);

        if (AudioraConfig.hudEnabled[12]) {
            String facing = player.getDirection().getName().toUpperCase(java.util.Locale.ROOT);
            int compassX = left + AudioraConfig.hudX[12];
            int compassY = top + 78 + AudioraConfig.hudY[12];
            graphics.pose().pushMatrix();
            graphics.pose().translate(compassX, compassY);
            graphics.pose().scale(AudioraConfig.hudScaleX(12), AudioraConfig.hudScaleY(12));
            graphics.text(client.font, "Facing: " + facing, 0, 0, 0xFF9FE7FF, true);
            graphics.pose().popMatrix();
        }

        int armorPieces = 0;
        if (AudioraConfig.hudEnabled[2]) {
            int armorX = left + AudioraConfig.hudX[2];
            int armorY = top + 24 + AudioraConfig.hudY[2];
            graphics.pose().pushMatrix();
            graphics.pose().translate(armorX, armorY);
            graphics.pose().scale(AudioraConfig.hudScaleX(2), AudioraConfig.hudScaleY(2));
            for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[] {
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET
            }) {
                ItemStack armor = player.getItemBySlot(slot);
                if (armor.isEmpty() || !armor.isDamageableItem()) continue;
                int remaining = Math.max(0, armor.getMaxDamage() - armor.getDamageValue());
                int percent = Math.round(100.0F * remaining / armor.getMaxDamage());
                int rowY = armorPieces * 14;
                graphics.item(armor, 0, rowY - 2);
                graphics.text(client.font, armor.getHoverName().getString() + " " + percent + "%",
                    20, rowY, 0xFFE8D58A, true);
                int barWidth = 64;
                graphics.fill(20, rowY + 10, 20 + barWidth, rowY + 12, 0xFF444444);
                graphics.fill(20, rowY + 10, 20 + Math.round(barWidth * percent / 100.0F), rowY + 12,
                    percent > 50 ? 0xFF55D66B : percent > 20 ? 0xFFFFC247 : 0xFFE64B4B);
                armorPieces++;
            }
            graphics.pose().popMatrix();
        }

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && AudioraConfig.hudEnabled[3]) {
            String heldLabel = held.getHoverName().getString();
            if (held.isDamageableItem()) {
                int remaining = Math.max(0, held.getMaxDamage() - held.getDamageValue());
                int percent = Math.round(100.0F * remaining / held.getMaxDamage());
                heldLabel += " " + percent + "%";
            }
            int heldX = left + AudioraConfig.hudX[3];
            int heldY = top + (armorPieces > 0 ? 84 : 24) + AudioraConfig.hudY[3];
            graphics.pose().pushMatrix();
            graphics.pose().translate(heldX, heldY);
            graphics.pose().scale(AudioraConfig.hudScaleX(3), AudioraConfig.hudScaleY(3));
            graphics.item(held, 0, 0);
            graphics.text(client.font, heldLabel, 20, 5, 0xFFFFFFFF, true);
            if (held.isDamageableItem()) {
                int remaining = Math.max(0, held.getMaxDamage() - held.getDamageValue());
                int percent = Math.round(100.0F * remaining / held.getMaxDamage());
                int barWidth = 80;
                graphics.fill(20, 17, 20 + barWidth, 20, 0xFF444444);
                graphics.fill(20, 17, 20 + Math.round(barWidth * percent / 100.0F), 20,
                    percent > 50 ? 0xFF55D66B : percent > 20 ? 0xFFFFC247 : 0xFFE64B4B);
            }
            graphics.pose().popMatrix();
        }

        if (client.hitResult instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (entity instanceof LivingEntity living && living.isAlive()) {
                if (living instanceof net.minecraft.world.entity.player.Player) {
                    if (AudioraConfig.hudEnabled[11]) renderTargetCard(graphics, client, living, deltaTracker, 11);
                } else if (AudioraConfig.hudEnabled[4]) {
                    renderTargetCard(graphics, client, living, deltaTracker, 4);
                }
            }
        }

        if (AudioraConfig.hudEnabled[5] && pickupAmount > 0 && System.currentTimeMillis() < pickupUntil) {
            String label = pickupName + " ×" + pickupAmount;
            int width = client.font.width(label);
            int x = (graphics.guiWidth() - width - 34) / 2 + AudioraConfig.itemInfoOffsetX + AudioraConfig.hudX[5];
            int y = graphics.guiHeight() - 64 + AudioraConfig.itemInfoOffsetY + AudioraConfig.hudY[5];
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(AudioraConfig.hudScaleX(5), AudioraConfig.hudScaleY(5));
            if (!pickupStack.isEmpty()) graphics.item(pickupStack, 4, 3);
            graphics.text(client.font, label, 24, 7, 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        } else {
            pickupAmount = 0;
            pickupStack = ItemStack.EMPTY;
        }

        if (AudioraConfig.hudEnabled[8] && damageTicks > 0) {
            String damageText = String.format(java.util.Locale.ROOT, "-%.1f", recentDamage);
            int damageX = graphics.guiWidth() / 2 - client.font.width(damageText) / 2
                + AudioraConfig.hudX[8];
            int damageY = graphics.guiHeight() / 2 - 42 + AudioraConfig.hudY[8];
            graphics.pose().pushMatrix();
            graphics.pose().translate(damageX, damageY - (25 - damageTicks) / 2.0F);
            graphics.pose().scale(AudioraConfig.hudScaleX(8), AudioraConfig.hudScaleY(8));
            graphics.text(client.font, damageText, 0, 0, 0xFFFF6868, true);
            graphics.pose().popMatrix();
        }

        if (AudioraConfig.hudEnabled[6] && !oreCounts.isEmpty()) {
            int line = 0;
            int totalOres = oreCounts.values().stream().mapToInt(Integer::intValue).sum();
            int maxWidth = 0;
            for (Map.Entry<String, Integer> ore : oreCounts.entrySet()) {
                if (line >= 5) break;
                int share = totalOres > 0 ? Math.round(100.0F * ore.getValue() / totalOres) : 0;
                String oreLabel = ore.getKey().replace('_', ' ') + ": " + ore.getValue() + " (" + share + "%)";
                maxWidth = Math.max(maxWidth, client.font.width(oreLabel) + 22);
                line++;
            }
            int oreX = graphics.guiWidth() - maxWidth - 6 + AudioraConfig.hudX[6];
            int oreY = 6 + AudioraConfig.hudY[6];
            graphics.pose().pushMatrix();
            graphics.pose().translate(oreX, oreY);
            graphics.pose().scale(AudioraConfig.hudScaleX(6), AudioraConfig.hudScaleY(6));
            line = 0;
            for (Map.Entry<String, Integer> ore : oreCounts.entrySet()) {
                if (line >= 5) break;
                int share = totalOres > 0 ? Math.round(100.0F * ore.getValue() / totalOres) : 0;
                String oreLabel = ore.getKey().replace('_', ' ') + ": " + ore.getValue() + " (" + share + "%)";
                int iconX = maxWidth - client.font.width(oreLabel) - 20;
                Identifier oreItemId = Identifier.fromNamespaceAndPath("minecraft", ore.getKey());
                var oreBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(oreItemId);
                ItemStack oreIcon = oreBlock.map(reference -> new ItemStack(reference.value().asItem())).orElse(ItemStack.EMPTY);
                if (!oreIcon.isEmpty()) graphics.item(oreIcon, iconX, line * 12 - 2);
                graphics.text(client.font, oreLabel, maxWidth - client.font.width(oreLabel), line * 12, 0xFFE8D58A, true);
                line++;
            }
            graphics.pose().popMatrix();
        }

        if (AudioraConfig.hudEnabled[10] && !player.getActiveEffects().isEmpty()) {
            int potionX = 6 + AudioraConfig.hudX[10];
            int potionY = graphics.guiHeight() / 2 + AudioraConfig.hudY[10];
            graphics.pose().pushMatrix();
            graphics.pose().translate(potionX, potionY);
            graphics.pose().scale(AudioraConfig.hudScaleX(10), AudioraConfig.hudScaleY(10));
            int line = 0;
            for (var effect : player.getActiveEffects()) {
                if (line >= 8) break;
                String effectName = effect.getEffect().value().getDisplayName().getString();
                int seconds = Math.max(0, effect.getDuration() / 20);
                String duration = String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
                graphics.text(client.font, effectName + " " + duration, 0, line * 12, 0xFFD8C8FF, true);
                line++;
            }
            graphics.pose().popMatrix();
        }
    }

    public static void onLivingEntityHealthChanged(LivingEntity entity, float oldHealth, float newHealth) {
        if (newHealth >= oldHealth || !entity.level().isClientSide()) return;
        recentDamage = oldHealth - newHealth;
        damageTicks = 25;
        AudioraCombo.onConfirmedDamage(entity.getId());
    }

    public static void recordMinedBlock(net.minecraft.world.level.block.state.BlockState state) {
        if (state == null || state.isAir()) return;
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK
            .getKey(state.getBlock()).getPath();
        if (path.endsWith("_ore") || path.equals("ancient_debris")) {
            oreCounts.merge(path, 1, Integer::sum);
        }
    }

    private static void renderTargetCard(
        GuiGraphicsExtractor graphics,
        Minecraft client,
        LivingEntity target,
        DeltaTracker deltaTracker,
        int hudIndex
    ) {
        String name = target.getName().getString();
        float maxHealth = Math.max(1.0F, target.getMaxHealth());
        float health = Math.max(0.0F, Math.min(maxHealth, target.getHealth()));
        float healthRatio = health / maxHealth;

        int cardWidth = Math.max(150, client.font.width(name) + 64);
        int cardHeight = 48;
        // Keep mob and player target cards on opposite sides so their previews never overlap.
        int x = (hudIndex == 4
            ? 10
            : graphics.guiWidth() - cardWidth - 10)
            + AudioraConfig.mobInfoOffsetX + AudioraConfig.hudX[hudIndex];
        int y = 12 + AudioraConfig.mobInfoOffsetY + AudioraConfig.hudY[hudIndex];

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(AudioraConfig.hudScaleX(hudIndex), AudioraConfig.hudScaleY(hudIndex));
        graphics.outline(0, 0, cardWidth, cardHeight, 0x88777777);

        try {
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
            EntityRenderState state = client.getEntityRenderDispatcher().extractEntity(target, partialTick);
            // Reverse the preview's horizontal facing so the mob/player is no longer shown from the mirrored side.
            graphics.entity(
                state,
                hudIndex == 4 ? 12.0F : 18.0F,
                new Vector3f(0.0F, 0.0F, 0.0F),
                new Quaternionf().rotationZ((float) Math.PI).rotateY(0.35F),
                null,
                4,
                3,
                43,
                45
            );
        } catch (RuntimeException exception) {
            // Keep the target name and health visible if a custom entity renderer cannot be previewed.
        }

        int textX = 49;
        int textY = 7;
        graphics.text(client.font, name, textX, textY, 0xFFFFFFFF, true);

        String healthLabel = String.format(java.util.Locale.ROOT, "%.1f / %.1f HP", health, maxHealth);
        graphics.text(client.font, healthLabel, textX, textY + 13, 0xFFE0E0E0, true);

        int barX = textX;
        int barY = 34;
        int barWidth = cardWidth - 58;
        graphics.fill(barX, barY, barX + barWidth, barY + 6, 0xFF3A3A3A);
        int filledWidth = Math.round(barWidth * healthRatio);
        if (filledWidth > 0) {
            int barColor = healthRatio > 0.5F ? 0xFF48D16A : healthRatio > 0.25F ? 0xFFFFC247 : 0xFFE64B4B;
            graphics.fill(barX, barY, barX + filledWidth, barY + 6, barColor);
        }
        graphics.pose().popMatrix();
    }

    private static void drawScaledText(GuiGraphicsExtractor graphics, Minecraft client, String text,
                                       int x, int y, int color, int hudIndex) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(AudioraConfig.hudScaleX(hudIndex), AudioraConfig.hudScaleY(hudIndex));
        graphics.text(client.font, text, 0, 0, color, true);
        graphics.pose().popMatrix();
    }
}