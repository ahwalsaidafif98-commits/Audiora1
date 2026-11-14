package id.audiora.controller;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AudioraScreen extends Screen {
    private static final int[] VOLUME_STEPS = {
        0, 10, 15, 20, 25, 30, 35, 40, 45, 50,
        55, 60, 65, 70, 75, 80, 85, 90, 95, 100
    };

    private final Screen parent;

    public AudioraScreen() {
        this(null);
    }

    public AudioraScreen(Screen parent) {
        super(Component.literal("Audiora Sound Controller"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        this.addRenderableWidget(Button.builder(masterLabel(), button -> {
            AudioraConfig.masterEnabled = !AudioraConfig.masterEnabled;
            button.setMessage(masterLabel());
            AudioraConfig.save();
        }).bounds(center - 150, 32, 92, 20).build());

        this.addRenderableWidget(new VolumeSlider(
            center - 53, 32, 203, 20, AudioraConfig.masterVolume, "Master"
        ));

        for (int i = 0; i < AudioraConfig.CATEGORIES.length; i++) {
            final int index = i;
            int column = i / 4;
            int row = i % 4;
            int x = center - 150 + column * 155;
            int y = 68 + row * 31;
            this.addRenderableWidget(Button.builder(toggleLabel(index), button -> {
                AudioraConfig.enabled[index] = !AudioraConfig.enabled[index];
                button.setMessage(toggleLabel(index));
                AudioraConfig.save();
            }).bounds(x, y, 45, 20).build());
            this.addRenderableWidget(new VolumeSlider(
                x + 50, y, 100, 20, AudioraConfig.volume[index], AudioraConfig.CATEGORIES[index]
            ));
        }

        this.addRenderableWidget(Button.builder(comboLabel(), button -> {
            AudioraConfig.comboEnabled = !AudioraConfig.comboEnabled;
            button.setMessage(comboLabel());
            AudioraConfig.save();
        }).bounds(center - 150, this.height - 82, 86, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("← Kiri"), button -> {
            AudioraConfig.comboOffsetX -= 5;
            AudioraConfig.save();
        }).bounds(center - 58, this.height - 82, 48, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Kanan →"), button -> {
            AudioraConfig.comboOffsetX += 5;
            AudioraConfig.save();
        }).bounds(center - 7, this.height - 82, 48, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("↑ Atas"), button -> {
            AudioraConfig.comboOffsetY -= 5;
            AudioraConfig.save();
        }).bounds(center + 44, this.height - 82, 48, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("↓ Bawah"), button -> {
            AudioraConfig.comboOffsetY += 5;
            AudioraConfig.save();
        }).bounds(center + 95, this.height - 82, 48, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Enable All"), button -> {
            for (int i = 0; i < AudioraConfig.enabled.length; i++) AudioraConfig.enabled[i] = true;
            AudioraConfig.save();
            this.minecraft.gui.setScreen(new AudioraScreen(this));
        }).bounds(center - 150, this.height - 54, 92, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Disable All"), button -> {
            for (int i = 0; i < AudioraConfig.enabled.length; i++) AudioraConfig.enabled[i] = false;
            AudioraConfig.save();
            this.init();
        }).bounds(center - 53, this.height - 54, 92, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
            AudioraConfig.reset();
            this.init();
        }).bounds(center + 44, this.height - 54, 55, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Apply / Save"), button -> {
            AudioraConfig.save();
            this.onClose();
        }).bounds(center + 104, this.height - 54, 96, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        drawAudioraIcon(graphics, this.width / 2 - 112, 7);
        graphics.text(this.font, "AUDIORA - Sound settings", this.width / 2 - 82, 12, 0xFFFFFFFF, true);
        graphics.text(this.font, "Master / Noise level", this.width / 2 - 150, 22, 0xFFB8D8FF, false);
        graphics.text(this.font, "Geser posisi tulisan COMBO:", this.width / 2 - 150, this.height - 110, 0xFFB8D8FF, false);
        graphics.text(this.font, "Mendatar: " + AudioraConfig.comboOffsetX + " | Tegak: " + AudioraConfig.comboOffsetY + " (5 px tiap tap)", this.width / 2 - 150, this.height - 96, 0xFFB8D8FF, false);
        for (int i = 0; i < AudioraConfig.CATEGORIES.length; i++) {
            int column = i / 4;
            int row = i % 4;
            int x = this.width / 2 - 150 + column * 155;
            int y = 68 + row * 31;
            graphics.text(this.font, AudioraConfig.CATEGORIES[i].toUpperCase(), x, y - 10, 0xFFE8E8E8, false);
        }
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
    public void onClose() {
        AudioraConfig.save();
        this.minecraft.gui.setScreen(parent);
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
