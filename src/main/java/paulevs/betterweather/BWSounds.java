package paulevs.betterweather;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "better_weather")
public class BWSounds {
    public static SoundEvent RAIN;

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        RAIN = new SoundEvent(new ResourceLocation("better_weather", "ambient.weather.rain"))
            .setRegistryName("better_weather", "ambient.weather.rain");
        event.getRegistry().register(RAIN);
    }
}
