package paulevs.betterweather.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import paulevs.betterweather.client.rendering.BetterWeatherRenderer;
import paulevs.betterweather.client.rendering.CloudRenderer;

import java.util.Arrays;
import java.util.List;

/**
 * Client-side debug bisect: toggle individual BetterWeather render subsystems off/on
 * to isolate which one causes a visual bug.
 * <p>
 * {@code /bwtoggle clouds} - cloud rendering<br>
 * {@code /bwtoggle rain} - rain/snow rendering<br>
 * {@code /bwtoggle scroll} - cloud drift (freeze)
 */
public class CommandBWToggle extends CommandBase {

    @Override
    public String getName() {
        return "bwtoggle";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/bwtoggle <clouds|rain|scroll> - toggle a render subsystem for debugging";
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
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, net.minecraft.util.math.BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "clouds", "rain", "scroll", "fog", "rainstr", "vclouds", "rainsnow");
        }
        return Arrays.asList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 1) {
            send(sender, TextFormatting.YELLOW + getUsage(sender));
            return;
        }
        switch (args[0].toLowerCase()) {
            case "clouds":
                BetterWeatherRenderer.debugSkipClouds = !BetterWeatherRenderer.debugSkipClouds;
                report(sender, "clouds rendering", !BetterWeatherRenderer.debugSkipClouds);
                break;
            case "rain":
                BetterWeatherRenderer.debugSkipWeather = !BetterWeatherRenderer.debugSkipWeather;
                report(sender, "rain/snow rendering", !BetterWeatherRenderer.debugSkipWeather);
                break;
            case "scroll":
                CloudRenderer.debugFreezeScroll = !CloudRenderer.debugFreezeScroll;
                report(sender, "cloud drift", !CloudRenderer.debugFreezeScroll);
                break;
            case "fog":
                BetterWeatherRenderer.debugSkipFog = !BetterWeatherRenderer.debugSkipFog;
                report(sender, "weather fog", !BetterWeatherRenderer.debugSkipFog);
                break;
            case "rainstr":
                BetterWeatherRenderer.debugSkipRainStrength = !BetterWeatherRenderer.debugSkipRainStrength;
                report(sender, "rain strength dimming", !BetterWeatherRenderer.debugSkipRainStrength);
                break;
            case "vclouds":
                BetterWeatherRenderer.debugLetVanillaClouds = !BetterWeatherRenderer.debugLetVanillaClouds;
                report(sender, "vanilla-cloud cancel", !BetterWeatherRenderer.debugLetVanillaClouds);
                break;
            case "rainsnow":
                BetterWeatherRenderer.debugLetRainSnow = !BetterWeatherRenderer.debugLetRainSnow;
                report(sender, "vanilla rain/snow cancel", !BetterWeatherRenderer.debugLetRainSnow);
                break;
            default:
                send(sender, TextFormatting.YELLOW + getUsage(sender));
        }
    }

    private static void report(ICommandSender sender, String what, boolean on) {
        send(sender, TextFormatting.AQUA + "[BWToggle] " + TextFormatting.WHITE + what + ": "
            + (on ? TextFormatting.GREEN + "ON" : TextFormatting.RED + "OFF"));
    }

    private static void send(ICommandSender sender, String msg) {
        sender.sendMessage(new TextComponentString(msg));
    }
}
