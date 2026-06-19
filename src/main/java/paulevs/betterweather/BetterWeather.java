package paulevs.betterweather;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import paulevs.betterweather.proxy.CommonProxy;

@Mod(
    modid = BetterWeather.MODID,
    name = BetterWeather.NAME,
    version = BetterWeather.VERSION,
    guiFactory = "paulevs.betterweather.client.config.BWGuiFactory"
)
public class BetterWeather {

    public static final String MODID = "better_weather";
    public static final String NAME = "Better Weather";
    public static final String VERSION = "0.3.1";

    @Mod.Instance(MODID)
    public static BetterWeather instance;

    @SidedProxy(
        clientSide = "paulevs.betterweather.proxy.ClientProxy",
        serverSide = "paulevs.betterweather.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }
}
