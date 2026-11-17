package id.audiora.controller;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class AudioraHudScreen extends Screen {
    private final Screen parent;
    private int selected;
    public static int previewIndex = 0;
    public static boolean isPreviewing(int index) { return previewIndex == index; }
    private int firstRow;
    private static final int PANEL_WIDTH = 260;
    private static final int ROW_HEIGHT = 23;

    public AudioraHudScreen(Screen parent) {
        super(Component.literal("Audiora HUD Settings"));
        this.parent = parent;
    }

    private int visibleRows() { return height < 250 ? 1 : height < 310 ? 2 : height < 400 ? 3 : 4; }
    private int panelX() { return 6; }
    private int top() { return Math.max(8, (height - (visibleRows() * ROW_HEIGHT + 196)) / 2); }
    private int controlsY() { return top() + 22 + visibleRows() * ROW_HEIGHT + 4; }

    @Override
    protected void init() {
        clearWidgets();
        int x = panelX(), top = top(), count = AudioraConfig.HUD_IDS.length, rows = visibleRows();
        previewIndex = selected;
        firstRow = Math.max(0, Math.min(firstRow, Math.max(0, count - rows)));

        for (int row = 0; row < rows; row++) {
            final int index = firstRow + row;
            if (index >= count) break;
            int y = top + 22 + row * ROW_HEIGHT;
            addRenderableWidget(Button.builder(Component.literal(isEnabled(index) ? "ON" : "OFF"), b -> {
                setEnabled(index, !isEnabled(index)); AudioraConfig.save(); rebuild();
            }).bounds(x + 158, y, 38, 19).build());
            addRenderableWidget(Button.builder(Component.literal("−"), b -> {
                AudioraConfig.hudScale[index] = Math.max(50, AudioraConfig.hudScale[index] - 10);
                AudioraConfig.save(); rebuild();
            }).bounds(x + 199, y, 25, 19).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                AudioraConfig.hudScale[index] = Math.min(200, AudioraConfig.hudScale[index] + 10);
                AudioraConfig.save(); rebuild();
            }).bounds(x + 227, y, 25, 19).build());
        }

        int cy = controlsY();
        addRenderableWidget(Button.builder(Component.literal("↑ List"), b -> { firstRow = Math.max(0, firstRow - 1); rebuild(); })
            .bounds(x + 4, cy, 72, 19).build());
        addRenderableWidget(Button.builder(Component.literal("↓ List"), b -> { firstRow = Math.min(Math.max(0, count - rows), firstRow + 1); rebuild(); })
            .bounds(x + 80, cy, 72, 19).build());
        addRenderableWidget(Button.builder(Component.literal("←"), b -> move(-5, 0)).bounds(x + 4, cy + 23, 54, 19).build());
        addRenderableWidget(Button.builder(Component.literal("→"), b -> move(5, 0)).bounds(x + 61, cy + 23, 54, 19).build());
        addRenderableWidget(Button.builder(Component.literal("↑"), b -> move(0, -5)).bounds(x + 118, cy + 23, 54, 19).build());
        addRenderableWidget(Button.builder(Component.literal("↓"), b -> move(0, 5)).bounds(x + 175, cy + 23, 54, 19).build());
        addRenderableWidget(Button.builder(Component.literal("Scale −"), b -> resizeScale(-10)).bounds(x + 4, cy + 46, 110, 19).build());
        addRenderableWidget(Button.builder(Component.literal("Scale +"), b -> resizeScale(10)).bounds(x + 118, cy + 46, 110, 19).build());
        addRenderableWidget(Button.builder(Component.literal("Width −"), b -> resizeWidth(-10)).bounds(x + 4, cy + 69, 110, 19).build());
        addRenderableWidget(Button.builder(Component.literal("Width +"), b -> resizeWidth(10)).bounds(x + 118, cy + 69, 110, 19).build());
        addRenderableWidget(Button.builder(Component.literal("Height −"), b -> resizeHeight(-10)).bounds(x + 4, cy + 92, 110, 19).build());
        addRenderableWidget(Button.builder(Component.literal("Height +"), b -> resizeHeight(10)).bounds(x + 118, cy + 92, 110, 19).build());
        addRenderableWidget(Button.builder(Component.literal("Reset selected"), b -> {
            AudioraConfig.hudX[selected] = 0; AudioraConfig.hudY[selected] = 0;
            AudioraConfig.hudScale[selected] = 100;
            AudioraConfig.hudWidth[selected] = 100; AudioraConfig.hudHeight[selected] = 100;
            setEnabled(selected, true); AudioraConfig.save(); rebuild();
        }).bounds(x + 4, cy + 115, 110, 19).build());
        addRenderableWidget(Button.builder(Component.literal("Reset all"), b -> {
            java.util.Arrays.fill(AudioraConfig.hudX, 0);
            java.util.Arrays.fill(AudioraConfig.hudY, 0);
            java.util.Arrays.fill(AudioraConfig.hudScale, 100);
            java.util.Arrays.fill(AudioraConfig.hudWidth, 100);
            java.util.Arrays.fill(AudioraConfig.hudHeight, 100);
            java.util.Arrays.fill(AudioraConfig.hudEnabled, true);
            AudioraConfig.comboEnabled = true; AudioraConfig.save(); rebuild();
        }).bounds(x + 118, cy + 115, 110, 19).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(x + 4, cy + 138, 239, 19).build());
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
    private void resizeScale(int amount) {
        AudioraConfig.hudScale[selected] = Math.max(50, Math.min(200, AudioraConfig.hudScale[selected] + amount));
        AudioraConfig.save(); rebuild();
    }
    private void resizeWidth(int amount) {
        AudioraConfig.hudWidth[selected] = Math.max(50, Math.min(200, AudioraConfig.hudWidth[selected] + amount));
        AudioraConfig.save(); rebuild();
    }
    private void resizeHeight(int amount) {
        AudioraConfig.hudHeight[selected] = Math.max(50, Math.min(200, AudioraConfig.hudHeight[selected] + amount));
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
            case "compass" -> "Compass"; case "keystrokes" -> "WASD + CPS"; default -> AudioraConfig.HUD_IDS[i];
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        int x = panelX(), top = top(), cy = controlsY(), rows = visibleRows();
        g.fill(x - 4, top - 25, x + PANEL_WIDTH, cy + 163, 0xDD101725);
        g.fill(x - 4, top - 25, x + PANEL_WIDTH, top - 5, 0xFF173B68);
        g.text(font, "AUDIORA / HUD SETTINGS", x + 5, top - 18, 0xFFFFFFFF, true);
        g.text(font, "Select a HUD to preview it live on screen.", x + 4, top + 1, 0xFF9FCFFF, false);
        for (int row = 0; row < rows; row++) {
            int i = firstRow + row;
            if (i >= AudioraConfig.HUD_IDS.length) break;
            int y = top + 22 + row * ROW_HEIGHT;
            g.fill(x, y - 1, x + PANEL_WIDTH - 8, y + 19,
                i == selected ? 0xAA305F91 : ((row & 1) == 0 ? 0x55303D50 : 0x332B394D));
            g.text(font, hudName(i), x + 5, y + 5, isEnabled(i) ? 0xFFF1F5FA : 0xFF8993A1, i == selected);
            g.text(font, AudioraConfig.hudScale[i] + "%", x + 119, y + 5, 0xFFB8D8F4, false);
        }
        g.text(font, "Selected: " + hudName(selected), x + 5, cy - 1, 0xFF80C8FF, true);
        g.text(font, "X " + AudioraConfig.hudX[selected] + " / Y " + AudioraConfig.hudY[selected]
            + "  Scale " + AudioraConfig.hudScale[selected] + "%", x + 5, cy + 72, 0xFFD5E4F2, false);
        g.text(font, "Width " + AudioraConfig.hudWidth[selected] + "%  Height "
            + AudioraConfig.hudHeight[selected] + "%", x + 5, cy + 86, 0xFFD5E4F2, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int max = Math.max(0, AudioraConfig.HUD_IDS.length - visibleRows());
        firstRow = Math.max(0, Math.min(max, firstRow + (deltaY < 0 ? 1 : -1)));
        rebuild();
        return true;
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) { }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int x = panelX(), top = top(), rows = visibleRows();
        for (int row = 0; row < rows; row++) {
            int i = firstRow + row, y = top + 22 + row * ROW_HEIGHT;
            if (i < AudioraConfig.HUD_IDS.length && mx >= x && mx < x + 155 && my >= y - 1 && my < y + 20) {
                selected = i; previewIndex = i; rebuild(); return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { AudioraConfig.save(); minecraft.gui.setScreen(parent); }
}
