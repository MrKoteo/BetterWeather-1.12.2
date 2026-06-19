package paulevs.betterweather.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import paulevs.betterweather.api.WeatherAPI;
import paulevs.betterweather.client.rendering.BetterWeatherRenderer;

import java.nio.FloatBuffer;

/**
 * Debug keybind (default F6). Dumps live weather/brightness values to STDOUT so they can be
 * read from the game log even while the GUI is hidden (F1) - the only way to measure the
 * F1-dark state, since chat and screenshots can't show it with the HUD off.
 */
@SideOnly(Side.CLIENT)
public final class BWDebugKey {

    public static final KeyBinding DUMP =
        new KeyBinding("BetterWeather Debug Dump", Keyboard.KEY_F6, "BetterWeather");

    private BWDebugKey() {
    }

    public static void poll(Minecraft mc) {
        if (!DUMP.isPressed()) {
            return;
        }
        if (mc.world == null || mc.getRenderViewEntity() == null) {
            System.out.println("[BWDUMP] no world/view entity");
            return;
        }
        // Trigger a render-time GL snapshot on the next frame (tick-time GL state is already
        // cleaned by vanilla, so it can't show our render-time leak).
        BetterWeatherRenderer.dumpRenderEntry = true;
        World world = mc.world;
        Entity entity = mc.getRenderViewEntity();
        int x = MathHelper.floor(entity.posX);
        int y = MathHelper.floor(entity.posY);
        int z = MathHelper.floor(entity.posZ);

        System.out.println(String.format(
            "[BWDUMP] hideGUI=%s y=%d sunBright=%.3f rainStr=%.3f thunderStr=%.3f lastBolt=%d timeOfDay=%d "
                + "| rainDensity=%.3f inCloud=%.3f fogDistance=%.3f isRaining=%s front=%.3f",
            mc.gameSettings.hideGUI,
            y,
            world.getSunBrightness(1.0F),
            world.getRainStrength(1.0F),
            world.getThunderStrength(1.0F),
            world.getLastLightningBolt(),
            world.getWorldTime() % 24000,
            BetterWeatherRenderer.debugRainDensity,
            BetterWeatherRenderer.debugInCloud,
            BetterWeatherRenderer.debugFogDistance,
            WeatherAPI.isRaining(world, x, y, z),
            WeatherAPI.sampleFront(world, x, z, 0.1)));

        FloatBuffer buf = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, buf);
        float cr = buf.get(0), cg = buf.get(1), cb = buf.get(2), ca = buf.get(3);
        buf.clear();
        GL11.glGetFloat(GL11.GL_FOG_COLOR, buf);
        float fr = buf.get(0), fg = buf.get(1), fb = buf.get(2);
        float fogStart = GL11.glGetFloat(GL11.GL_FOG_START);
        float fogEnd = GL11.glGetFloat(GL11.GL_FOG_END);

        System.out.println(String.format(
            "[BWDUMP] GL currentColor=(%.2f,%.2f,%.2f,%.2f) fogEnabled=%s fogStart=%.1f fogEnd=%.1f "
                + "fogColor=(%.2f,%.2f,%.2f) lighting=%s blend=%s tex2D=%s",
            cr, cg, cb, ca,
            GL11.glIsEnabled(GL11.GL_FOG),
            fogStart, fogEnd,
            fr, fg, fb,
            GL11.glIsEnabled(GL11.GL_LIGHTING),
            GL11.glIsEnabled(GL11.GL_BLEND),
            GL11.glIsEnabled(GL11.GL_TEXTURE_2D)));
    }
}
