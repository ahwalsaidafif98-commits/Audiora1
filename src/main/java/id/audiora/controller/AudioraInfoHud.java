package id.audiora.controller;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

public final class AudioraInfoHud {
    private static final Map<String, Integer> previousCounts = new HashMap<>();
    private static String pickupName = "";
    private static int pickupAmount;
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
                        pickupAmount = gained;
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

        if (client.hitResult instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (entity instanceof LivingEntity living && living.isAlive()) {
                String name = living.getName().getString();
                int width = client.font.width(name);
                int x = (graphics.guiWidth() - Math.max(110, width + 24)) / 2;
                int y = 12;
                graphics.fill(x, y, x + Math.max(110, width + 24), y + 28, 0xB9000000);
                graphics.outline(x, y, Math.max(110, width + 24), 28, 0xFF65B8FF);
                graphics.text(client.font, name, graphics.guiWidth() / 2 - width / 2, y + 10, 0xFFFFFFFF, true);
            }
        }

        if (pickupAmount > 0 && System.currentTimeMillis() < pickupUntil) {
            String label = pickupName + " " + pickupAmount + "x";
            int width = client.font.width(label);
            int x = (graphics.guiWidth() - width - 30) / 2;
            int y = graphics.guiHeight() - 64;
            graphics.fill(x, y, x + width + 30, y + 24, 0xC9000000);
            graphics.outline(x, y, width + 30, 24, 0xFF65B8FF);
            graphics.text(client.font, label, x + 15, y + 8, 0xFFFFFFFF, true);
        } else {
            pickupAmount = 0;
        }
    }
}
