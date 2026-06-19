package paulevs.betterweather.mixin.common;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import paulevs.betterweather.api.WeatherAPI;

@Mixin(World.class)
public class MixinWorld {

    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    private void betterweather_isRaining(CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(false);
    }

    @Inject(method = "isThundering", at = @At("HEAD"), cancellable = true)
    private void betterweather_isThundering(CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(false);
    }

    @Inject(method = "isRainingAt", at = @At("HEAD"), cancellable = true)
    private void betterweather_isRainingAt(BlockPos pos, CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(WeatherAPI.isRaining(
            (World) (Object) this, pos.getX(), pos.getY(), pos.getZ()));
    }

    @Inject(method = "getRainStrength", at = @At("HEAD"), cancellable = true)
    private void betterweather_getRainStrength(float delta, CallbackInfoReturnable<Float> info) {
        World self = (World) (Object) this;
        if (!self.isRemote) return;
        // Debug bisect (/bwtoggle rainstr): let vanilla return so the rain sky/lightmap
        // dimming is removed, to test whether that is the source of the F1 darkness.
        if (paulevs.betterweather.client.rendering.BetterWeatherRenderer.debugSkipRainStrength) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.getRenderViewEntity() == null) return;
        Entity entity = mc.getRenderViewEntity();
        float density = WeatherAPI.getRainDensity(
            mc.world, entity.posX, entity.posY, entity.posZ, true);
        info.setReturnValue(density);
    }

    @Inject(method = "getThunderStrength", at = @At("HEAD"), cancellable = true)
    private void betterweather_getThunderStrength(float delta, CallbackInfoReturnable<Float> info) {
        info.setReturnValue(0.0F);
    }
}
