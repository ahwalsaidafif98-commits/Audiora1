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

    private AudioraInfoHud() {}

    public static void initialize() {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("audiora_controller", "mob_item_info"),
            AudioraInfoHud::extract
        );
    }

    public static void tick(Minecraft client) {
        if (client.player == null) {
            previousCounts.clear();
            return;
        }

        if (client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living) {
            float health = living.getHealth();
            if (living.getId() == lastTargetId && lastTargetHealth >= 0.0F && health < lastTargetHealth) {
                recentDamage = lastTargetHealth - health;
                damageTicks = 25;
            }
            lastTargetId = living.getId();
            lastTargetHealth = health;
        } else {
            lastTargetId = -1;
            lastTargetHealth = -1.0F;
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
                        if (isOreDrop(itemPath)) oreCounts.merge(itemPath, gained, Integer::sum);
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
        drawScaledText(graphics, client, coordinates, left + AudioraConfig.hudX[0], top + AudioraConfig.hudY[0], 0xFFFFFFFF, 0);

        String biome = client.level.getBiome(player.blockPosition()).unwrapKey()
            .map(key -> key.toString().replace("ResourceKey[minecraft:", "").replace("]", "").replace('_', ' '))
            .orElse("unknown");
        drawScaledText(graphics, client, "Biome: " + biome, left + AudioraConfig.hudX[1], top + 12 + AudioraConfig.hudY[1], 0xFFB8D8FF, 1);

        int armorTotal = 0;
        int armorPieces = 0;
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[] {
            net.minecraft.world.entity.EquipmentSlot.HEAD,
            net.minecraft.world.entity.EquipmentSlot.CHEST,
            net.minecraft.world.entity.EquipmentSlot.LEGS,
            net.minecraft.world.entity.EquipmentSlot.FEET
        }) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.isDamageableItem()) {
                int remaining = Math.max(0, armor.getMaxDamage() - armor.getDamageValue());
                armorTotal += Math.round(100.0F * remaining / armor.getMaxDamage());
                armorPieces++;
            }
        }
        if (armorPieces > 0) {
            drawScaledText(graphics, client, "Armor: " + (armorTotal / armorPieces) + "%",
                left + AudioraConfig.hudX[2], top + 24 + AudioraConfig.hudY[2], 0xFFE8D58A, 2);
        }

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) {
            String heldLabel = held.getHoverName().getString();
            if (held.isDamageableItem()) {
                int remaining = Math.max(0, held.getMaxDamage() - held.getDamageValue());
                int percent = Math.round(100.0F * remaining / held.getMaxDamage());
                heldLabel += " " + percent + "%";
            }
            int heldX = left + AudioraConfig.hudX[3];
            int heldY = top + (armorPieces > 0 ? 36 : 24) + AudioraConfig.hudY[3];
            float heldScale = AudioraConfig.hudScale[3] / 100.0F;
            graphics.pose().pushMatrix();
            graphics.pose().translate(heldX, heldY);
            graphics.pose().scale(heldScale, heldScale);
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
                renderTargetCard(graphics, client, living, deltaTracker);
            }
        }

        if (pickupAmount > 0 && System.currentTimeMillis() < pickupUntil) {
            String label = pickupName + " ×" + pickupAmount;
            int width = client.font.width(label);
            int x = (graphics.guiWidth() - width - 34) / 2 + AudioraConfig.itemInfoOffsetX + AudioraConfig.hudX[5];
            int y = graphics.guiHeight() - 64 + AudioraConfig.itemInfoOffsetY + AudioraConfig.hudY[5];
            float scale = AudioraConfig.hudScale[5] / 100.0F;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(scale, scale);
            if (!pickupStack.isEmpty()) graphics.item(pickupStack, 4, 3);
            graphics.text(client.font, label, 24, 7, 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        } else {
            pickupAmount = 0;
            pickupStack = ItemStack.EMPTY;
        }

        if (damageTicks > 0) {
            String damageText = String.format(java.util.Locale.ROOT, "-%.1f", recentDamage);
            int damageX = graphics.guiWidth() / 2 - client.font.width(damageText) / 2
                + AudioraConfig.hudX[8];
            int damageY = graphics.guiHeight() / 2 - 42 + AudioraConfig.hudY[8];
            float damageScale = AudioraConfig.hudScale[8] / 100.0F;
            graphics.pose().pushMatrix();
            graphics.pose().translate(damageX, damageY - (25 - damageTicks) / 2.0F);
            graphics.pose().scale(damageScale, damageScale);
            graphics.text(client.font, damageText, 0, 0, 0xFFFF6868, true);
            graphics.pose().popMatrix();
        }

        if (!oreCounts.isEmpty()) {
            int line = 0;
            int totalOres = oreCounts.values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<String, Integer> ore : oreCounts.entrySet()) {
                if (line >= 5) break;
                int share = totalOres > 0 ? Math.round(100.0F * ore.getValue() / totalOres) : 0;
                String oreLabel = ore.getKey().replace('_', ' ') + ": " + ore.getValue() + " (" + share + "%)";
                graphics.text(client.font, oreLabel, graphics.guiWidth() - client.font.width(oreLabel) - 6,
                    6 + line * 12, 0xFFE8D58A, true);
                line++;
            }
        }
    }

    private static boolean isOreDrop(String path) {
        return path.endsWith("_ore") || path.equals("ancient_debris")
            || path.startsWith("raw_") || path.equals("diamond") || path.equals("emerald")
            || path.equals("lapis_lazuli") || path.equals("redstone") || path.equals("quartz")
            || path.equals("amethyst_shard") || path.equals("coal") || path.equals("copper_ingot")
            || path.equals("iron_ingot") || path.equals("gold_ingot");
    }

    private static void renderTargetCard(
        GuiGraphicsExtractor graphics,
        Minecraft client,
        LivingEntity target,
        DeltaTracker deltaTracker
    ) {
        String name = target.getName().getString();
        float maxHealth = Math.max(1.0F, target.getMaxHealth());
        float health = Math.max(0.0F, Math.min(maxHealth, target.getHealth()));
        float healthRatio = health / maxHealth;

        int cardWidth = Math.max(150, client.font.width(name) + 64);
        int cardHeight = 48;
        int x = (graphics.guiWidth() - cardWidth) / 2 + AudioraConfig.mobInfoOffsetX + AudioraConfig.hudX[4];
        int y = 12 + AudioraConfig.mobInfoOffsetY + AudioraConfig.hudY[4];

        float scale = AudioraConfig.hudScale[4] / 100.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.outline(0, 0, cardWidth, cardHeight, 0x88777777);

        try {
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
            EntityRenderState state = client.getEntityRenderDispatcher().extractEntity(target, partialTick);
            // Reverse the preview's horizontal facing so the mob/player is no longer shown from the mirrored side.
            graphics.entity(
                state,
                18.0F,
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
        float scale = AudioraConfig.hudScale[hudIndex] / 100.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(client.font, text, 0, 0, color, true);
        graphics.pose().popMatrix();
    }
}