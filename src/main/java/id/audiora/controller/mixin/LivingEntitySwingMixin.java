package id.audiora.controller.mixin;

import id.audiora.controller.AudioraConfig;
import id.audiora.controller.AudioraSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.SwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySwingMixin {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"))
    private void audiora$playSwordSwing(InteractionHand hand, CallbackInfo ci) {
        if (!((Object) this instanceof LocalPlayer player)) return;
        if (!(player.getItemInHand(hand).getItem() instanceof SwordItem)) return;
        if (AudioraConfig.scaleForSound(AudioraSounds.SWORD_SWING_ID.toString()) <= 0.0f) return;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(AudioraSounds.SWORD_SWING, 0.85F));
    }
}
