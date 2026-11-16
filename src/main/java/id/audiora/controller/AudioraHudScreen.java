package id.audiora.controller;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class AudioraHudScreen extends Screen {
    private final Screen parent;
    private int selected;
    private int firstRow;
    private static final int PANEL_WIDTH = 260;
    private static final int ROW_HEIGHT = 27;
    private static final int VISIBLE_ROWS = 6;

    public AudioraHudScreen(Screen parent) {
        super(Component.literal("Audiora HUD Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = Math.max(6, width - PANEL_WIDTH - 8);
        int top = 54;
        int count = AudioraConfig.HUD_IDS.length;
        firstRow = Math.max(0, Math.min(firstRow, Math.max(0, count - VISIBLE_ROWS)));

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int index = firstRow + row;
            if (index >= count) break;
            int y = top + 22 + row * ROW_HEIGHT;
            addRenderableWidget(Button.builder(Component.literal(isEnabled(index) ? "ON" : "OFF"), b -> {
                setEnabled(index, !isEnabled(index)); AudioraConfig.save(); rebuild();
            }).bounds(x + 160, y, 38, 20).build());
            addRenderableWidget(Button.builder(Component.literal("−"), b -> {
                AudioraConfig.hudScale[index] = Math.max(50, AudioraConfig.hudScale[index] - 10);
                AudioraConfig.save(); rebuild();
            }).bounds(x + 201, y, 25, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                AudioraConfig.hudScale[index] = Math.min(200, AudioraConfig.hudScale[index] + 10);
                AudioraConfig.save(); rebuild();
            }).bounds(x + 229, y, 25, 20).build());
        }

        int controlsY = top + 22 + VISIBLE_ROWS * ROW_HEIGHT + 8;
        addRenderableWidget(Button.builder(Component.literal("↑ List"), b -> {
            firstRow = Math.max(0, firstRow - 1); rebuild();
        }).bounds(x + 4, controlsY, 72, 20).build());
        addRenderableWidget(Button.builder(Component.literal("↓ List"), b -> {
            firstRow = Math.min(count - VISIBLE_ROWS, firstRow + 1); rebuild();
        }).bounds(x + 80, controlsY, 72, 20).build());
        addRenderableWidget(Button.builder(Component.literal("←"), b -> move(-5, 0))
            .bounds(x + 4, controlsY + 28, 54, 20).build());
        addRenderableWidget(Button.builder(Component.literal("→"), b -> move(5, 0))
            .bounds(x + 61, controlsY + 28, 54, 20).build());
        addRenderableWidget(Button.builder(Component.literal("↑"), b -> move(0, -5))
            .bounds(x + 118, controlsY + 28, 54, 20).build());
        addRenderableWidget(Button.builder(Component.literal("↓"), b -> move(0, 5))
            .bounds(x + 175, controlsY + 28, 54, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Size −"), b -> resize(-10))
            .bounds(x + 4, controlsY + 56, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Size +"), b -> resize(10))
            .bounds(x + 118, controlsY + 56, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset selected"), b -> {
            AudioraConfig.hudX[selected] = 0; AudioraConfig.hudY[selected] = 0;
            AudioraConfig.hudScale[selected] = 100; setEnabled(selected, true);
            AudioraConfig.save(); rebuild();
        }).bounds(x + 4, controlsY + 84, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset all"), b -> {
            java.util.Arrays.fill(AudioraConfig.hudX, 0);
            java.util.Arrays.fill(AudioraConfig.hudY, 0);
            java.util.Arrays.fill(AudioraConfig.hudScale, 100);
            java.util.Arrays.fill(AudioraConfig.hudEnabled, true);
            AudioraConfig.comboEnabled = true; AudioraConfig.save(); rebuild();
        }).bounds(x + 118, controlsY + 84, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(x + 4, controlsY + 112, 236, 20).build());
    }

    private void rebuild() { clearWidgets(); init(); }
    private boolean isEnabled(int i) { return i == 7 ? AudioraConfig.comboEnabled : AudioraConfig.hudEnabled[i]; }
    private void setEnabled(int i, boolean value) {
        AudioraConfig.hudEnabled[i] = value;
        if (i == 7) AudioraConfig.comboEnabled = value;
    }
    private void move(int dx, int dy) {
        AudioraConfig.hudX[selected] += dx; AudioraConfig.hudY[selected] += dy;
        AudioraConfig.save();
    }
    private void resize(int amount) {
        AudioraConfig.hudScale[selected] = Math.max(50, Math.min(200, AudioraConfig.hudScale[selected] + amount));
        AudioraConfig.save(); rebuild();
    }
    private String hudName(int i) {
        return switch (AudioraConfig.HUD_IDS[i]) {
            case "coordinates" -> "Coordinates"; case "biome" -> "Biome";
            case "armor" -> "Armor"; case "held_item" -> "Held item";
            case "target" -> "Mob Guide"; case "pickup" -> "Pickup";
            case "ore_tracker" -> "Ore Tracker"; case "combo" -> "Crit combo";
            case "damage_number" -> "Damage number"; case "critical_hit" -> "Critical hit";
            case "potion_effects" -> "Potion effects"; case "player_target" -> "Player Guide";
            case "compass" -> "Compass"; default -> AudioraConfig.HUD_IDS[i];
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        int x = Math.max(6, width - PANEL_WIDTH - 8), top = 54;
        int controlsY = top + 22 + VISIBLE_ROWS * ROW_HEIGHT + 8;
        g.fill(x - 5, top - 30, x + PANEL_WIDTH, controlsY + 142, 0xDD101725);
        g.fill(x - 5, top - 30, x + PANEL_WIDTH, top - 6, 0xFF173B68);
        g.text(font, "AUDIORA / HUD SETTINGS", x + 5, top - 22, 0xFFFFFFFF, true);
        g.text(font, "ON HUDs appear only when their condition is met", x + 4, top - 1, 0xFF9FCFFF, false);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int i = firstRow + row;
            if (i >= AudioraConfig.HUD_IDS.length) break;
            int y = top + 22 + row * ROW_HEIGHT;
            g.fill(x, y - 2, x + PANEL_WIDTH - 8, y + 21,
                i == selected ? 0xAA305F91 : ((row & 1) == 0 ? 0x55303D50 : 0x332B394D));
            g.text(font, hudName(i), x + 5, y + 5, isEnabled(i) ? 0xFFF1F5FA : 0xFF8993A1, i == selected);
            g.text(font, AudioraConfig.hudScale[i] + "%", x + 132, y + 5, 0xFFB8D8F4, false);
        }
        g.text(font, "Selected: " + hudName(selected), x + 5, controlsY - 1, 0xFF80C8FF, true);
        g.text(font, "Position X " + AudioraConfig.hudX[selected] + " / Y " + AudioraConfig.hudY[selected]
            + "    Scale " + AudioraConfig.hudScale[selected] + "%", x + 5, controlsY + 88, 0xFFD5E4F2, false);
        g.text(font, "Use mouse wheel or List buttons to browse HUDs.", x + 5, controlsY + 108, 0xFF9FCFFF, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int max = Math.max(0, AudioraConfig.HUD_IDS.length - VISIBLE_ROWS);
        firstRow = Math.max(0, Math.min(max, firstRow + (deltaY < 0 ? 1 : -1)));
        rebuild();
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) { }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int x = Math.max(6, width - PANEL_WIDTH - 8), top = 54;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int i = firstRow + row, y = top + 22 + row * ROW_HEIGHT;
            if (i < AudioraConfig.HUD_IDS.length && mx >= x && mx < x + 155 && my >= y - 2 && my < y + 21) {
                selected = i; rebuild(); return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { AudioraConfig.save(); minecraft.gui.setScreen(parent); }
}
