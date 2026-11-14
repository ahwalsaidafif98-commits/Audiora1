package id.audiora.controller;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AudioraScreen extends Screen {
    public AudioraScreen() {
        super(Component.literal("Audiora Sound Controller"));
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        this.addRenderableWidget(Button.builder(masterLabel(), button -> {
            AudioraConfig.masterEnabled = !AudioraConfig.masterEnabled;
            button.setMessage(masterLabel());
            AudioraConfig.save();
        }).bounds(center - 150, 32, 145, 20).build());

        this.addRenderableWidget(Button.builder(volumeLabel(AudioraConfig.masterVolume), button -> {
            AudioraConfig.masterVolume = (AudioraConfig.masterVolume + 10) % 110;
            button.setMessage(volumeLabel(AudioraConfig.masterVolume));
            AudioraConfig.save();
        }).bounds(center + 5, 32, 145, 20).build());

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
            }).bounds(x, y, 92, 20).build());
            this.addRenderableWidget(Button.builder(volumeLabel(AudioraConfig.volume[index]), button -> {
                AudioraConfig.volume[index] = (AudioraConfig.volume[index] + 10) % 110;
                button.setMessage(volumeLabel(AudioraConfig.volume[index]));
                AudioraConfig.save();
            }).bounds(x + 97, y, 53, 20).build());
        }

        this.addRenderableWidget(Button.builder(comboLabel(), button -> {
            AudioraConfig.comboEnabled = !AudioraConfig.comboEnabled;
            button.setMessage(comboLabel());
            AudioraConfig.save();
        }).bounds(center - 150, this.height - 82, 92, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("X-"), button -> {
            AudioraConfig.comboOffsetX -= 5;
            AudioraConfig.save();
        }).bounds(center - 55, this.height - 82, 30, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("X+"), button -> {
            AudioraConfig.comboOffsetX += 5;
            AudioraConfig.save();
        }).bounds(center - 22, this.height - 82, 30, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Y-"), button -> {
            AudioraConfig.comboOffsetY -= 5;
            AudioraConfig.save();
        }).bounds(center + 11, this.height - 82, 30, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Y+"), button -> {
            AudioraConfig.comboOffsetY += 5;
            AudioraConfig.save();
        }).bounds(center + 44, this.height - 82, 30, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Enable All"), button -> {
            for (int i = 0; i < AudioraConfig.enabled.length; i++) AudioraConfig.enabled[i] = true;
            AudioraConfig.save();
            this.minecraft.gui.setScreen(new AudioraScreen());
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
        graphics.text(this.font, "Combo position: X " + AudioraConfig.comboOffsetX + " | Y " + AudioraConfig.comboOffsetY + " (5 px per tap)", this.width / 2 - 150, this.height - 96, 0xFFB8D8FF, false);
        for (int i = 0; i < AudioraConfig.CATEGORIES.length; i++) {
            int column = i / 4;
            int row = i % 4;
            int x = this.width / 2 - 150 + column * 155;
            int y = 68 + row * 31;
            graphics.text(this.font, AudioraConfig.CATEGORIES[i].toUpperCase(), x, y - 10, 0xFFE8E8E8, false);
        }
    }

    private static void drawAudioraIcon(GuiGraphicsExtractor graphics, int x, int y) {
        // Ikon pixel-art ungu: cincin luar, cincin dalam terbuka, dan panah naik.
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
        // Panah mengarah ke atas.
        graphics.fill(x + 11, y + 5, x + 14, y + 17, 0xFFEBD9FF);
        graphics.fill(x + 8, y + 8, x + 17, y + 11, 0xFFEBD9FF);
        graphics.fill(x + 7, y + 9, x + 10, y + 12, 0xFFEBD9FF);
        graphics.fill(x + 15, y + 9, x + 18, y + 12, 0xFFEBD9FF);
        graphics.fill(x + 12, y + 14, x + 16, y + 20, 0xFFEBD9FF);
    }

    @Override
    public void onClose() {
        AudioraConfig.save();
        this.minecraft.gui.setScreen(null);
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

    private static Component volumeLabel(int value) {
        return Component.literal(value + "%");
    }
}