package paulevs.betterweather.mixin.common;

import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldInfo.class)
public class MixinWorldInfo {

    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    private void betterweather_isRaining(CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(false);
    }
}
