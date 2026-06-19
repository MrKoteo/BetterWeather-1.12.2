package paulevs.betterweather.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import paulevs.betterweather.client.BWDebugKey;
import paulevs.betterweather.client.rendering.BetterWeatherRenderer;
import paulevs.betterweather.client.sound.WeatherSounds;

@SideOnly(Side.CLIENT)
public class BWClientEvents {

    // DEBUG: hard-disable ALL BetterWeather per-frame render + fog hooks at build time, to
    // determine if our client rendering is the source of the F1 darkness. Set false to restore.
    private static final boolean DEBUG_DISABLE_RENDER = false;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (DEBUG_DISABLE_RENDER) return;
        BetterWeatherRenderer.renderAfterWater(
            event.getPartialTicks(), Minecraft.getMinecraft());
    }

    @SubscribeEvent
    public void onFogColors(EntityViewRenderEvent.FogColors event) {
        if (DEBUG_DISABLE_RENDER) return;
        Minecraft mc = Minecraft.getMinecraft();
        BetterWeatherRenderer.fogColorR = event.getRed();
        BetterWeatherRenderer.fogColorG = event.getGreen();
        BetterWeatherRenderer.fogColorB = event.getBlue();
        BetterWeatherRenderer.updateFogColor(mc, (float) event.getRenderPartialTicks());
        event.setRed(BetterWeatherRenderer.fogColorR);
        event.setGreen(BetterWeatherRenderer.fogColorG);
        event.setBlue(BetterWeatherRenderer.fogColorB);
    }

    @SubscribeEvent
    public void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
        if (DEBUG_DISABLE_RENDER) return;
        BetterWeatherRenderer.updateFogDepth(event.getFarPlaneDistance());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            WeatherSounds.updateSound();
            BWDebugKey.poll(Minecraft.getMinecraft());
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.getGui() instanceof GuiMainMenu) {
            WeatherSounds.stop();
        }
    }
}
