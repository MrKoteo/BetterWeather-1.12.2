package paulevs.betterweather.util;

import java.util.Random;

/**
 * Minimal 3D Improved Perlin noise for cloud light variation.
 * Seeded with {@code new Random(0)} to match original deterministic output.
 * <p>
 * Deviation: Original Beta 1.7.3 {@code net.minecraft.util.noise.PerlinNoise}
 * uses a specific gradient permutation. This is a standard Improved Perlin
 * implementation producing output in approximately {@code [-1, 1]}.
 */
public class BWPerlinNoise {
	private static final int[] PERM = new int[512];
	private static final double[] GRAD_X = new double[512];
	private static final double[] GRAD_Y = new double[512];
	private static final double[] GRAD_Z = new double[512];

	static {
		Random random = new Random(0);
		int[] p = new int[256];
		for (int i = 0; i < 256; i++) {
			p[i] = i;
		}
		for (int i = 255; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int tmp = p[i];
			p[i] = p[j];
			p[j] = tmp;
		}
		for (int i = 0; i < 256; i++) {
			PERM[i] = p[i];
			PERM[i + 256] = p[i];
		}
		for (int i = 0; i < 512; i++) {
			double theta = PERM[i] * (Math.PI * 2.0 / 256.0);
			double phi = (PERM[(i + 67) & 511]) * (Math.PI / 256.0);
			GRAD_X[i] = Math.sin(theta) * Math.cos(phi);
			GRAD_Y[i] = Math.cos(theta) * Math.cos(phi);
			GRAD_Z[i] = Math.sin(phi);
		}
	}

	public BWPerlinNoise(Random random) {
		// Static table already initialized; seed ignored for determinism.
	}

	public double sample(double x, double y, double z) {
		int xi = (int) Math.floor(x) & 255;
		int yi = (int) Math.floor(y) & 255;
		int zi = (int) Math.floor(z) & 255;

		double xf = x - Math.floor(x);
		double yf = y - Math.floor(y);
		double zf = z - Math.floor(z);

		double u = fade(xf);
		double v = fade(yf);
		double w = fade(zf);

		int aaa = PERM[PERM[PERM[xi] + yi] + zi];
		int aba = PERM[PERM[PERM[xi] + yi + 1] + zi];
		int aab = PERM[PERM[PERM[xi] + yi] + zi + 1];
		int abb = PERM[PERM[PERM[xi] + yi + 1] + zi + 1];
		int baa = PERM[PERM[PERM[xi + 1] + yi] + zi];
		int bba = PERM[PERM[PERM[xi + 1] + yi + 1] + zi];
		int bab = PERM[PERM[PERM[xi + 1] + yi] + zi + 1];
		int bbb = PERM[PERM[PERM[xi + 1] + yi + 1] + zi + 1];

		double x1 = BWMath.lerp((float) u,
			(float) grad(aaa, xf, yf, zf),
			(float) grad(baa, xf - 1, yf, zf));
		double x2 = BWMath.lerp((float) u,
			(float) grad(aba, xf, yf - 1, zf),
			(float) grad(bba, xf - 1, yf - 1, zf));
		double y1 = BWMath.lerp((float) v, (float) x1, (float) x2);

		double x3 = BWMath.lerp((float) u,
			(float) grad(aab, xf, yf, zf - 1),
			(float) grad(bab, xf - 1, yf, zf - 1));
		double x4 = BWMath.lerp((float) u,
			(float) grad(abb, xf, yf - 1, zf - 1),
			(float) grad(bbb, xf - 1, yf - 1, zf - 1));
		double y2 = BWMath.lerp((float) v, (float) x3, (float) x4);

		return BWMath.lerp((float) w, (float) y1, (float) y2);
	}

	private static double grad(int hash, double x, double y, double z) {
		return GRAD_X[hash] * x + GRAD_Y[hash] * y + GRAD_Z[hash] * z;
	}

	private static double fade(double t) {
		return t * t * t * (t * (t * 6 - 15) + 10);
	}
}
