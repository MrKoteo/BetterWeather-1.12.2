package paulevs.betterweather.mixin.client;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "renderRainSnow", at = @At("HEAD"), cancellable = true)
    private void betterweather_renderRainSnow(float partialTicks, CallbackInfo info) {
        if (paulevs.betterweather.client.rendering.BetterWeatherRenderer.debugLetRainSnow) return;
        info.cancel();
    }
}
