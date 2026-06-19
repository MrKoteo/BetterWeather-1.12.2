package paulevs.betterweather.api;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import paulevs.betterweather.config.CommonConfig;
import paulevs.betterweather.util.BWMath;
import paulevs.betterweather.util.ImageSampler;
import paulevs.betterweather.util.WeatherDims;

import java.util.ArrayList;
import java.util.List;

public class WeatherAPI {
    private static final ImageSampler MAIN_SHAPE_SAMPLER = new ImageSampler("data/better_weather/clouds/main_shape.png");
    private static final ImageSampler LARGE_DETAILS_SAMPLER = new ImageSampler("data/better_weather/clouds/large_details.png");
    private static final ImageSampler VARIATION_SAMPLER = new ImageSampler("data/better_weather/clouds/variation.png");
    private static final ImageSampler FRONTS_SAMPLER = new ImageSampler("data/better_weather/clouds/rain_fronts.png");
    private static final ImageSampler RAIN_DENSITY = new ImageSampler("data/better_weather/clouds/rain_density.png");
    private static final ImageSampler VANILLA_CLOUDS = new ImageSampler("data/better_weather/clouds/vanilla_clouds.png").setSmooth(true);
    private static final ImageSampler THUNDERSTORMS = new ImageSampler("data/better_weather/clouds/thunderstorms.png");
    private static final float[] CLOUD_SHAPE = new float[64];
    private static final int[] OFFSET_X;
    private static final int[] OFFSET_Z;

    public static boolean isRaining(World world, int x, int y, int z) {
        if (world.provider.doesWaterVaporize()) return false;

        int dimId = world.provider.getDimension();
        if (WeatherDims.isNoRain(dimId)) return false;

        if (y > getCloudHeight(world) + 8) return false;
        if (y < getRainHeight(world, x, z)) return false;

        z = (int) (z - world.getTotalWorldTime() * CommonConfig.getCloudsSpeed() * 32);
        if (CommonConfig.isEternalRain() || WeatherDims.isEternalRain(dimId)) {
            return !CommonConfig.useVanillaClouds() || getCloudDensity(x, 2, z, 1F) > 0.5F;
        }

        float rainFront = sampleFront(world, x, z, 0.1);
        if (rainFront < 0.2F) return false;

        float coverage = getCoverage(rainFront);
        int sampleHeight = CommonConfig.useVanillaClouds() ? 2 : 7;
        return getCloudDensity(x, sampleHeight, z, rainFront) > coverage;
    }

    public static boolean isThundering(World world, int x, int y, int z) {
        return isRaining(world, x, y, z) && sampleThunderstorm(world, x, z, 0.05) > 0.3F;
    }

    public static float inCloud(World world, double x, double y, double z) {
        z -= world.getTotalWorldTime() * CommonConfig.getCloudsSpeed() * 32;
        int x1 = MathHelper.floor(x / 2.0) << 1;
        int y1 = MathHelper.floor(y / 2.0) << 1;
        int z1 = MathHelper.floor(z / 2.0) << 1;

        int x2 = x1 + 2;
        int y2 = y1 + 2;
        int z2 = z1 + 2;

        float dx = (float) (x - x1) / 2F;
        float dy = (float) (y - y1) / 2F;
        float dz = (float) (z - z1) / 2F;

        float a = isInCloud(world, x1, y1, z1) ? 1F : 0F;
        float b = isInCloud(world, x2, y1, z1) ? 1F : 0F;
        float c = isInCloud(world, x1, y2, z1) ? 1F : 0F;
        float d = isInCloud(world, x2, y2, z1) ? 1F : 0F;
        float e = isInCloud(world, x1, y1, z2) ? 1F : 0F;
        float f = isInCloud(world, x2, y1, z2) ? 1F : 0F;
        float g = isInCloud(world, x1, y2, z2) ? 1F : 0F;
        float h = isInCloud(world, x2, y2, z2) ? 1F : 0F;

        return BWMath.interpolate3D(dx, dy, dz, a, b, c, d, e, f, g, h);
    }

    private static boolean isInCloud(World world, int x, int y, int z) {
        if (world.provider.doesWaterVaporize()) return false;
        int start = (int) getCloudHeight(world);
        if (y < start || y > start + 64) return false;
        float rainFront = sampleFront(world, x, z, 0.1);
        float coverage = getCoverage(rainFront);
        return getCloudDensity(x, y - start, z, rainFront) > coverage;
    }

    public static float getCloudDensity(int x, int y, int z, float rainFront) {
        if (CommonConfig.useVanillaClouds()) {
            if (y > 6) return 0;
            float shape = y == 0 || y == 5 ? 1 : 0;
            return VANILLA_CLOUDS.sample(x / 16.0, z / 16.0) * 3 - shape;
        }

        float density = MAIN_SHAPE_SAMPLER.sample(x * 0.75F, z * 0.75F);
        density += LARGE_DETAILS_SAMPLER.sample(x * 2.5F, z * 2.5F);

        density -= VARIATION_SAMPLER.sample(y * 2.5F, x * 2.5F) * 0.05F;
        density -= VARIATION_SAMPLER.sample(z * 2.5F, y * 2.5F) * 0.05F;
        density -= VARIATION_SAMPLER.sample(z * 2.5F, x * 2.5F) * 0.05F;

        int value = (int) (BWMath.hashCode(x, y, z) % 3);
        density -= value * 0.01F;

        float density1 = density - CLOUD_SHAPE[BWMath.clamp(y << 1, 0, 63)];
        float density2 = density + MAIN_SHAPE_SAMPLER.sample(x * 1.5F, z * 1.5F) - CLOUD_SHAPE[BWMath.clamp(y, 0, 63)] * 3F;

        return BWMath.lerp(rainFront, density1, density2);
    }

    public static float sampleFront(World world, int x, int z, double scale) {
        if (CommonConfig.isEternalRain()) return 1F;

        int dimId = world.provider.getDimension();
        if (WeatherDims.isNoRain(dimId)) return 0F;
        if (WeatherDims.isEternalRain(dimId)) return 1F;

        float front = FRONTS_SAMPLER.sample(x * scale, z * scale);
        if (!CommonConfig.isFrequentRain()) {
            scale *= 0.7;
            front *= RAIN_DENSITY.sample(x * scale, z * scale);
        }
        return front;
    }

    public static float sampleThunderstorm(World world, int x, int z, double scale) {
        if (CommonConfig.isEternalThunder()) return 1F;

        int dimId = world.provider.getDimension();
        if (WeatherDims.isNoThunder(dimId)) return 0F;
        if (WeatherDims.isEternalThunder(dimId)) return 1F;

        return THUNDERSTORMS.sample(x * scale, z * scale);
    }

    public static float getCoverage(float rainFront) {
        return BWMath.lerp(rainFront, 1.3F, 0.5F);
    }

    public static int getRainHeight(World world, int x, int z) {
        int max = (int) (getCloudHeight(world) + 4);
        int height = world.getHeight(x, z);
        if (height >= max) return max;
        Chunk chunk = world.getChunk(x >> 4, z >> 4);
        int lx = x & 15;
        int lz = z & 15;
        for (int y = max; y > height; y--) {
            IBlockState state = chunk.getBlockState(new BlockPos(lx, y, lz));
            if (state.getMaterial() == Material.AIR) continue;
            if (state.isOpaqueCube() || state.isFullCube() || state.getMaterial().isLiquid()) return y + 1;
        }
        return height;
    }

    public static float getRainDensity(World world, double x, double y, double z, boolean includeSnow) {
        int x1 = MathHelper.floor(x);
        int y1 = MathHelper.floor(y);
        int z1 = MathHelper.floor(z);
        int x2 = x1 + 1;
        int z2 = z1 + 1;

        float dx = (float) (x - x1);
        float dz = (float) (z - z1);
        dz -= (float) ((world.getTotalWorldTime() * CommonConfig.getCloudsSpeed() * 32) % 1.0);

        float a = getRainDensity(world, x1, y1, z1, includeSnow);
        float b = getRainDensity(world, x2, y1, z1, includeSnow);
        float c = getRainDensity(world, x1, y1, z2, includeSnow);
        float d = getRainDensity(world, x2, y1, z2, includeSnow);

        float value = BWMath.interpolate2D(dx, dz, a, b, c, d);
        return BWMath.clamp(value, 0F, 1F);
    }

    private static float getRainDensity(World world, int x, int y, int z, boolean includeSnow) {
        if (world.provider.doesWaterVaporize()) return 0;

        Biome biome = world.getBiome(new BlockPos(x, 0, z));
        boolean canSnow = biome.getEnableSnow();

        int count = 0;
        for (int i = 0; i < OFFSET_X.length; i++) {
            boolean snowCheck = includeSnow || !canSnow;
            if (snowCheck && isRaining(world, x + OFFSET_X[i], y, z + OFFSET_Z[i])) {
                count++;
                if (count >= 64) return 1F;
            }
        }

        return count / 64F;
    }

    static {
        for (byte i = 0; i < 16; i++) {
            CLOUD_SHAPE[i] = (16 - i) / 16F;
            CLOUD_SHAPE[i] *= CLOUD_SHAPE[i];
        }
        for (byte i = 16; i < 64; i++) {
            CLOUD_SHAPE[i] = (i - 16) / 48F;
            CLOUD_SHAPE[i] *= CLOUD_SHAPE[i];
        }

        int radius = 6;
        int capacity = radius * 2 + 1;
        capacity *= capacity;

        List<int[]> offsets = new ArrayList<>(capacity);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    offsets.add(new int[] { x, z });
                }
            }
        }
        offsets.sort((v1, v2) -> {
            int d1 = v1[0] * v1[0] + v1[1] * v1[1];
            int d2 = v2[0] * v2[0] + v2[1] * v2[1];
            return Integer.compare(d1, d2);
        });

        int size = offsets.size();
        OFFSET_X = new int[size];
        OFFSET_Z = new int[size];
        for (int i = 0; i < size; i++) {
            OFFSET_X[i] = offsets.get(i)[0];
            OFFSET_Z[i] = offsets.get(i)[1];
        }
    }

    /**
     * Cloud layer height for {@code world}. In the overworld this is the configured BetterWeather
     * height (preset or custom) so the client render and the server-side weather sim agree - the
     * old code returned 128 on the client but a hardcoded 108 on the server, a 20-block mismatch.
     * Other dimensions keep their vanilla provider height.
     */
    public static float getCloudHeight(World world) {
        if (world.provider.getDimension() == 0) {
            return CommonConfig.getCloudHeight();
        }
        return world.provider.getCloudHeight();
    }
}
