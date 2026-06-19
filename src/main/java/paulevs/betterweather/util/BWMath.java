package paulevs.betterweather.util;

/**
 * Reimplements StationAPI MathHelper helpers used by BetterWeather.
 * All methods match original StationAPI semantics exactly.
 */
public final class BWMath {

    private BWMath() {
    }

    /**
     * Linear interpolation: {@code a + delta * (b - a)}.
     * Matches StationAPI {@code MathHelper.lerp}.
     */
    public static float lerp(float delta, float a, float b) {
        return a + delta * (b - a);
    }

    /**
     * Bilinear interpolation over a 2D unit square.
     * Standard lerp nesting: lerp(dz, lerp(dx, a, b), lerp(dx, c, d)).
     * Matches StationAPI {@code MathHelper.interpolate2D}.
     */
    public static float interpolate2D(float dx, float dz, float a, float b, float c, float d) {
        float i1 = lerp(dx, a, b);
        float i2 = lerp(dx, c, d);
        return lerp(dz, i1, i2);
    }

    /**
     * Trilinear interpolation over a 3D unit cube.
     * Standard trilinear: lerp along x, then y, then z.
     * Matches StationAPI {@code MathHelper.interpolate3D}.
     */
    public static float interpolate3D(float dx, float dy, float dz,
                                       float a, float b, float c, float d,
                                       float e, float f, float g, float h) {
        float i1 = lerp(dx, a, b);
        float i2 = lerp(dx, c, d);
        float j1 = lerp(dx, e, f);
        float j2 = lerp(dx, g, h);
        float w1 = lerp(dy, i1, i2);
        float w2 = lerp(dy, j1, j2);
        return lerp(dz, w1, w2);
    }

    /**
     * StationAPI coordinate hash - deterministic, stable per-coord.
     * Used as {@code RANDOM.setSeed(...)} and for weather variation selection.
     */
    public static int hashCode(int x, int y, int z) {
        long l = (long) (x * 3129871) ^ (long) z * 116129781L ^ (long) y;
        l = l * l * 42317861L + l * 11L;
        return (int) (l >> 16);
    }

    /**
     * Clamp {@code v} to [{@code min}, {@code max}].
     */
    public static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    /**
     * Clamp {@code v} to [{@code min}, {@code max}].
     */
    public static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    /**
     * Returns true if {@code n} is a positive power of two.
     */
    public static boolean isPowerOfTwo(int n) {
        return n != 0 && (n & (n - 1)) == 0;
    }
}
