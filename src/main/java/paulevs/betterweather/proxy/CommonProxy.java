package paulevs.betterweather.proxy;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import paulevs.betterweather.config.CommonConfig;
import paulevs.betterweather.event.BWCommonEvents;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        CommonConfig.init();
    }

    public void init(FMLInitializationEvent event) {
        BWCommonEvents commonHandler = new BWCommonEvents();
        FMLCommonHandler.instance().bus().register(commonHandler);
    }
}
