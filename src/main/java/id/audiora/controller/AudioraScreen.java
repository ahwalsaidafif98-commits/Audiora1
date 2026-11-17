package id.audiora.controller;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

public final class AudioraScreen extends Screen {
    private static final int[] VOLUME_STEPS = {0,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100};
    private final Screen parent;
    private int firstCategory;

    public AudioraScreen() { this(null); }
    public AudioraScreen(Screen parent) {
        super(Component.literal("Audiora Sound Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        int panelW = Math.min(430, width - 24);
        int panelX = (width - panelW) / 2;
        int panelY = Math.max(12, (height - 270) / 2);
        int sideW = 112;
        int mainX = panelX + sideW + 12;
        int mainW = panelW - sideW - 20;
        int rowY = panelY + 42;
        int maxFirst = Math.max(0, AudioraConfig.CATEGORIES.length - 5);
        firstCategory = Math.max(0, Math.min(maxFirst, firstCategory));

        addRenderableWidget(Button.builder(Component.literal("General  •"), b -> {})
            .bounds(panelX, panelY + 30, sideW, 22).build());
        addRenderableWidget(Button.builder(Component.literal("HUD Settings"), b ->
            this.minecraft.gui.setScreen(new AudioraHudScreen(this)))
            .bounds(panelX, panelY + 56, sideW, 22).build());

        addRenderableWidget(Button.builder(masterLabel(), b -> {
            AudioraConfig.masterEnabled = !AudioraConfig.masterEnabled;
            b.setMessage(masterLabel()); AudioraConfig.save();
        }).bounds(mainX, rowY, 100, 20).build());
        addRenderableWidget(new VolumeSlider(mainX + 106, rowY, mainW - 106, 20,
            AudioraConfig.masterVolume, "Master"));

        for (int row = 0; row < 5; row++) {
            int index = firstCategory + row;
            if (index >= AudioraConfig.CATEGORIES.length) break;
            final int category = index;
            int y = rowY + 34 + row * 25;
            addRenderableWidget(Button.builder(toggleLabel(category), b -> {
                AudioraConfig.enabled[category] = !AudioraConfig.enabled[category];
                b.setMessage(toggleLabel(category)); AudioraConfig.save();
            }).bounds(mainX, y, 42, 20).build());
            addRenderableWidget(new VolumeSlider(mainX + 47, y, mainW - 98, 20,
                AudioraConfig.volume[category], AudioraConfig.CATEGORIES[category]));
            addRenderableWidget(Button.builder(Component.literal("▶"), b -> previewCategory(category))
                .bounds(mainX + mainW - 46, y, 42, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("↑"), b -> {
            firstCategory = Math.max(0, firstCategory - 1); rebuild();
        }).bounds(mainX, rowY + 164, 42, 20).build());
        addRenderableWidget(Button.builder(Component.literal("↓"), b -> {
            firstCategory = Math.min(maxFirst, firstCategory + 1); rebuild();
        }).bounds(mainX + 47, rowY + 164, 42, 20).build());
        addRenderableWidget(Button.builder(comboLabel(), b -> {
            AudioraConfig.comboEnabled = !AudioraConfig.comboEnabled;
            AudioraConfig.hudEnabled[7] = AudioraConfig.comboEnabled;
            b.setMessage(comboLabel()); AudioraConfig.save();
        }).bounds(mainX + 94, rowY + 164, 88, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Enable All"), b -> {
            AudioraConfig.masterEnabled = true;
            for (int i = 0; i < AudioraConfig.enabled.length; i++) AudioraConfig.enabled[i] = true;
            AudioraConfig.save(); rebuild();
        }).bounds(mainX, rowY + 192, 88, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Disable All"), b -> {
            AudioraConfig.masterEnabled = false;
            for (int i = 0; i < AudioraConfig.enabled.length; i++) AudioraConfig.enabled[i] = false;
            AudioraConfig.save(); rebuild();
        }).bounds(mainX + 94, rowY + 192, 88, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            AudioraConfig.masterEnabled = true; AudioraConfig.masterVolume = 100;
            for (int i = 0; i < AudioraConfig.enabled.length; i++) {
                AudioraConfig.enabled[i] = true; AudioraConfig.volume[i] = 100;
            }
            AudioraConfig.save(); rebuild();
        }).bounds(mainX + 188, rowY + 192, Math.max(52, mainW - 188), 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(panelX + panelW - 78, panelY + 240, 78, 20).build());
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int maxFirst = Math.max(0, AudioraConfig.CATEGORIES.length - 4);
        firstCategory = Math.max(0, Math.min(maxFirst, firstCategory + (deltaY < 0 ? 1 : -1)));
        rebuild();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        int panelW = Math.min(430, width - 24);
        int panelX = (width - panelW) / 2;
        int panelY = Math.max(12, (height - 270) / 2);
        int sideW = 112, mainX = panelX + sideW + 12;
        int mainW = panelW - sideW - 20, rowY = panelY + 42;
        g.fill(panelX - 5, panelY - 8, panelX + panelW + 5, panelY + 270, 0xB9222B38);
        g.fill(panelX - 5, panelY - 8, panelX + panelW + 5, panelY + 18, 0xFF28527C);
        g.text(font, "AUDIORA SETTINGS", panelX + 5, panelY, 0xFFFFFFFF, true);
        g.fill(panelX, panelY + 30, panelX + sideW, panelY + 53, 0xAA305F91);
        g.text(font, "GENERAL", panelX + 9, panelY + 37, 0xFFFFFFFF, true);
        g.text(font, "Master Sound", mainX, rowY - 12, 0xFFB8D8FF, true);
        g.text(font, "Sound categories", mainX, rowY + 24, 0xFFB8D8FF, true);
        for (int row = 0; row < 5; row++) {
            int i = firstCategory + row;
            if (i >= AudioraConfig.CATEGORIES.length) break;
            int y = rowY + 34 + row * 25;
            g.text(font, AudioraConfig.CATEGORIES[i].toUpperCase(), mainX + 2, y - 10, 0xFFE8E8E8, false);
        }
    }

    private static void previewCategory(int category) {
        String name = AudioraConfig.CATEGORIES[category];
        String[] terms = switch (name) {
            case "inventory" -> new String[]{"chest", "barrel", "shulker"};
            case "player" -> new String[]{"player", "step", "hurt"};
            case "items" -> new String[]{"item", "pickup", "equip"};
            case "birds" -> new String[]{"parrot", "chicken", "ambient"};
            case "insects" -> new String[]{"bee", "cave", "ambient"};
            case "nature" -> new String[]{"water", "ambient", "rain"};
            default -> new String[]{"ui.button", "ui"};
        };
        for (Identifier id : BuiltInRegistries.SOUND_EVENT.keySet()) {
            String path = id.getPath();
            for (String term : terms) if (path.contains(term)) {
                var holder = BuiltInRegistries.SOUND_EVENT.get(id);
                if (holder.isPresent()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(holder.get(), 0.8F));
                    return;
                }
            }
        }
    }

    private static Component masterLabel() {
        return Component.literal("Master: " + (AudioraConfig.masterEnabled ? "ON" : "OFF"));
    }
    private static Component comboLabel() {
        return Component.literal("Combo: " + (AudioraConfig.comboEnabled ? "ON" : "OFF"));
    }
    private static Component toggleLabel(int i) {
        return Component.literal(AudioraConfig.enabled[i] ? "ON" : "OFF");
    }
    private static int nearestStep(int v) {
        int best = 0, d = Integer.MAX_VALUE;
        for (int s : VOLUME_STEPS) {
            int n = Math.abs(v - s);
            if (n < d) { d = n; best = s; }
        }
        return best;
    }

    private static final class VolumeSlider extends AbstractSliderButton {
        private final String label;
        VolumeSlider(int x, int y, int w, int h, int value, String label) {
            super(x, y, w, h, Component.empty(), position(value));
            this.label = label;
            updateMessage();
        }
        private static double position(int v) {
            int n = nearestStep(v);
            for (int i = 0; i < VOLUME_STEPS.length; i++)
                if (VOLUME_STEPS[i] == n) return (double) i / (VOLUME_STEPS.length - 1);
            return 1;
        }
        @Override protected void updateMessage() {
            int i = (int) Math.round(value * (VOLUME_STEPS.length - 1));
            setMessage(Component.literal(VOLUME_STEPS[i] + "%"));
        }
        @Override protected void applyValue() {
            int i = (int) Math.round(value * (VOLUME_STEPS.length - 1));
            int v = VOLUME_STEPS[i];
            value = (double) i / (VOLUME_STEPS.length - 1);
            if ("Master".equals(label)) AudioraConfig.masterVolume = v;
            else {
                int cat = AudioraConfig.indexOfCategory(label);
                if (cat >= 0) AudioraConfig.volume[cat] = v;
            }
            AudioraConfig.save();
            updateMessage();
        }
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() {
        AudioraConfig.save();
        this.minecraft.gui.setScreen(parent);
    }
}
