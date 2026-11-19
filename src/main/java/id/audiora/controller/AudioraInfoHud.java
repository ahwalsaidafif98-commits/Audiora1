package id.audiora.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Deque;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
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
    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();
    private static boolean wasLeftMouseDown;
    private static boolean wasRightMouseDown;

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

    private static void tickKeystrokes(Minecraft client) {
        long now = System.currentTimeMillis();
        long window = client.getWindow().handle();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (leftDown && !wasLeftMouseDown) leftClicks.addLast(now);
        if (rightDown && !wasRightMouseDown) rightClicks.addLast(now);
        wasLeftMouseDown = leftDown;
        wasRightMouseDown = rightDown;
        while (!leftClicks.isEmpty() && now - leftClicks.peekFirst() > 1000L) leftClicks.removeFirst();
        while (!rightClicks.isEmpty() && now - rightClicks.peekFirst() > 1000L) rightClicks.removeFirst();
    }

    private static ItemStack findStack(Inventory inventory, String itemId) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem().toString().equals(itemId)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean shouldRenderHud(int index) {
        if (Minecraft.getInstance().gui.screen() instanceof AudioraHudScreen) return AudioraHudScreen.isPreviewing(index);
        return AudioraConfig.hudEnabled[index];
    }

    private static boolean isHudPreview(int index) {
        return Minecraft.getInstance().gui.screen() instanceof AudioraHudScreen && AudioraHudScreen.isPreviewing(index);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        boolean editingHud = client.gui.screen() instanceof AudioraHudScreen;
        if (client.player == null || (client.gui.screen() != null && !editingHud)) return;

        // Compact information HUDs requested for Audiora 1.0.3.
        int left = 6;
        int top = 6;
        var player = client.player;
        String coordinates = String.format(java.util.Locale.ROOT, "XYZ: %.1f / %.1f / %.1f",
            player.getX(), player.getY(), player.getZ());
        if (shouldRenderHud(0)) renderCoordinates(graphics, client, player, left, top);

        if (client.level != null && shouldRenderHud(1)) {
            String biome = client.level.getBiome(player.blockPosition()).unwrapKey()
                .map(key -> key.toString().replace("ResourceKey[minecraft:", "").replace("]", "").replace('_', ' '))
                .orElse("unknown");
            String biomeLabel = "Biome: " + biome;
            // During editing, keep the preview on the opposite side of the
            // settings panel so the panel cannot cover the selected HUD.
            int biomeX = editingHud
                ? graphics.guiWidth() - client.font.width(biomeLabel) - 8 + AudioraConfig.hudX[1]
                : left + AudioraConfig.hudX[1];
            drawScaledText(graphics, client, biomeLabel, biomeX,
                top + 12 + AudioraConfig.hudY[1], 0xFFB8D8FF, 1);
        }

        if (shouldRenderHud(12)) {
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
        if (shouldRenderHud(2)) {
            int armorX = left + AudioraConfig.hudX[2];
            int armorY = top + 24 + AudioraConfig.hudY[2];
            graphics.pose().pushMatrix();
            graphics.pose().translate(armorX, armorY);
            graphics.pose().scale(AudioraConfig.hudScaleX(2), AudioraConfig.hudScaleY(2));
            int armorPanelHeight = 8;
            for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[] {
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET
            }) {
                ItemStack armor = player.getItemBySlot(slot);
                if (!armor.isEmpty() && armor.isDamageableItem()) armorPanelHeight += 19;
            }
            // Armor durability has no panel background or outline.
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
                int rowY = 5 + armorPieces * 19;
                graphics.item(armor, 3, rowY - 3);
                graphics.text(client.font, percent + "%", 25, rowY, 0xFFE8F1FA, true);
                int barWidth = 79;
                graphics.fill(25, rowY + 10, 25 + barWidth, rowY + 13, 0xFF343F50);
                graphics.fill(25, rowY + 10, 25 + Math.round(barWidth * percent / 100.0F), rowY + 13,
                    percent > 50 ? 0xFF55D66B : percent > 20 ? 0xFFFFC247 : 0xFFE64B4B);
                armorPieces++;
            }
            graphics.pose().popMatrix();
        }

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && shouldRenderHud(3)) {
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
                    if (shouldRenderHud(4)) renderTargetCard(graphics, client, living, deltaTracker, 4);
                } else if (shouldRenderHud(4)) {
                    renderTargetCard(graphics, client, living, deltaTracker, 4);
                }
            }
        }

        if (shouldRenderHud(5) && pickupAmount > 0 && System.currentTimeMillis() < pickupUntil) {
            String label = pickupName + "  ×" + pickupAmount;
            int width = Math.max(112, client.font.width(label) + 36);
            int x = (graphics.guiWidth() - width) / 2 + AudioraConfig.itemInfoOffsetX + AudioraConfig.hudX[5];
            int y = graphics.guiHeight() - 66 + AudioraConfig.itemInfoOffsetY + AudioraConfig.hudY[5];
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(AudioraConfig.hudScaleX(5), AudioraConfig.hudScaleY(5));
            graphics.fill(0, 0, width, 25, 0xC9101725);
            graphics.outline(0, 0, width, 25, 0x8877A9D8);
            if (!pickupStack.isEmpty()) graphics.item(pickupStack, 5, 4);
            graphics.text(client.font, pickupName, 27, 4, 0xFFFFFFFF, true);
            graphics.text(client.font, "×" + pickupAmount, 27, 14, 0xFF9FCFFF, true);
            graphics.pose().popMatrix();
        } else {
            pickupAmount = 0;
            pickupStack = ItemStack.EMPTY;
        }

        if (shouldRenderHud(8) && damageTicks > 0) {
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

        if (shouldRenderHud(6) && (!oreCounts.isEmpty() || isHudPreview(6))) {
            int line = 0;
            int maxWidth = 0;
            java.util.List<ItemStack> oreItems = new java.util.ArrayList<>();
            for (String orePath : oreCounts.keySet()) {
                if (line >= 5) break;
                String itemPath = orePath;
                if (itemPath.startsWith("deepslate_")) itemPath = itemPath.substring("deepslate_".length());
                if (itemPath.endsWith("_ore")) itemPath = itemPath.substring(0, itemPath.length() - 4);
                if (itemPath.equals("ancient_debris")) itemPath = "netherite_scrap";
                if (itemPath.equals("redstone")) itemPath = "redstone";
                if (itemPath.equals("lapis")) itemPath = "lapis_lazuli";
                if (itemPath.equals("diamond") || itemPath.equals("emerald") || itemPath.equals("coal")
                    || itemPath.equals("copper") || itemPath.equals("iron") || itemPath.equals("gold")
                    || itemPath.equals("netherite_scrap") || itemPath.equals("redstone") || itemPath.equals("lapis_lazuli")) {
                    Identifier itemId = Identifier.fromNamespaceAndPath("minecraft", itemPath);
                    ItemStack icon = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId)
                        .map(reference -> new ItemStack(reference.value())).orElse(ItemStack.EMPTY);
                    if (!icon.isEmpty()) oreItems.add(icon);
                }
                line++;
            }
            int oreX = graphics.guiWidth() - 34 + AudioraConfig.hudX[6];
            int oreY = 6 + AudioraConfig.hudY[6];
            graphics.pose().pushMatrix();
            graphics.pose().translate(oreX, oreY);
            graphics.pose().scale(AudioraConfig.hudScaleX(6), AudioraConfig.hudScaleY(6));
            for (int i = 0; i < oreItems.size(); i++) graphics.item(oreItems.get(i), 0, i * 18);
            graphics.pose().popMatrix();
        }

        if (shouldRenderHud(10) && !player.getActiveEffects().isEmpty()) {
            int potionX = 6 + AudioraConfig.hudX[10];
            int potionY = graphics.guiHeight() / 2 + AudioraConfig.hudY[10];
            graphics.pose().pushMatrix();
            graphics.pose().translate(potionX, potionY);
            graphics.pose().scale(AudioraConfig.hudScaleX(10), AudioraConfig.hudScaleY(10));
            int visibleEffects = Math.min(8, player.getActiveEffects().size());
            int panelWidth = 142;
            int panelHeight = 8 + visibleEffects * 15;
            graphics.fill(0, 0, panelWidth, panelHeight, 0xC9101725);
            graphics.outline(0, 0, panelWidth, panelHeight, 0x8877A9D8);
            int line = 0;
            for (var effect : player.getActiveEffects()) {
                if (line >= 8) break;
                String effectName = effect.getEffect().value().getDisplayName().getString();
                int seconds = Math.max(0, effect.getDuration() / 20);
                String duration = String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
                String level = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
                int rowY = 4 + line * 15;
                graphics.fill(3, rowY - 1, 5, rowY + 10, 0xFF9B7BDE);
                graphics.text(client.font, effectName + level, 9, rowY, 0xFFECE5FF, true);
                int durationX = panelWidth - client.font.width(duration) - 6;
                graphics.text(client.font, duration, durationX, rowY, 0xFF9FCFFF, true);
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

        int cardWidth = Math.max(150, client.font.width(name) + 58);
        int cardHeight = 48;
        // Keep each preview beside its own target name inside the centered target card.
        int x = (graphics.guiWidth() - cardWidth) / 2
            + AudioraConfig.mobInfoOffsetX + AudioraConfig.hudX[hudIndex];
        int y = 12 + AudioraConfig.mobInfoOffsetY + AudioraConfig.hudY[hudIndex];

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(AudioraConfig.hudScaleX(hudIndex), AudioraConfig.hudScaleY(hudIndex));
        graphics.fill(0, 0, cardWidth, cardHeight, 0xD5101725);
        graphics.outline(0, 0, cardWidth, cardHeight, 0x9977A9D8);

        if (target instanceof AbstractClientPlayer targetPlayer) {
            // Players use their actual skin head (including the outer hat layer), kept still.
            try {
                PlayerFaceExtractor.extractRenderState(graphics, targetPlayer.getSkin(), 5, 8, 32);
            } catch (RuntimeException exception) {
                graphics.text(client.font, "?", 17, 17, 0xFFFFD36A, true);
            }
        } else {
            // Mobs rotate continuously through a full turn, including showing their back.
            try {
                float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
                EntityRenderState state = client.getEntityRenderDispatcher().extractEntity(target, partialTick);
                float previewScale = 18.0F;
                float entitySize = Math.max(target.getBbHeight(), target.getBbWidth());
                if (entitySize > 1.8F) previewScale *= 1.8F / entitySize;
                float rotation = (System.currentTimeMillis() % 8000L) / 8000.0F * ((float) Math.PI * 2.0F);
                graphics.entity(
                    state,
                    previewScale,
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new Quaternionf().rotationY(rotation),
                    null,
                    2,
                    2,
                    38,
                    45
                );
            } catch (RuntimeException exception) {
                // Keep the target card usable if a renderer does not support the entity.
                try {
                    String entityPath = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                        .getKey(target.getType()).getPath();
                    Identifier spawnEggId = Identifier.fromNamespaceAndPath("minecraft", entityPath + "_spawn_egg");
                    ItemStack spawnEgg = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .get(spawnEggId)
                        .map(reference -> new ItemStack(reference.value()))
                        .orElse(ItemStack.EMPTY);
                    if (!spawnEgg.isEmpty()) graphics.item(spawnEgg, 13, 16);
                    else graphics.text(client.font, "?", 24, 17, 0xFFFFD36A, true);
                } catch (RuntimeException ignored) {
                    graphics.text(client.font, "?", 24, 17, 0xFFFFD36A, true);
                }
            }
        }

        int textX = 42;
        int textY = 7;
        graphics.text(client.font, name, textX, textY, 0xFFFFFFFF, true);

        int healthPercent = Math.round(healthRatio * 100.0F);
        String healthLabel = healthPercent + "%";
        graphics.text(client.font, healthLabel, textX, textY + 13, 0xFFE0E0E0, true);

        int barX = textX;
        int barY = 34;
        int barWidth = cardWidth - 51;
        graphics.fill(barX, barY, barX + barWidth, barY + 6, 0xFF3A3A3A);
        int filledWidth = Math.round(barWidth * healthRatio);
        if (filledWidth > 0) {
            int barColor = healthRatio > 0.5F ? 0xFF48D16A : healthRatio > 0.25F ? 0xFFFFC247 : 0xFFE64B4B;
            graphics.fill(barX, barY, barX + filledWidth, barY + 6, barColor);
        }
        graphics.pose().popMatrix();
    }

    private static void renderCoordinates(GuiGraphicsExtractor graphics, Minecraft client,
                                           net.minecraft.world.entity.player.Player player,
                                           int left, int top) {
        String xValue = String.format(java.util.Locale.ROOT, "%.2f", player.getX());
        String yValue = String.format(java.util.Locale.ROOT, "%.2f", player.getY());
        String zValue = String.format(java.util.Locale.ROOT, "%.2f", player.getZ());
        String xLabel = "X: ";
        String yLabel = "Y: ";
        String zLabel = "Z: ";
        int gap = 7;
        int totalWidth = client.font.width(xLabel + xValue)
            + client.font.width(yLabel + yValue) + client.font.width(zLabel + zValue) + gap * 2;
        int x = (graphics.guiWidth() - totalWidth) / 2 + AudioraConfig.hudX[0];
        int y = 6 + AudioraConfig.hudY[0];
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(AudioraConfig.hudScaleX(0), AudioraConfig.hudScaleY(0));
        graphics.fill(-4, -3, totalWidth + 4, client.font.lineHeight + 3, 0x990D1722);
        int cursor = 0;
        graphics.text(client.font, xLabel, cursor, 0, 0xFF55E66B, true);
        cursor += client.font.width(xLabel);
        graphics.text(client.font, xValue, cursor, 0, 0xFFFFFFFF, true);
        cursor += client.font.width(xValue) + gap;
        graphics.text(client.font, yLabel, cursor, 0, 0xFF55BFFF, true);
        cursor += client.font.width(yLabel);
        graphics.text(client.font, yValue, cursor, 0, 0xFFFFFFFF, true);
        cursor += client.font.width(yValue) + gap;
        graphics.text(client.font, zLabel, cursor, 0, 0xFFFF5555, true);
        cursor += client.font.width(zLabel);
        graphics.text(client.font, zValue, cursor, 0, 0xFFFFFFFF, true);
        graphics.pose().popMatrix();
    }

    private static void drawKey(GuiGraphicsExtractor graphics, Minecraft client, String label,
                                int x, int y, boolean pressed) {
        int background = pressed ? 0xFF4B9B58 : 0xDD303B49;
        graphics.fill(x, y, x + 20, y + 18, background);
        int textX = x + (20 - client.font.width(label)) / 2;
        graphics.text(client.font, label, textX, y + 5, 0xFFFFFFFF, true);
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