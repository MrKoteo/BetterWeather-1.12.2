package paulevs.betterweather.mixin.common;

import net.minecraft.block.BlockFire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulevs.betterweather.api.WeatherAPI;

import java.util.Random;

@Mixin(BlockFire.class)
public class MixinBlockFire {

    @Inject(method = "updateTick", at = @At("HEAD"), cancellable = true)
    private void betterweather_updateTick(World worldIn, BlockPos pos,
            IBlockState state, Random rand, CallbackInfo info) {
        if (WeatherAPI.isRaining(worldIn, pos.getX(), pos.getY() + 1, pos.getZ())) {
            worldIn.setBlockToAir(pos);
            info.cancel();
        }
    }
}
