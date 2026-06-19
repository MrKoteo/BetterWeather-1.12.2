package paulevs.betterweather;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import paulevs.betterweather.entity.EntityBWLightning;

@Mod.EventBusSubscriber(modid = BetterWeather.MODID)
public class BWEntities {

    private static int nextId = 0;

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(
            EntityEntryBuilder.create()
                .entity(EntityBWLightning.class)
                .id(new ResourceLocation(BetterWeather.MODID, "bw_lightning"), nextId++)
                .name(BetterWeather.MODID + ".bw_lightning")
                .tracker(256, 1, true)
                .build()
        );
    }
}
