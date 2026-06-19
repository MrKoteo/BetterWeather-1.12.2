package paulevs.betterweather.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import paulevs.betterweather.BWSounds;
import paulevs.betterweather.api.WeatherAPI;

@SideOnly(Side.CLIENT)
public class WeatherSounds {
    private static RainLoopSound currentSound;

    /**
     * Call each client tick. Starts or maintains the looping rain sound based on
     * current rain density at the player position. Safe to call repeatedly.
     */
    public static void updateSound() {
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        EntityPlayer player = mc.player;

        if (world == null || player == null) {
            stop();
            return;
        }

        float density = WeatherAPI.getRainDensity(world, player.posX, player.posY, player.posZ, false);

        if (currentSound != null && currentSound.isDonePlaying()) {
            currentSound = null;
        }

        if (density > 0.0F) {
            if (currentSound == null) {
                currentSound = new RainLoopSound();
                mc.getSoundHandler().playSound(currentSound);
            }
        } else {
            stop();
        }
    }

    /**
     * Immediately stop the rain loop. Safe to call when no sound is playing.
     */
    public static void stop() {
        if (currentSound != null) {
            currentSound.markDone();
            currentSound = null;
        }
    }

    private static class RainLoopSound extends MovingSound {
        void markDone() {
            this.donePlaying = true;
        }

        private RainLoopSound() {
            super(BWSounds.RAIN, SoundCategory.WEATHER);
            this.repeat = true;
            this.attenuationType = AttenuationType.NONE;
            this.volume = 0.0F;
        }

        @Override
        public void update() {
            Minecraft mc = Minecraft.getMinecraft();
            World world = mc.world;
            EntityPlayer player = mc.player;

            if (world == null || player == null) {
                this.donePlaying = true;
                return;
            }

            this.xPosF = (float) player.posX;
            this.yPosF = (float) player.posY;
            this.zPosF = (float) player.posZ;

            float density = WeatherAPI.getRainDensity(world, player.posX, player.posY, player.posZ, false);

            if (density <= 0.0F) {
                this.donePlaying = true;
                return;
            }

            this.volume = density * 0.5F;

            boolean underRoof = player.posY < WeatherAPI.getRainHeight(
                world, MathHelper.floor(player.posX), MathHelper.floor(player.posZ));
            this.pitch = underRoof ? 0.25F : 1.0F;
        }
    }
}
