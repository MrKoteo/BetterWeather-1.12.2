package paulevs.betterweather.mixin.client;

import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses vanilla cloud rendering. BetterWeather draws its own volumetric clouds via
 * RenderWorldLastEvent, so the vanilla layer must not also render (otherwise both show).
 */
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void betterweather_renderClouds(float partialTicks, int pass,
            double x, double y, double z, CallbackInfo info) {
        if (paulevs.betterweather.client.rendering.BetterWeatherRenderer.debugLetVanillaClouds) return;
        info.cancel();
    }
}
