package paulevs.betterweather.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import paulevs.betterweather.api.WeatherAPI;
import paulevs.betterweather.config.CommonConfig;
import paulevs.betterweather.entity.EntityBWLightning;

/**
 * Per-chunk lightning conductor search and strike spawning.
 * <p>
 * Called once per chunk each tick (gated by {@link #tick()} to fire at most
 * once every 32 calls).  Behaviour is a direct port of the original Beta 1.7.3
 * {@code LightningUtil} with these translations:
 * <ul>
 *   <li>{@code Level} → {@code World}</li>
 *   <li>{@code level.random} → {@code world.rand}</li>
 *   <li>{@code WeatherTags.LIGHTNING_ROD} → OreDictionary {@code "blockLightningRod"}</li>
 *   <li>{@code level.addEntity(LightningEntity)} → {@code world.spawnEntity(EntityBWLightning)}</li>
 * </ul>
 */
public final class LightningUtil {

    private static int lightningTicks;

    private LightningUtil() {
    }

    /**
     * Advance the per-mod-tick counter.  Call from a {@code WorldTickEvent}
     * or similar repeating hook.
     */
    public static void tick() {
        lightningTicks = (lightningTicks + 1) & 31;
    }

    /**
     * Attempt to spawn a lightning bolt in chunk ({@code cx}, {@code cz}).
     * <p>
     * If a lightning-rod block (OreDictionary {@code blockLightningRod}) is
     * found within the configured {@link CommonConfig#getRodCheckSide()} radius,
     * the bolt strikes that position.  Otherwise a random position in the
     * chunk is chosen.
     *
     * @param world the world
     * @param cx    chunk X coordinate
     * @param cz    chunk Z coordinate
     */
    public static void processChunk(World world, int cx, int cz) {
        if (lightningTicks > 0) return;

        if (CommonConfig.getLightningChance() > 1
            && world.rand.nextInt(CommonConfig.getLightningChance()) > 0) return;

        int px, py, pz;
        int lx = 0, ly = Integer.MIN_VALUE, lz = 0;

        // --- rod search ---
        int radius = CommonConfig.getRodCheckSide();
        rodSearch:
        for (int dx = -radius; dx <= radius; dx++) {
            px = (cx << 4) + dx + 8;
            for (int dz = -radius; dz <= radius; dz++) {
                pz = (cz << 4) + dz + 8;
                py = WeatherAPI.getRainHeight(world, px, pz);
                if (!WeatherAPI.isThundering(world, px, py, pz)) continue;
                if (!isLightningRod(world, px, py - 1, pz)) continue;
                lx = px;
                ly = py;
                lz = pz;
                break rodSearch;
            }
        }

        // --- fallback: random position in chunk ---
        if (ly == Integer.MIN_VALUE) {
            lx = (cx << 4) | world.rand.nextInt(16);
            lz = (cz << 4) | world.rand.nextInt(16);
            ly = WeatherAPI.getRainHeight(world, lx, lz);
            if (!WeatherAPI.isThundering(world, lx, ly, lz)) return;
        }

        world.spawnEntity(new EntityBWLightning(world, lx, ly, lz));
    }

    /**
     * Returns {@code true} if the block at ({@code x},{@code y},{@code z})
     * matches the OreDictionary name {@code "blockLightningRod"}.
     */
    private static boolean isLightningRod(World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        ItemStack stack = new ItemStack(world.getBlockState(pos).getBlock());
        // OreDictionary.getOreIDs throws "Stack can not be invalid!" on an empty stack.
        // Air (and any block with no item form) yields an empty stack - skip it.
        if (stack.isEmpty()) return false;
        for (int id : OreDictionary.getOreIDs(stack)) {
            if ("blockLightningRod".equals(OreDictionary.getOreName(id))) {
                return true;
            }
        }
        return false;
    }
}
