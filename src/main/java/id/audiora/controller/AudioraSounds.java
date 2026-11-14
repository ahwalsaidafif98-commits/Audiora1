package id.audiora.controller;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Registers Audiora's built-in inventory sound events. */
public final class AudioraSounds {
    public static final Identifier INVENTORY_OPEN_ID =
        Identifier.fromNamespaceAndPath("audiora_controller", "inv.buka");
    public static final Identifier INVENTORY_CLOSE_ID =
        Identifier.fromNamespaceAndPath("audiora_controller", "inv.tutup");

    public static SoundEvent INVENTORY_OPEN;
    public static SoundEvent INVENTORY_CLOSE;

    private AudioraSounds() {}

    public static void initialize() {
        INVENTORY_OPEN = Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            INVENTORY_OPEN_ID,
            SoundEvent.createVariableRangeEvent(INVENTORY_OPEN_ID)
        );
        INVENTORY_CLOSE = Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            INVENTORY_CLOSE_ID,
            SoundEvent.createVariableRangeEvent(INVENTORY_CLOSE_ID)
        );
    }
}
