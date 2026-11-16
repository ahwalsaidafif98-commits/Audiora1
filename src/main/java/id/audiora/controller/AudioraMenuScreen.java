package id.audiora.controller;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AudioraMenuScreen extends Screen {
    public AudioraMenuScreen() {
        super(Component.literal("Audiora Settings"));
    }

    @Override
    protected void init() {
        int center = width / 2;
        addRenderableWidget(Button.builder(Component.literal("HUD Settings"), b ->
            minecraft.gui.setScreen(new AudioraHudScreen(this)))
            .bounds(center - 100, height / 2 - 24, 200, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Sound Settings"), b ->
            minecraft.gui.setScreen(new AudioraScreen(this)))
            .bounds(center - 100, height / 2 + 4, 200, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(center - 100, height / 2 + 36, 200, 20).build());
    }

    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics,
                                   int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(font, "AUDIORA", width / 2 - font.width("AUDIORA") / 2,
            height / 2 - 62, 0xFFFFFFFF, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
