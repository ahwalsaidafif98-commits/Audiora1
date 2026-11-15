package id.audiora.controller;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

public final class AudioraScreen extends Screen {
    private static final int[] VOLUME_STEPS = {
        0, 10, 15, 20, 25, 30, 35, 40, 45, 50,
        55, 60, 65, 70, 75, 80, 85, 90, 95, 100
    };

    private final Screen parent;
    private static boolean editingMobHud = true;
    private static int selectedHud = 0;

    public AudioraScreen() {
        this(null);
    }

    public AudioraScreen(Screen parent) {
        super(Component.literal("Audiora Sound Controller"));
        this.parent = parent;
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    protected void init() {
        int center = this.width / 2;

        this.addRenderableWidget(Button.builder(masterLabel(), button -> {
            AudioraConfig.masterEnabled = !AudioraConfig.masterEnabled;
            button.setMessage(masterLabel());
            AudioraConfig.save();
        }).bounds(center - 150, 34, 92, 18).build());

        this.addRenderableWidget(new VolumeSlider(
            center - 53, 34, 203, 18, AudioraConfig.masterVolume, "Master"
        ));

        // Two compact columns; all seven categories fit without a scroll area.
        for (int i = 0; i < AudioraConfig.CATEGORIES.length; i++) {
            final int index = i;
            int column = i / 4;
            int row = i % 4;
            int x = center - 150 + column * 155;
            int cy = 65 + row * 23;
            this.addRenderableWidget(Button.builder(toggleLabel(index), button -> {
                AudioraConfig.enabled[index] = !AudioraConfig.enabled[index];
                button.setMessage(toggleLabel(index));
                AudioraConfig.save();
            }).bounds(x, cy, 45, 18).build());
            this.addRenderableWidget(new VolumeSlider(
                x + 49, cy, 76, 18, AudioraConfig.volume[index], AudioraConfig.CATEGORIES[index]
            ));
            this.addRenderableWidget(Button.builder(Component.literal("▶"), button -> previewCategory(index))
                .bounds(x + 128, cy, 22, 18).build());
        }

        this.addRenderableWidget(Button.builder(hudPositionLabel(), button -> {
            editingMobHud = !editingMobHud;
            button.setMessage(hudPositionLabel());
        }).bounds(center - 150, 171, 78, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("←"), button -> {
            if (editingMobHud) AudioraConfig.mobInfoOffsetX -= 5;
            else AudioraConfig.itemInfoOffsetX -= 5;
            AudioraConfig.save();
        }).bounds(center - 68, 171, 34, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("→"), button -> {
            if (editingMobHud) AudioraConfig.mobInfoOffsetX += 5;
            else AudioraConfig.itemInfoOffsetX += 5;
            AudioraConfig.save();
        }).bounds(center - 31, 171, 34, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("↑"), button -> {
            if (editingMobHud) AudioraConfig.mobInfoOffsetY -= 5;
            else AudioraConfig.itemInfoOffsetY -= 5;
            AudioraConfig.save();
        }).bounds(center + 6, 171, 34, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("↓"), button -> {
            if (editingMobHud) AudioraConfig.mobInfoOffsetY += 5;
            else AudioraConfig.itemInfoOffsetY += 5;
            AudioraConfig.save();
        }).bounds(center + 43, 171, 34, 18).build());

        this.addRenderableWidget(Button.builder(comboLabel(), button -> {
            AudioraConfig.comboEnabled = !AudioraConfig.comboEnabled;
            button.setMessage(comboLabel());
            AudioraConfig.save();
        }).bounds(center - 150, 210, 86, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("← Kiri"), button -> {
            AudioraConfig.comboOffsetX -= 5;
            AudioraConfig.save();
        }).bounds(center - 58, 210, 48, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Kanan →"), button -> {
            AudioraConfig.comboOffsetX += 5;
            AudioraConfig.save();
        }).bounds(center - 7, 210, 48, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("↑ Atas"), button -> {
            AudioraConfig.comboOffsetY -= 5;
            AudioraConfig.save();
        }).bounds(center + 44, 210, 48, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("↓ Bawah"), button -> {
            AudioraConfig.comboOffsetY += 5;
            AudioraConfig.save();
        }).bounds(center + 95, 210, 48, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Enable All"), button -> {
            AudioraConfig.masterEnabled = true;
            for (int i = 0; i < AudioraConfig.enabled.length; i++) AudioraConfig.enabled[i] = true;
            AudioraConfig.save();
            rebuild();
        }).bounds(center - 150, 240, 92, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Disable All"), button -> {
            AudioraConfig.masterEnabled = false;
            for (int i = 0; i < AudioraConfig.enabled.length; i++) AudioraConfig.enabled[i] = false;
            AudioraConfig.save();
            rebuild();
        }).bounds(center - 53, 240, 92, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
            AudioraConfig.reset();
            rebuild();
        }).bounds(center + 44, 240, 55, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Apply / Save"), button -> {
            AudioraConfig.save();
            this.onClose();
        }).bounds(center + 104, 240, 96, 18).build());

        this.addRenderableWidget(Button.builder(selectedHudLabel(), button -> {
            selectedHud = (selectedHud + 1) % AudioraConfig.HUD_IDS.length;
            button.setMessage(selectedHudLabel());
        }).bounds(center - 150, 270, 112, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Size -"), button -> {
            AudioraConfig.hudScale[selectedHud] = Math.max(50, AudioraConfig.hudScale[selectedHud] - 10);
            AudioraConfig.save();
        }).bounds(center - 34, 270, 48, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Size +"), button -> {
            AudioraConfig.hudScale[selectedHud] = Math.min(200, AudioraConfig.hudScale[selectedHud] + 10);
            AudioraConfig.save();
        }).bounds(center + 17, 270, 48, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Reset HUD"), button -> {
            AudioraConfig.hudX[selectedHud] = 0;
            AudioraConfig.hudY[selectedHud] = 0;
            AudioraConfig.hudScale[selectedHud] = 100;
            AudioraConfig.save();
        }).bounds(center + 68, 270, 78, 18).build());
        this.addRenderableWidget(Button.builder(hudToggleLabel(), button -> {
            AudioraConfig.hudEnabled[selectedHud] = !AudioraConfig.hudEnabled[selectedHud];
            button.setMessage(hudToggleLabel());
            AudioraConfig.save();
        }).bounds(center - 150, 294, 92, 18).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int center = this.width / 2;
        graphics.text(this.font, "AUDIORA - Sound settings", center - 82, 12, 0xFFFFFFFF, true);
        drawAudioraIcon(graphics, center - 112, 7);
        graphics.text(this.font, "Master volume", center - 150, 25, 0xFFB8D8FF, false);

        for (int i = 0; i < AudioraConfig.CATEGORIES.length; i++) {
            int column = i / 4;
            int row = i % 4;
            int x = center - 150 + column * 155;
            int cy = 65 + row * 23;
            graphics.text(this.font, AudioraConfig.CATEGORIES[i].toUpperCase(), x, cy - 9, 0xFFE8E8E8, false);
        }

        graphics.text(this.font, "HUD Mob / Item position", center - 150, 158, 0xFFB8D8FF, false);
        graphics.text(this.font, "Crit Combo position", center - 150, 197, 0xFFB8D8FF, false);
        graphics.text(this.font, "Drag anywhere to move selected HUD • " + selectedHudLabel().getString(),
            8, this.height - 24, 0xFFFFFFFF, true);
        drawHudPreview(graphics);
    }

    private static void drawAudioraIcon(GuiGraphicsExtractor graphics, int x, int y) {
        for (int step = 0; step < 360; step += 4) {
            double a = Math.toRadians(step);
            int px = x + 12 + (int)Math.round(Math.cos(a) * 10);
            int py = y + 12 + (int)Math.round(Math.sin(a) * 10);
            graphics.fill(px, py, px + 2, py + 2, 0xFFB982F2);
        }
        for (int step = 48; step < 312; step += 5) {
            double a = Math.toRadians(step);
            int px = x + 12 + (int)Math.round(Math.cos(a) * 7);
            int py = y + 12 + (int)Math.round(Math.sin(a) * 7);
            graphics.fill(px, py, px + 2, py + 2, 0xFFEBD9FF);
        }
        graphics.fill(x + 11, y + 5, x + 14, y + 17, 0xFFEBD9FF);
        graphics.fill(x + 8, y + 8, x + 17, y + 11, 0xFFEBD9FF);
        graphics.fill(x + 7, y + 9, x + 10, y + 12, 0xFFEBD9FF);
        graphics.fill(x + 15, y + 9, x + 18, y + 12, 0xFFEBD9FF);
        graphics.fill(x + 12, y + 14, x + 16, y + 20, 0xFFEBD9FF);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Keep the live world visible while editing HUD layout.
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        AudioraConfig.hudX[selectedHud] += (int)Math.round(deltaX);
        AudioraConfig.hudY[selectedHud] += (int)Math.round(deltaY);
        AudioraConfig.save();
        return true;
    }

    private void drawHudPreview(GuiGraphicsExtractor graphics) {
        int x = this.width / 2 + AudioraConfig.hudX[selectedHud];
        int y = this.height / 2 + AudioraConfig.hudY[selectedHud];
        float scale = AudioraConfig.hudScale[selectedHud] / 100.0F;
        int w = Math.max(40, Math.round(110 * scale));
        int h = Math.max(14, Math.round(24 * scale));
        graphics.outline(x, y, w, h, 0xFFFFD45A);
        graphics.text(this.font, AudioraConfig.HUD_IDS[selectedHud] + " " + AudioraConfig.hudScale[selectedHud] + "%",
            x + 3, y + 4, 0xFFFFD45A, true);
    }

    @Override
    public void onClose() {
        AudioraConfig.save();
        this.minecraft.gui.setScreen(parent);
    }

    private static void previewCategory(int category) {
        String name = AudioraConfig.CATEGORIES[category];
        String[] terms = switch (name) {
            case "inventory" -> new String[] {"chest", "barrel", "shulker"};
            case "player" -> new String[] {"player", "step", "hurt"};
            case "items" -> new String[] {"item", "pickup", "equip"};
            case "birds" -> new String[] {"parrot", "chicken", "ambient"};
            case "insects" -> new String[] {"bee", "cave", "ambient"};
            case "nature" -> new String[] {"water", "ambient", "rain"};
            default -> new String[] {"ui.button", "ui"};
        };
        for (Identifier id : BuiltInRegistries.SOUND_EVENT.keySet()) {
            String path = id.getPath();
            boolean match = false;
            for (String term : terms) if (path.contains(term)) { match = true; break; }
            if (!match) continue;
            var eventHolder = BuiltInRegistries.SOUND_EVENT.get(id);
            if (eventHolder.isPresent()) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(eventHolder.get(), 0.8F));
                return;
            }
        }
        // No matching vanilla sample is registered for this category.
    }

    private static Component hudPositionLabel() {
        return Component.literal("Posisi: " + (editingMobHud ? "Mob" : "Item"));
    }

    private static Component selectedHudLabel() {
        return Component.literal("HUD: " + AudioraConfig.HUD_IDS[selectedHud]);
    }

    private static Component hudToggleLabel() {
        return Component.literal("HUD: " + (AudioraConfig.hudEnabled[selectedHud] ? "ON" : "OFF"));
    }

    private static Component comboLabel() {
        return Component.literal("Crit Combo: " + (AudioraConfig.comboEnabled ? "ON" : "OFF"));
    }

    private static Component masterLabel() {
        return Component.literal("Master: " + (AudioraConfig.masterEnabled ? "ON" : "OFF"));
    }

    private static Component toggleLabel(int index) {
        return Component.literal(AudioraConfig.enabled[index] ? "ON" : "OFF");
    }

    private static int nearestStep(int value) {
        int best = VOLUME_STEPS[0];
        int distance = Math.abs(value - best);
        for (int step : VOLUME_STEPS) {
            int candidateDistance = Math.abs(value - step);
            if (candidateDistance < distance) {
                best = step;
                distance = candidateDistance;
            }
        }
        return best;
    }

    private static final class VolumeSlider extends AbstractSliderButton {
        private final String label;

        private VolumeSlider(int x, int y, int width, int height, int initialValue, String label) {
            super(x, y, width, height, Component.empty(), sliderPosition(initialValue));
            this.label = label;
            updateMessage();
        }

        private static double sliderPosition(int volume) {
            int snapped = nearestStep(volume);
            for (int i = 0; i < VOLUME_STEPS.length; i++) {
                if (VOLUME_STEPS[i] == snapped) return (double)i / (VOLUME_STEPS.length - 1);
            }
            return 1.0;
        }

        @Override
        protected void updateMessage() {
            int index = (int)Math.round(this.value * (VOLUME_STEPS.length - 1));
            this.setMessage(Component.literal(VOLUME_STEPS[index] + "%"));
        }

        @Override
        protected void applyValue() {
            int index = (int)Math.round(this.value * (VOLUME_STEPS.length - 1));
            int snapped = VOLUME_STEPS[index];
            this.value = (double)index / (VOLUME_STEPS.length - 1);
            if ("Master".equals(label)) {
                AudioraConfig.masterVolume = snapped;
            } else {
                int category = AudioraConfig.indexOfCategory(label);
                if (category >= 0) AudioraConfig.volume[category] = snapped;
            }
            AudioraConfig.save();
            updateMessage();
        }
    }
}
