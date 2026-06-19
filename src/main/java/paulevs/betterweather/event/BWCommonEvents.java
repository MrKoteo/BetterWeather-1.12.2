package paulevs.betterweather.event;

import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import paulevs.betterweather.util.LightningUtil;

public class BWCommonEvents {

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;

        WorldServer worldServer = (WorldServer) event.world;
        LightningUtil.tick();

        for (Chunk chunk : worldServer.getChunkProvider().getLoadedChunks()) {
            LightningUtil.processChunk(worldServer, chunk.x, chunk.z);
        }
    }
}
