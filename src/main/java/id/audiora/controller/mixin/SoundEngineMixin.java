package id.audiora.controller.mixin;

import id.audiora.controller.AudioraConfig;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Inject(
        method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
        at = @At("RETURN"),
        cancellable = true
    )
    private void audiora$applySoundControls(SoundInstance instance, CallbackInfoReturnable<Float> cir) {
        float scale = AudioraConfig.scaleForSound(instance.getId().toString());
        cir.setReturnValue(cir.getReturnValue() * scale);
    }
}
