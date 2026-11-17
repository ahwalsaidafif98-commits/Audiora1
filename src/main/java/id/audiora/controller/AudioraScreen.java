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
    private int selectedCategory;
    private int firstCategory;

    public AudioraScreen() { this(null); }
    public AudioraScreen(Screen parent) {
        super(Component.literal("Audiora Sound Settings"));
        this.parent = parent;
    }
    private int panelW() { return Math.min(500, width - 20); }
    private int panelX() { return (width - panelW()) / 2; }
    private int panelY() { return Math.max(8, (height - 270) / 2); }
    private int sideW() { return Math.min(154, panelW() / 3); }
    private int mainX() { return panelX() + sideW() + 12; }
    private int mainW() { return panelW() - sideW() - 22; }
    private int visibleRows() { return Math.max(4, Math.min(9, (height - 120) / 21)); }

    @Override protected void init() {
        clearWidgets();
        int x = panelX(), y = panelY(), rows = visibleRows();
        int maxFirst = Math.max(0, AudioraConfig.CATEGORIES.length - rows);
        firstCategory = Math.max(0, Math.min(maxFirst, firstCategory));
        for (int row = 0; row < rows; row++) {
            int index = firstCategory + row;
            if (index >= AudioraConfig.CATEGORIES.length) break;
            final int category = index;
            addRenderableWidget(Button.builder(Component.literal((selectedCategory == category ? "› " : "") + title(category)), b -> {
                selectedCategory = category; rebuild();
            }).bounds(x + 4, y + 39 + row * 21, sideW() - 8, 19).build());
        }
        int right = mainX(), w = mainW(), cy = y + 58;
        addRenderableWidget(Button.builder(masterLabel(), b -> {
            AudioraConfig.masterEnabled = !AudioraConfig.masterEnabled;
            AudioraConfig.save(); rebuild();
        }).bounds(right, cy, Math.max(66, w / 2 - 4), 20).build());
        addRenderableWidget(new VolumeSlider(right + w / 2 + 2, cy, Math.max(60, w - w / 2 - 2), 20,
            AudioraConfig.masterVolume, "Master"));
        addRenderableWidget(Button.builder(toggleLabel(selectedCategory), b -> {
            AudioraConfig.enabled[selectedCategory] = !AudioraConfig.enabled[selectedCategory];
            AudioraConfig.save(); rebuild();
        }).bounds(right, cy + 55, Math.max(66, w / 2 - 4), 20).build());
        addRenderableWidget(new VolumeSlider(right, cy + 105, w, 20,
            AudioraConfig.volume[selectedCategory], AudioraConfig.CATEGORIES[selectedCategory]));
        addRenderableWidget(Button.builder(Component.literal("▶ Preview"), b -> previewCategory(selectedCategory))
            .bounds(right, cy + 139, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Enable All"), b -> {
            AudioraConfig.masterEnabled = true;
            java.util.Arrays.fill(AudioraConfig.enabled, true);
            AudioraConfig.save(); rebuild();
        }).bounds(right, cy + 173, Math.max(60, w / 2 - 4), 20).build());
        addRenderableWidget(Button.builder(Component.literal("Disable All"), b -> {
            AudioraConfig.masterEnabled = false;
            java.util.Arrays.fill(AudioraConfig.enabled, false);
            AudioraConfig.save(); rebuild();
        }).bounds(right + w / 2 + 2, cy + 173, Math.max(60, w - w / 2 - 2), 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            AudioraConfig.masterEnabled = true; AudioraConfig.masterVolume = 100;
            java.util.Arrays.fill(AudioraConfig.enabled, true); java.util.Arrays.fill(AudioraConfig.volume, 100);
            AudioraConfig.save(); rebuild();
        }).bounds(right, cy + 201, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("↑"), b -> { firstCategory = Math.max(0, firstCategory - 1); rebuild(); })
            .bounds(x + 4, y + 42 + rows * 21, 38, 18).build());
        addRenderableWidget(Button.builder(Component.literal("↓"), b -> { firstCategory = Math.min(maxFirst, firstCategory + 1); rebuild(); })
            .bounds(x + 46, y + 42 + rows * 21, 38, 18).build());
        addRenderableWidget(Button.builder(Component.literal("HUD Settings"), b -> minecraft.gui.setScreen(new AudioraHudScreen(this)))
            .bounds(x + 4, y + 238, sideW() - 8, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(x + panelW() - 78, y + 238, 74, 20).build());
    }

    private void rebuild() { clearWidgets(); init(); }
    private String title(int i) {
        String s = AudioraConfig.CATEGORIES[i];
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    @Override public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int max = Math.max(0, AudioraConfig.CATEGORIES.length - visibleRows());
        firstCategory = Math.max(0, Math.min(max, firstCategory + (dy < 0 ? 1 : -1)));
        rebuild(); return true;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        int x = panelX(), y = panelY(), right = mainX(), w = mainW(), rows = visibleRows();
        g.fill(x - 3, y - 5, x + panelW() + 3, y + 266, 0x55202935);
        g.fill(x - 3, y - 5, x + panelW() + 3, y + 20, 0x88304761);
        g.text(font, "AUDIORA / SOUND SETTINGS", x + 5, y + 3, 0xFFFFFFFF, true);
        g.text(font, "SOUND", x + 7, y + 27, 0xFFB8D8FF, true);
        g.text(font, "VOLUME CONTROL", right, y + 27, 0xFFB8D8FF, true);
        g.fill(right - 3, y + 43, right + w + 2, y + 226, 0x33202935);
        g.text(font, "Master volume", right + 3, y + 48, 0xFFE8F1FA, true);
        g.text(font, title(selectedCategory), right + 3, y + 98, 0xFFFFFFFF, true);
        g.text(font, "Category volume: " + AudioraConfig.volume[selectedCategory] + "%", right + 3, y + 132, 0xFFE8F1FA, false);
        g.text(font, "Master: " + (AudioraConfig.masterEnabled ? "ON" : "OFF"), right + w / 2 + 5, y + 63, 0xFFB8D8FF, false);
        for (int row = 0; row < rows; row++) {
            int i = firstCategory + row;
            if (i >= AudioraConfig.CATEGORIES.length) break;
            int yy = y + 39 + row * 21;
            g.fill(x + 4, yy, x + sideW() - 4, yy + 19,
                i == selectedCategory ? 0x664A83B8 : 0x33202935);
            g.text(font, (AudioraConfig.enabled[i] ? "● " : "○ ") + title(i), x + 9, yy + 5,
                i == selectedCategory ? 0xFFFFFFFF : 0xFFD0D8E2, i == selectedCategory);
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
    private static Component masterLabel() { return Component.literal("Master: " + (AudioraConfig.masterEnabled ? "ON" : "OFF")); }
    private static Component toggleLabel(int i) { return Component.literal(AudioraConfig.enabled[i] ? "ON" : "OFF"); }
    private static int nearestStep(int v) {
        int best = 0, d = Integer.MAX_VALUE;
        for (int s : VOLUME_STEPS) { int n = Math.abs(v - s); if (n < d) { d = n; best = s; } }
        return best;
    }
    private static final class VolumeSlider extends AbstractSliderButton {
        private final String label;
        VolumeSlider(int x, int y, int w, int h, int value, String label) {
            super(x, y, w, h, Component.empty(), position(value));
            this.label = label; updateMessage();
        }
        private static double position(int v) {
            int n = nearestStep(v);
            for (int i = 0; i < VOLUME_STEPS.length; i++) if (VOLUME_STEPS[i] == n) return (double) i / (VOLUME_STEPS.length - 1);
            return 1;
        }
        @Override protected void updateMessage() {
            int i = (int) Math.round(value * (VOLUME_STEPS.length - 1));
            setMessage(Component.literal(VOLUME_STEPS[i] + "%"));
        }
        @Override protected void applyValue() {
            int i = (int) Math.round(value * (VOLUME_STEPS.length - 1));
            int v = VOLUME_STEPS[i]; value = (double) i / (VOLUME_STEPS.length - 1);
            if ("Master".equals(label)) AudioraConfig.masterVolume = v;
            else { int cat = AudioraConfig.indexOfCategory(label); if (cat >= 0) AudioraConfig.volume[cat] = v; }
            AudioraConfig.save(); updateMessage();
        }
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { AudioraConfig.save(); minecraft.gui.setScreen(parent); }
}
