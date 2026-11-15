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
    private static final int VIEW_TOP = 34;
    private static final int VIEW_BOTTOM_GAP = 8;
    private static final int CONTENT_HEIGHT = 390;

    private final Screen parent;
    private static boolean editingMobHud = true;
    private int scrollOffset;

    public AudioraScreen() {
        this(null);
    }

    public AudioraScreen(Screen parent) {
        super(Component.literal("Audiora Sound Controller"));
        this.parent = parent;
    }

    private int y(int contentY) {
        return contentY - scrollOffset;
    }

    private int maxScroll() {
        return Math.max(0, CONTENT_HEIGHT - (this.height - VIEW_TOP - VIEW_BOTTOM_GAP));
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
        }).bounds(center - 150, y(38), 92, 18).build());

        this.addRenderableWidget(new VolumeSlider(
            center - 53, y(38), 203, 18, AudioraConfig.masterVolume, "Master"
        ));

        for (int i = 0; i < AudioraConfig.CATEGORIES.length; i++) {
            final int index = i;
            int column = i / 4;
            int row = i % 4;
            int x = center - 150 + column * 155;
            int cy = 70 + row * 27;
            this.addRenderableWidget(Button.builder(toggleLabel(index), button -> {
                AudioraConfig.enabled[index] = !AudioraConfig.enabled[index];
                button.setMessage(toggleLabel(index));
                AudioraConfig.save();
            }).bounds(x, y(cy), 45, 18).build());
            this.addRenderableWidget(new VolumeSlider(
                x + 50, y(cy), 100, 18, AudioraConfig.volume[index], AudioraConfig.CATEGORIES[index]
            ));
        }

        this.addRenderableWidget(Button.builder(hudPositionLabel(), button -> {
            editingMobHud = !editingMobHud;
            button.setMessage(hudPositionLabel());
        }).bounds(center - 150, y(205), 78, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("←"), button -> {
            if (editingMobHud) AudioraConfig.mobInfoOffsetX -= 5;
            else AudioraConfig.itemInfoOffsetX -= 5;
            AudioraConfig.save();
        }).bounds(center - 68, y(205), 34, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("→"), button -> {
            if (editingMobHud) AudioraConfig.mobInfoOffsetX += 5;
            else AudioraConfig.itemInfoOffsetX += 5;
            AudioraConfig.save();
        }).bounds(center - 31, y(205), 34, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("↑"), button -> {
            if (editingMobHud) AudioraConfig.mobInfoOffsetY -= 5;
            else AudioraConfig.itemInfoOffsetY -= 5;
            AudioraConfig.save();
        }).bounds(center + 6, y(205), 34, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("↓"), button -> {
            if (editingMobHud) AudioraConfig.mobInfoOffsetY += 5;
            else AudioraConfig.itemInfoOffsetY += 5;
            AudioraConfig.save();
        }).bounds(center + 43, y(205), 34, 18).build());

        this.addRenderableWidget(Button.builder(comboLabel(), button -> {
            AudioraConfig.comboEnabled = !AudioraConfig.comboEnabled;
            button.setMessage(comboLabel());
            AudioraConfig.save();
        }).bounds(center - 150, y(240), 86, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("← Kiri"), button -> {
            AudioraConfig.comboOffsetX -= 5;
            AudioraConfig.save();
        }).bounds(center - 58, y(240), 48, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Kanan →"), button -> {
            AudioraConfig.comboOffsetX += 5;
            AudioraConfig.save();
        }).bounds(center - 7, y(240), 48, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("↑ Atas"), button -> {
            AudioraConfig.comboOffsetY -= 5;
            AudioraConfig.save();
        }).bounds(center + 44, y(240), 48, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("↓ Bawah"), button -> {
            AudioraConfig.comboOffsetY += 5;
            AudioraConfig.save();
        }).bounds(center + 95, y(240), 48, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Enable All"), button -> {
            for (int i = 0; i < AudioraConfig.enabled.length; i++) AudioraConfig.enabled[i] = true;
            AudioraConfig.save();
            rebuild();
        }).bounds(center - 150, y(285), 92, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Disable All"), button -> {
            for (int i = 0; i < AudioraConfig.enabled.length; i++) AudioraConfig.enabled[i] = false;
            AudioraConfig.save();
            rebuild();
        }).bounds(center - 53, y(285), 92, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
            AudioraConfig.reset();
            rebuild();
        }).bounds(center + 44, y(285), 55, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("Apply / Save"), button -> {
            AudioraConfig.save();
            this.onClose();
        }).bounds(center + 104, y(285), 96, 18).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int oldOffset = scrollOffset;
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset - (int)Math.round(scrollY * 24.0)));
        if (oldOffset != scrollOffset) {
            rebuild();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.enableScissor(0, VIEW_TOP, this.width, this.height - VIEW_BOTTOM_GAP);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int center = this.width / 2;
        graphics.text(this.font, "Master / Noise level", center - 150, y(27), 0xFFB8D8FF, false);
        for (int i = 0; i < AudioraConfig.CATEGORIES.length; i++) {
            int column = i / 4;
            int row = i % 4;
            int x = center - 150 + column * 155;
            int cy = 70 + row * 27;
            graphics.text(this.font, AudioraConfig.CATEGORIES[i].toUpperCase(), x, y(cy - 10), 0xFFE8E8E8, false);
        }
        graphics.text(this.font, "HUD Mob / Item position", center - 150, y(193), 0xFFB8D8FF, false);
        graphics.text(this.font, "Crit Combo position", center - 150, y(228), 0xFFB8D8FF, false);
        graphics.disableScissor();

        drawAudioraIcon(graphics, center - 112, 7);
        graphics.text(this.font, "AUDIORA - Sound settings", center - 82, 12, 0xFFFFFFFF, true);

        if (maxScroll() > 0) {
            int trackTop = VIEW_TOP + 2;
            int trackBottom = this.height - VIEW_BOTTOM_GAP - 2;
            int trackHeight = trackBottom - trackTop;
            int thumbHeight = Math.max(18, trackHeight * (trackHeight / (trackHeight + maxScroll())));
            int thumbY = trackTop + (trackHeight - thumbHeight) * scrollOffset / maxScroll();
            graphics.fill(this.width - 5, trackTop, this.width - 2, trackBottom, 0x66505050);
            graphics.fill(this.width - 5, thumbY, this.width - 2, thumbY + thumbHeight, 0xFFB982F2);
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

    private static Component hudPositionLabel() {
        return Component.literal("Posisi: " + (editingMobHud ? "Mob" : "Item"));
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
