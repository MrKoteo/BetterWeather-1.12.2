package paulevs.betterweather.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import paulevs.betterweather.api.WeatherAPI;
import paulevs.betterweather.client.rendering.BetterWeatherRenderer;

/**
 * Client-side diagnostic: dumps the live weather/fog sample values at the player,
 * so visual bugs (stuck fog haze, etc.) can be debugged from real numbers.
 */
public class CommandBWDebug extends CommandBase {

    @Override
    public String getName() {
        return "bwdebug";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/bwdebug - print live BetterWeather fog/cloud sample values at your position";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        Entity entity = sender.getCommandSenderEntity();
        if (entity == null) {
            return;
        }
        World world = entity.getEntityWorld();
        int x = MathHelper.floor(entity.posX);
        int y = MathHelper.floor(entity.posY);
        int z = MathHelper.floor(entity.posZ);

        int cloudHeight = (int) WeatherAPI.getCloudHeight(world);
        int rainHeight = WeatherAPI.getRainHeight(world, x, z);
        boolean raining = WeatherAPI.isRaining(world, x, y, z);
        float front = WeatherAPI.sampleFront(world, x, z, 0.1);

        send(sender, TextFormatting.AQUA + "[BWDebug] " + TextFormatting.WHITE
            + "y=" + y + "  cloudY=" + cloudHeight + "  rainTop=" + rainHeight
            + "  isRaining=" + raining + "  front=" + fmt(front));
        send(sender, TextFormatting.AQUA + "[BWDebug] " + TextFormatting.WHITE
            + "fog: rainDensity=" + fmt(BetterWeatherRenderer.debugRainDensity)
            + "  inCloud=" + fmt(BetterWeatherRenderer.debugInCloud)
            + "  fogDistance=" + fmt(BetterWeatherRenderer.debugFogDistance));
        send(sender, TextFormatting.AQUA + "[BWDebug] " + TextFormatting.WHITE
            + "vanilla: sunBright=" + fmt(world.getSunBrightness(1.0F))
            + "  rainStr=" + fmt(world.getRainStrength(1.0F))
            + "  thunderStr=" + fmt(world.getThunderStrength(1.0F))
            + "  lastBolt=" + world.getLastLightningBolt()
            + "  timeOfDay=" + (world.getWorldTime() % 24000));
    }

    private static void send(ICommandSender sender, String msg) {
        sender.sendMessage(new TextComponentString(msg));
    }

    private static String fmt(float v) {
        return String.format("%.3f", v);
    }
}
