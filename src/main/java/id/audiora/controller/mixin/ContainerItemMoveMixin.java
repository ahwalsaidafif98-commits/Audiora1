package id.audiora.controller.mixin;

import id.audiora.controller.AudioraConfig;
import id.audiora.controller.AudioraSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerItemMoveMixin {
    @Inject(
        method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
        at = @At("HEAD")
    )
    private void audiora$playItemMove(Slot slot, int slotId, int buttonNum, ContainerInput input, CallbackInfo ci) {
        if (slot == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || AudioraConfig.scaleForSound(AudioraSounds.ITEM_MOVE_ID.toString()) <= 0.0f) return;
        client.getSoundManager().play(SimpleSoundInstance.forUI(AudioraSounds.ITEM_MOVE, 0.9F));
    }
}
