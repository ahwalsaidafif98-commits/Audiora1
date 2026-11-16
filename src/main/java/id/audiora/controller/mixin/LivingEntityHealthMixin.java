package id.audiora.controller.mixin;

import id.audiora.controller.AudioraInfoHud;
import id.audiora.controller.AudioraCombo;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHealthMixin {
    @Unique private float audiora$oldHealth;

    @Inject(method = "setHealth(F)V", at = @At("HEAD"))
    private void audiora$captureHealth(float health, CallbackInfo ci) {
        this.audiora$oldHealth = ((LivingEntity)(Object)this).getHealth();
    }

    @Inject(method = "setHealth(F)V", at = @At("TAIL"))
    private void audiora$detectDamage(float health, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        AudioraInfoHud.onLivingEntityHealthChanged(entity, this.audiora$oldHealth, entity.getHealth());
        if (entity.getHealth() < this.audiora$oldHealth) AudioraCombo.onConfirmedDamage(entity.getId());
    }
}
