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
        graphics.text(client.font, coordinates, left + AudioraConfig.hudX[0], top + AudioraConfig.hudY[0], 0xFFFFFFFF, true);

        String biome = client.level.getBiome(player.blockPosition()).unwrapKey()
            .map(key -> key.toString().replace("ResourceKey[minecraft:", "").replace("]", "").replace('_', ' '))
            .orElse("unknown");
        graphics.text(client.font, "Biome: " + biome, left + AudioraConfig.hudX[1], top + 12 + AudioraConfig.hudY[1], 0xFFB8D8FF, true);

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
            graphics.text(client.font, "Armor: " + (armorTotal / armorPieces) + "%",
                left + AudioraConfig.hudX[2], top + 24 + AudioraConfig.hudY[2], 0xFFE8D58A, true);
        }

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) {
            String heldLabel = held.getHoverName().getString();
            if (held.isDamageableItem()) {
                int remaining = Math.max(0, held.getMaxDamage() - held.getDamageValue());
                int percent = Math.round(100.0F * remaining / held.getMaxDamage());
                heldLabel += " " + percent + "%";
            }
            graphics.text(client.font, heldLabel, left, top + (armorPieces > 0 ? 36 : 24),
                0xFFFFFFFF, true);
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
            graphics.fill(x, y, x + width + 30, y + 22, 0xB9000000);
            if (!pickupStack.isEmpty()) graphics.item(pickupStack, x + 4, y + 3);
            graphics.text(client.font, label, x + 24, y + 7, 0xFFFFFFFF, true);
        } else {
            pickupAmount = 0;
            pickupStack = ItemStack.EMPTY;
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

        graphics.fill(x, y, x + cardWidth, y + cardHeight, 0xB9000000);
        graphics.outline(x, y, cardWidth, cardHeight, 0xFF777777);

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
                x + 4,
                y + 3,
                x + 43,
                y + 45
            );
        } catch (RuntimeException exception) {
            // Keep the target name and health visible if a custom entity renderer cannot be previewed.
        }

        int textX = x + 49;
        int textY = y + 7;
        graphics.text(client.font, name, textX, textY, 0xFFFFFFFF, true);

        String healthLabel = String.format(java.util.Locale.ROOT, "%.1f / %.1f HP", health, maxHealth);
        graphics.text(client.font, healthLabel, textX, textY + 13, 0xFFE0E0E0, true);

        int barX = textX;
        int barY = y + 34;
        int barWidth = cardWidth - 58;
        graphics.fill(barX, barY, barX + barWidth, barY + 6, 0xFF3A3A3A);
        int filledWidth = Math.round(barWidth * healthRatio);
        if (filledWidth > 0) {
            int barColor = healthRatio > 0.5F ? 0xFF48D16A : healthRatio > 0.25F ? 0xFFFFC247 : 0xFFE64B4B;
            graphics.fill(barX, barY, barX + filledWidth, barY + 6, barColor);
        }
    }
}