package id.audiora.controller;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AudioraHudScreen extends Screen {
    private final Screen parent;
    private int selected;
    private int firstRow;
    public static int previewIndex = 0;
    public static boolean isPreviewing(int index) { return previewIndex == index; }
    private static final int ROW_HEIGHT = 19;

    public AudioraHudScreen(Screen parent) {
        super(Component.literal("Audiora HUD Settings"));
        this.parent = parent;
    }

    private int panelW() { return Math.min(500, width - 20); }
    private int panelX() { return (width - panelW()) / 2; }
    private int panelY() { return Math.max(8, (height - 270) / 2); }
    private int sideW() { return Math.min(154, panelW() / 3); }
    private int mainX() { return panelX() + sideW() + 12; }
    private int mainW() { return panelW() - sideW() - 22; }
    private int visibleRows() { return Math.max(3, Math.min(10, (height - 112) / ROW_HEIGHT)); }

    @Override protected void init() {
        clearWidgets();
        int x = panelX(), y = panelY(), rows = visibleRows(), count = AudioraConfig.HUD_IDS.length;
        firstRow = Math.max(0, Math.min(firstRow, Math.max(0, count - rows)));
        previewIndex = selected;

        for (int row = 0; row < rows; row++) {
            int index = firstRow + row;
            if (index >= count) break;
            final int i = index;
            addRenderableWidget(Button.builder(Component.literal((isEnabled(i) ? "● " : "○ ") + hudName(i)), b -> {
                selected = i; previewIndex = i; rebuild();
            }).bounds(x + 4, y + 38 + row * ROW_HEIGHT, sideW() - 8, 17).build());
        }

        int right = mainX(), w = mainW(), cy = y + 48;
        addRenderableWidget(Button.builder(Component.literal(isEnabled(selected) ? "HUD: ON" : "HUD: OFF"), b -> {
            setEnabled(selected, !isEnabled(selected)); AudioraConfig.save(); rebuild();
        }).bounds(right, cy, Math.max(60, w / 2 - 4), 20).build());
        addRenderableWidget(Button.builder(Component.literal("Scale −"), b -> resizeScale(-10))
            .bounds(right, cy + 29, Math.max(60, w / 2 - 4), 20).build());
        addRenderableWidget(Button.builder(Component.literal("Scale +"), b -> resizeScale(10))
            .bounds(right + w / 2 + 2, cy + 29, Math.max(60, w - w / 2 - 2), 20).build());
        addRenderableWidget(Button.builder(Component.literal("←"), b -> move(-5, 0)).bounds(right, cy + 57, 38, 20).build());
        addRenderableWidget(Button.builder(Component.literal("→"), b -> move(5, 0)).bounds(right + 42, cy + 57, 38, 20).build());
        addRenderableWidget(Button.builder(Component.literal("↑"), b -> move(0, -5)).bounds(right + 84, cy + 57, 38, 20).build());
        addRenderableWidget(Button.builder(Component.literal("↓"), b -> move(0, 5)).bounds(right + 126, cy + 57, 38, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Width −"), b -> resizeWidth(-10)).bounds(right, cy + 85, Math.max(60, w / 2 - 4), 20).build());
        addRenderableWidget(Button.builder(Component.literal("Width +"), b -> resizeWidth(10)).bounds(right + w / 2 + 2, cy + 85, Math.max(60, w - w / 2 - 2), 20).build());
        addRenderableWidget(Button.builder(Component.literal("Height −"), b -> resizeHeight(-10)).bounds(right, cy + 113, Math.max(60, w / 2 - 4), 20).build());
        addRenderableWidget(Button.builder(Component.literal("Height +"), b -> resizeHeight(10)).bounds(right + w / 2 + 2, cy + 113, Math.max(60, w - w / 2 - 2), 20).build());

        addRenderableWidget(Button.builder(Component.literal("↑"), b -> { firstRow = Math.max(0, firstRow - 1); rebuild(); })
            .bounds(x + 4, y + 40 + rows * ROW_HEIGHT, 38, 18).build());
        addRenderableWidget(Button.builder(Component.literal("↓"), b -> { firstRow = Math.min(Math.max(0, count - rows), firstRow + 1); rebuild(); })
            .bounds(x + 46, y + 40 + rows * ROW_HEIGHT, 38, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            AudioraConfig.hudX[selected] = 0; AudioraConfig.hudY[selected] = 0;
            AudioraConfig.hudScale[selected] = 100; AudioraConfig.hudWidth[selected] = 100;
            AudioraConfig.hudHeight[selected] = 100; setEnabled(selected, true);
            AudioraConfig.save(); rebuild();
        }).bounds(right, cy + 145, Math.max(60, w / 2 - 4), 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset All"), b -> {
            java.util.Arrays.fill(AudioraConfig.hudX, 0); java.util.Arrays.fill(AudioraConfig.hudY, 0);
            java.util.Arrays.fill(AudioraConfig.hudScale, 100); java.util.Arrays.fill(AudioraConfig.hudWidth, 100);
            java.util.Arrays.fill(AudioraConfig.hudHeight, 100); java.util.Arrays.fill(AudioraConfig.hudEnabled, true);
            AudioraConfig.comboEnabled = true; AudioraConfig.save(); rebuild();
        }).bounds(right + w / 2 + 2, cy + 145, Math.max(60, w - w / 2 - 2), 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(x + panelW() - 78, y + 238, 74, 20).build());
    }

    private void rebuild() { clearWidgets(); init(); }
    private boolean isEnabled(int i) { return i == 7 ? AudioraConfig.comboEnabled : AudioraConfig.hudEnabled[i]; }
    private void setEnabled(int i, boolean value) {
        AudioraConfig.hudEnabled[i] = value;
        if (i == 7) AudioraConfig.comboEnabled = value;
    }
    private void move(int dx, int dy) {
        AudioraConfig.hudX[selected] += dx; AudioraConfig.hudY[selected] += dy;
        AudioraConfig.save(); rebuild();
    }
    private void resizeScale(int n) {
        AudioraConfig.hudScale[selected] = Math.max(50, Math.min(200, AudioraConfig.hudScale[selected] + n));
        AudioraConfig.save(); rebuild();
    }
    private void resizeWidth(int n) {
        AudioraConfig.hudWidth[selected] = Math.max(50, Math.min(200, AudioraConfig.hudWidth[selected] + n));
        AudioraConfig.save(); rebuild();
    }
    private void resizeHeight(int n) {
        AudioraConfig.hudHeight[selected] = Math.max(50, Math.min(200, AudioraConfig.hudHeight[selected] + n));
        AudioraConfig.save(); rebuild();
    }
    private String hudName(int i) {
        return switch (AudioraConfig.HUD_IDS[i]) {
            case "coordinates" -> "Coordinates"; case "biome" -> "Biome";
            case "armor" -> "Armor"; case "held_item" -> "Held item";
            case "target" -> "Mob Guide"; case "pickup" -> "Pickup";
            case "ore_tracker" -> "Ore Tracker"; case "combo" -> "Combo";
            case "damage_number" -> "Damage number"; case "critical_hit" -> "Critical hit";
            case "potion_effects" -> "Potion effects"; case "player_target" -> "Player Guide";
            case "compass" -> "Compass"; case "keystrokes" -> "WASD + CPS";
            default -> AudioraConfig.HUD_IDS[i];
        };
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        int x = panelX(), y = panelY(), rows = visibleRows(), right = mainX(), w = mainW();
        // Lightly transparent editor surfaces keep the game visible behind the settings.
        g.fill(x - 3, y - 5, x + panelW() + 3, y + 266, 0x55202935);
        g.fill(x - 3, y - 5, x + panelW() + 3, y + 20, 0x88304761);
        g.text(font, "AUDIORA / HUD SETTINGS", x + 5, y + 3, 0xFFFFFFFF, true);
        g.text(font, "HUD LIST", x + 7, y + 27, 0xFFB8D8FF, true);
        g.text(font, "HUD SIZE & POSITION", right, y + 27, 0xFFB8D8FF, true);
        g.fill(right - 3, y + 43, right + w + 2, y + 220, 0x33202935);
        g.text(font, "Selected: " + hudName(selected), right + 3, y + 49, 0xFFFFFFFF, true);
        g.text(font, "Scale: " + AudioraConfig.hudScale[selected] + "%", right + 3, y + 91, 0xFFE8F1FA, false);
        g.text(font, "X: " + AudioraConfig.hudX[selected] + "   Y: " + AudioraConfig.hudY[selected], right + 3, y + 119, 0xFFE8F1FA, false);
        g.text(font, "Width: " + AudioraConfig.hudWidth[selected] + "%", right + 3, y + 147, 0xFFE8F1FA, false);
        g.text(font, "Height: " + AudioraConfig.hudHeight[selected] + "%", right + 3, y + 175, 0xFFE8F1FA, false);
    }

    @Override public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int max = Math.max(0, AudioraConfig.HUD_IDS.length - visibleRows());
        firstRow = Math.max(0, Math.min(max, firstRow + (dy < 0 ? 1 : -1)));
        rebuild(); return true;
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) { }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { AudioraConfig.save(); minecraft.gui.setScreen(parent); }
}
