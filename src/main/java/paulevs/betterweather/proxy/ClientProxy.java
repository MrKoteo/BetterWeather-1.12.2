package paulevs.betterweather.proxy;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import paulevs.betterweather.client.BWDebugKey;
import paulevs.betterweather.client.RenderBWLightningFactory;
import paulevs.betterweather.command.CommandBWDebug;
import paulevs.betterweather.command.CommandBWToggle;
import paulevs.betterweather.command.CommandFindRain;
import paulevs.betterweather.config.ClientConfig;
import paulevs.betterweather.entity.EntityBWLightning;
import paulevs.betterweather.event.BWClientEvents;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ClientConfig.init();
        RenderingRegistry.registerEntityRenderingHandler(
            EntityBWLightning.class, new RenderBWLightningFactory());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        BWClientEvents clientHandler = new BWClientEvents();
        MinecraftForge.EVENT_BUS.register(clientHandler);
        FMLCommonHandler.instance().bus().register(clientHandler);

        ClientCommandHandler.instance.registerCommand(new CommandFindRain());
        ClientCommandHandler.instance.registerCommand(new CommandBWDebug());
        ClientCommandHandler.instance.registerCommand(new CommandBWToggle());
        ClientRegistry.registerKeyBinding(BWDebugKey.DUMP);
    }
}
