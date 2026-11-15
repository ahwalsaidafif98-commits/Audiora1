package id.audiora.controller;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class AudioraSounds {
    public static final Identifier JUMP_ID = id("jump");
    public static final Identifier ITEM_MOVE_ID = id("item_move");
    public static final Identifier INVENTORY_OPEN_ID = id("inventory_open");
    public static final Identifier INVENTORY_CLOSE_ID = id("inventory_close");
    public static final Identifier SWORD_SWING_ID = id("sword_swing");
    public static final Identifier TOOL_SWORD_ID = id("item_sword_select");
    public static final Identifier TOOL_PICKAXE_ID = id("item_pickaxe_select");
    public static final Identifier TOOL_AXE_ID = id("item_axe_select");
    public static final Identifier TOOL_SHOVEL_ID = id("item_shovel_select");
    public static final Identifier TOOL_HOE_ID = id("item_hoe_select");
    public static final Identifier TOOL_SPEAR_ID = id("item_spear_select");
    public static final Identifier TOOL_MACE_ID = id("item_mace_select");
    public static final Identifier TOOL_TRIDENT_ID = id("item_trident_select");
    public static final Identifier NETHERITE_STEP_ID = id("armor_netherite_step");

    public static SoundEvent JUMP;
    public static SoundEvent ITEM_MOVE;
    public static SoundEvent INVENTORY_OPEN;
    public static SoundEvent INVENTORY_CLOSE;
    public static SoundEvent SWORD_SWING;
    public static SoundEvent TOOL_SWORD;
    public static SoundEvent TOOL_PICKAXE;
    public static SoundEvent TOOL_AXE;
    public static SoundEvent TOOL_SHOVEL;
    public static SoundEvent TOOL_HOE;
    public static SoundEvent TOOL_SPEAR;
    public static SoundEvent TOOL_MACE;
    public static SoundEvent TOOL_TRIDENT;
    public static SoundEvent NETHERITE_STEP;

    private AudioraSounds() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("audiora_controller", path);
    }

    public static void initialize() {
        JUMP = register(JUMP_ID);
        ITEM_MOVE = register(ITEM_MOVE_ID);
        INVENTORY_OPEN = register(INVENTORY_OPEN_ID);
        INVENTORY_CLOSE = register(INVENTORY_CLOSE_ID);
        SWORD_SWING = register(SWORD_SWING_ID);
        TOOL_SWORD = register(TOOL_SWORD_ID);
        TOOL_PICKAXE = register(TOOL_PICKAXE_ID);
        TOOL_AXE = register(TOOL_AXE_ID);
        TOOL_SHOVEL = register(TOOL_SHOVEL_ID);
        TOOL_HOE = register(TOOL_HOE_ID);
        TOOL_SPEAR = register(TOOL_SPEAR_ID);
        TOOL_MACE = register(TOOL_MACE_ID);
        TOOL_TRIDENT = register(TOOL_TRIDENT_ID);
        NETHERITE_STEP = register(NETHERITE_STEP_ID);
    }

    private static SoundEvent register(Identifier id) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
