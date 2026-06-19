package paulevs.betterweather;

import net.minecraft.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import paulevs.betterweather.util.LightningLightBlock;

@Mod.EventBusSubscriber(modid = BetterWeather.MODID)
public class BWBlocks {

    // Technical invisible light block placed at a bolt's strike point.
    // No ItemBlock - never obtainable in inventory.
    public static Block lightningLight;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        lightningLight = new LightningLightBlock();
        event.getRegistry().register(lightningLight);
    }
}
