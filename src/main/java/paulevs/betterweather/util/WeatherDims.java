package paulevs.betterweather.util;

import paulevs.betterweather.config.CommonConfig;

/**
 * Replaces StationAPI dimension tags with simple dimension-ID set checks.
 * Each static method queries the matching {@link CommonConfig} {@code Set<Integer>}.
 */
public final class WeatherDims {

    private WeatherDims() {
    }

    /**
     * Returns true if rain is disabled in the given dimension.
     */
    public static boolean isNoRain(int dimId) {
        return CommonConfig.getNoRainDims().contains(dimId);
    }

    /**
     * Returns true if thunder is disabled in the given dimension.
     */
    public static boolean isNoThunder(int dimId) {
        return CommonConfig.getNoThunderDims().contains(dimId);
    }

    /**
     * Returns true if rain is always active in the given dimension.
     */
    public static boolean isEternalRain(int dimId) {
        return CommonConfig.getEternalRainDims().contains(dimId);
    }

    /**
     * Returns true if thunder is always active in the given dimension.
     */
    public static boolean isEternalThunder(int dimId) {
        return CommonConfig.getEternalThunderDims().contains(dimId);
    }
}
