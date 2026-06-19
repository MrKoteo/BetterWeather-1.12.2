package paulevs.betterweather.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import paulevs.betterweather.api.WeatherAPI;

/**
 * Client-side debug/test command: scans outward from the player and reports the
 * nearest column that is currently raining under BetterWeather's localized sim.
 * <p>
 * Usage: {@code /findrain [radius] [step]}. With no arguments it auto-expands the
 * search radius until rain is found (or a hard cap is reached). Because rain is
 * sampled from cloud fronts (not a global on/off), this is the quick way to
 * locate a rain front when {@code eternalRain} is off.
 */
public class CommandFindRain extends CommandBase {

    private static final int DEFAULT_STEP = 4;
    private static final int AUTO_START_RADIUS = 256;
    private static final int AUTO_MAX_RADIUS = 4096;

    @Override
    public String getName() {
        return "findrain";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/findrain [radius] [step] - locate the nearest localized rain front";
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
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        Entity entity = sender.getCommandSenderEntity();
        if (entity == null) {
            return;
        }
        World world = entity.getEntityWorld();
        int ox = MathHelper.floor(entity.posX);
        int oz = MathHelper.floor(entity.posZ);

        int step = args.length > 1 ? parseInt(args[1], 1, 64) : DEFAULT_STEP;

        Hit hit = new Hit();
        int searched;
        if (args.length > 0) {
            // Explicit radius -> single fixed scan.
            searched = parseInt(args[0], 16, AUTO_MAX_RADIUS);
            scan(world, ox, oz, searched, step, hit);
        } else {
            // No args -> auto-expand until found or capped.
            searched = AUTO_START_RADIUS;
            while (true) {
                hit.reset();
                scan(world, ox, oz, searched, step, hit);
                if (hit.found() || searched >= AUTO_MAX_RADIUS) {
                    break;
                }
                searched = Math.min(searched * 2, AUTO_MAX_RADIUS);
            }
        }

        if (!hit.found()) {
            sender.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "[BetterWeather] No rain within " + searched + " blocks."));
            return;
        }

        int dist = (int) Math.sqrt((double) hit.distSq);
        float front = WeatherAPI.sampleFront(world, hit.x, hit.z, 0.1);
        sender.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "[BetterWeather] " + TextFormatting.WHITE
                + "Nearest rain: " + hit.x + ", " + hit.y + ", " + hit.z
                + " (" + dist + " blocks away, front " + String.format("%.2f", front) + ")  "
                + TextFormatting.GRAY + "/tp " + hit.x + " " + hit.y + " " + hit.z));
    }

    private static void scan(World world, int ox, int oz, int radius, int step, Hit hit) {
        for (int dx = -radius; dx <= radius; dx += step) {
            int x = ox + dx;
            for (int dz = -radius; dz <= radius; dz += step) {
                int z = oz + dz;
                long distSq = (long) dx * dx + (long) dz * dz;
                if (distSq >= hit.distSq) {
                    continue;
                }
                int y = WeatherAPI.getRainHeight(world, x, z);
                if (!WeatherAPI.isRaining(world, x, y, z)) {
                    continue;
                }
                hit.distSq = distSq;
                hit.x = x;
                hit.y = y;
                hit.z = z;
            }
        }
    }

    private static final class Hit {
        long distSq = Long.MAX_VALUE;
        int x, y, z;

        boolean found() {
            return distSq != Long.MAX_VALUE;
        }

        void reset() {
            distSq = Long.MAX_VALUE;
        }
    }
}
