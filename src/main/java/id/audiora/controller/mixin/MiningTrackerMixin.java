package id.audiora.controller.mixin;

import id.audiora.controller.AudioraInfoHud;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MiningTrackerMixin {
    @Unique private BlockState audiora$minedState;

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"))
    private void audiora$captureOre(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        audiora$minedState = net.minecraft.client.Minecraft.getInstance().level == null
            ? null : net.minecraft.client.Minecraft.getInstance().level.getBlockState(pos);
    }

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("RETURN"))
    private void audiora$countOre(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && audiora$minedState != null) {
            AudioraInfoHud.recordMinedBlock(audiora$minedState);
        }
        audiora$minedState = null;
    }
}
