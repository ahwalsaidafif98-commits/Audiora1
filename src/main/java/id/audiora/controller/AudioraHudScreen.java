package id.audiora.controller;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Compact, right-docked HUD editor. Every HUD element can be enabled,
 * resized and selected independently without leaving the game view visible.
 */
public final class AudioraHudScreen extends Screen {
    private final Screen parent;
    private int selected;
    private int page;

    private static final int PANEL_WIDTH = 244;
    private static final int ROW_HEIGHT = 27;
    private static final int VISIBLE_ROWS = 13;

    public AudioraHudScreen(Screen parent) {
        super(Component.literal("Audiora HUD Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        int panelX = Math.max(6, width - PANEL_WIDTH - 8);
        int top = 48;

        // HUD list: one compact row per element, with enable and size controls.
        for (int i = 0; i < VISIBLE_ROWS && i < AudioraConfig.HUD_IDS.length; i++) {
            final int index = i;
            int y = top + 26 + i * ROW_HEIGHT;
            boolean enabled = isEnabled(index);

            addRenderableWidget(Button.builder(
                Component.literal(enabled ? "ON" : "OFF"),
                button -> {
                    setEnabled(index, !isEnabled(index));
                    AudioraConfig.save();
                    rebuild();
                }).bounds(panelX + 164, y, 34, 20).build());

            addRenderableWidget(Button.builder(Component.literal("−"), button -> {
                AudioraConfig.hudScale[index] = Math.max(50, AudioraConfig.hudScale[index] - 10);
                AudioraConfig.save();
                rebuild();
            }).bounds(panelX + 201, y, 19, 20).build());

            addRenderableWidget(Button.builder(Component.literal("+"), button -> {
                AudioraConfig.hudScale[index] = Math.min(200, AudioraConfig.hudScale[index] + 10);
                AudioraConfig.save();
                rebuild();
            }).bounds(panelX + 222, y, 19, 20).build());
        }

        // Position controls apply to the currently selected HUD item.
        int controlsY = top + 26 + VISIBLE_ROWS * ROW_HEIGHT + 4;
        addRenderableWidget(Button.builder(Component.literal("←"), b -> move(-5, 0))
            .bounds(panelX + 4, controlsY, 42, 20).build());
        addRenderableWidget(Button.builder(Component.literal("→"), b -> move(5, 0))
            .bounds(panelX + 48, controlsY, 42, 20).build());
        addRenderableWidget(Button.builder(Component.literal("↑"), b -> move(0, -5))
            .bounds(panelX + 92, controlsY, 42, 20).build());
        addRenderableWidget(Button.builder(Component.literal("↓"), b -> move(0, 5))
            .bounds(panelX + 136, controlsY, 42, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Reset selected"), b -> {
            AudioraConfig.hudX[selected] = 0;
            AudioraConfig.hudY[selected] = 0;
            AudioraConfig.hudScale[selected] = 100;
            setEnabled(selected, true);
            AudioraConfig.save();
            rebuild();
        }).bounds(panelX + 4, controlsY + 26, 112, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Reset all"), b -> {
            java.util.Arrays.fill(AudioraConfig.hudX, 0);
            java.util.Arrays.fill(AudioraConfig.hudY, 0);
            java.util.Arrays.fill(AudioraConfig.hudScale, 100);
            java.util.Arrays.fill(AudioraConfig.hudEnabled, true);
            AudioraConfig.comboEnabled = true;
            AudioraConfig.save();
            rebuild();
        }).bounds(panelX + 120, controlsY + 26, 120, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(panelX + 4, controlsY + 52, 236, 20).build());
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private boolean isEnabled(int index) {
        return index == 7 ? AudioraConfig.comboEnabled : AudioraConfig.hudEnabled[index];
    }

    private void setEnabled(int index, boolean enabled) {
        AudioraConfig.hudEnabled[index] = enabled;
        if (index == 7) AudioraConfig.comboEnabled = enabled;
    }

    private void move(int dx, int dy) {
        AudioraConfig.hudX[selected] += dx;
        AudioraConfig.hudY[selected] += dy;
        AudioraConfig.save();
    }

    private String hudName(int index) {
        return switch (AudioraConfig.HUD_IDS[index]) {
            case "coordinates" -> "Coordinates";
            case "biome" -> "Biome";
            case "armor" -> "Armor";
            case "held_item" -> "Held item";
            case "target" -> "Mob name / health";
            case "pickup" -> "Pickup";
            case "ore_tracker" -> "Ore tracker";
            case "combo" -> "Crit combo";
            case "damage_number" -> "Damage number";
            case "critical_hit" -> "Critical hit";
            case "potion_effects" -> "Potion effects";
            case "player_target" -> "Player name / health";
            case "compass" -> "Compass";
            default -> AudioraConfig.HUD_IDS[index].replace('_', ' ');
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int x = Math.max(6, width - PANEL_WIDTH - 8);
        int top = 48;
        int bottom = top + 26 + VISIBLE_ROWS * ROW_HEIGHT + 86;

        // Dark translucent card with a subtle blue header and selected-row marker.
        g.fill(x - 5, top - 28, x + PANEL_WIDTH, bottom, 0xAA101725);
        g.fill(x - 5, top - 28, x + PANEL_WIDTH, top - 5, 0xFF173B68);
        g.text(font, "AUDIORA  /  HUD SETTINGS", x + 5, top - 21, 0xFFFFFFFF, true);
        g.text(font, "HUD ELEMENTS", x + 5, top + 7, 0xFF80C8FF, true);

        for (int i = 0; i < VISIBLE_ROWS && i < AudioraConfig.HUD_IDS.length; i++) {
            int y = top + 26 + i * ROW_HEIGHT;
            if (i == selected) {
                g.fill(x, y - 2, x + PANEL_WIDTH - 4, y + 21, 0x88305F91);
            } else if ((i & 1) == 0) {
                g.fill(x, y - 2, x + PANEL_WIDTH - 4, y + 21, 0x442B394D);
            }
            g.text(font, hudName(i), x + 5, y + 5,
                isEnabled(i) ? 0xFFF1F5FA : 0xFF8993A1, i == selected);
            g.text(font, AudioraConfig.hudScale[i] + "%", x + 202, y + 5, 0xFFB8D8F4, false);
        }

        int controlsY = top + 26 + VISIBLE_ROWS * ROW_HEIGHT + 4;
        g.fill(x, controlsY - 4, x + PANEL_WIDTH - 4, controlsY + 76, 0x55304B68);
        g.text(font, "POSITION  •  " + hudName(selected), x + 5, controlsY - 1, 0xFF80C8FF, true);
        g.text(font, "X: " + AudioraConfig.hudX[selected] + "   Y: " + AudioraConfig.hudY[selected]
            + "   SIZE: " + AudioraConfig.hudScale[selected] + "%", x + 5, controlsY + 52, 0xFFD5E4F2, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // Keep the world visible behind the HUD editor.
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int x = Math.max(6, width - PANEL_WIDTH - 8);
        int top = 48;
        for (int i = 0; i < VISIBLE_ROWS && i < AudioraConfig.HUD_IDS.length; i++) {
            int y = top + 26 + i * ROW_HEIGHT;
            if (mouseX >= x && mouseX < x + 160 && mouseY >= y - 2 && mouseY < y + 21) {
                selected = i;
                rebuild();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        AudioraConfig.save();
        minecraft.gui.setScreen(parent);
    }
}
