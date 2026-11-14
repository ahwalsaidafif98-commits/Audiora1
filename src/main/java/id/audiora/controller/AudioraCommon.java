package id.audiora.controller;

import net.fabricmc.api.ModInitializer;

/** Registers Audiora sound events before the client sound manager loads resources. */
public final class AudioraCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        AudioraSounds.initialize();
    }
}
