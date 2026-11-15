package id.audiora.controller;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class AudioraSounds {
    public static final Identifier JUMP_ID = Identifier.fromNamespaceAndPath("audiora_controller", "jump");
    public static final Identifier ITEM_MOVE_ID = Identifier.fromNamespaceAndPath("audiora_controller", "item_move");
    public static final Identifier SWORD_SWING_ID = Identifier.fromNamespaceAndPath("audiora_controller", "sword_swing");

    public static SoundEvent JUMP;
    public static SoundEvent ITEM_MOVE;
    public static SoundEvent SWORD_SWING;

    private AudioraSounds() {}

    public static void initialize() {
        JUMP = register(JUMP_ID);
        ITEM_MOVE = register(ITEM_MOVE_ID);
        SWORD_SWING = register(SWORD_SWING_ID);
    }

    private static SoundEvent register(Identifier id) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
