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
        int c = this.width / 2;
        int top = Math.max(8, (this.height - 250) / 2);
        int maxFirst = Math.max(0, AudioraConfig.CATEGORIES.length - 4);
        firstCategory = Math.max(0, Math.min(maxFirst, firstCategory));

        this.addRenderableWidget(Button.builder(masterLabel(), b -> {
            AudioraConfig.masterEnabled = !AudioraConfig.masterEnabled;
            b.setMessage(masterLabel());
            AudioraConfig.save();
        }).bounds(c - 150, top + 16, 92, 20).build());
        this.addRenderableWidget(new VolumeSlider(c - 53, top + 16, 203, 20, AudioraConfig.masterVolume, "Master"));

        for (int row = 0; row < 4; row++) {
            int index = firstCategory + row;
            if (index >= AudioraConfig.CATEGORIES.length) break;
            final int category = index;
            int x = c - 150, y = top + 50 + row * 22;
            this.addRenderableWidget(Button.builder(toggleLabel(category), b -> {
                AudioraConfig.enabled[category] = !AudioraConfig.enabled[category];
                b.setMessage(toggleLabel(category));
                AudioraConfig.save();
            }).bounds(x, y, 45, 20).build());
            this.addRenderableWidget(new VolumeSlider(x + 49, y, 76, 20,
                AudioraConfig.volume[category], AudioraConfig.CATEGORIES[category]));
            this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> previewCategory(category))
                .bounds(x + 128, y, 22, 20).build());
        }

        this.addRenderableWidget(Button.builder(Component.literal("↑"), b -> {
            firstCategory = Math.max(0, firstCategory - 1);
            rebuild();
        }).bounds(c - 150, top + 140, 42, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("↓"), b -> {
            firstCategory = Math.min(maxFirst, firstCategory + 1);
            rebuild();
        }).bounds(c - 104, top + 140, 42, 20).build());
        this.addRenderableWidget(Button.builder(comboLabel(), b -> {
            AudioraConfig.comboEnabled = !AudioraConfig.comboEnabled;
            AudioraConfig.hudEnabled[7] = AudioraConfig.comboEnabled;
            b.setMessage(comboLabel());
            AudioraConfig.save();
        }).bounds(c - 150, top + 140, 92, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("HUD Settings  ⚙"), b ->
            this.minecraft.gui.setScreen(new AudioraHudScreen(this)))
            .bounds(c - 53, top + 140, 203, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Enable All"), b -> {
            AudioraConfig.masterEnabled = true;
            for (int i = 0; i < AudioraConfig.enabled.length; i++) AudioraConfig.enabled[i] = true;
            AudioraConfig.save();
            rebuild();
        }).bounds(c - 150, top + 169, 92, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Disable All"), b -> {
            AudioraConfig.masterEnabled = false;
            for (int i = 0; i < AudioraConfig.enabled.length; i++) AudioraConfig.enabled[i] = false;
            AudioraConfig.save();
            rebuild();
        }).bounds(c - 53, top + 169, 92, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Reset Sound"), b -> {
            AudioraConfig.masterEnabled = true;
            AudioraConfig.masterVolume = 100;
            for (int i = 0; i < AudioraConfig.enabled.length; i++) {
                AudioraConfig.enabled[i] = true;
                AudioraConfig.volume[i] = 100;
            }
            AudioraConfig.save();
            rebuild();
        }).bounds(c + 44, top + 169, 90, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(c + 104, top + 196, 96, 20).build());
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
        int c = this.width / 2;
        int top = Math.max(8, (this.height - 250) / 2);
        g.text(this.font, "AUDIORA • SOUND CONTROL", c - 83, top - 8, 0xFFFFFFFF, true);
        g.text(this.font, "Master volume", c - 150, top + 6, 0xFFB8D8FF, false);
        for (int row = 0; row < 4; row++) {
            int i = firstCategory + row;
            if (i >= AudioraConfig.CATEGORIES.length) break;
            int x = c - 150, y = top + 50 + row * 22;
            g.text(this.font, AudioraConfig.CATEGORIES[i].toUpperCase(), x, y - 10, 0xFFE8E8E8, false);
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
