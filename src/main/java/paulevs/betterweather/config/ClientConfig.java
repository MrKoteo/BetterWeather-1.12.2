package paulevs.betterweather.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;

@SideOnly(Side.CLIENT)
public class ClientConfig {
	private static final Config CONFIG = new Config(new File("config/better_weather/client.cfg"));
	private static boolean fluffyClouds;
	private static GameSettings options;
	private static boolean betaRainColor;
	private static int cloudViewDistance;

	public static void init() {
		CONFIG.addEntry("fluffyClouds", true,
			"Render clouds fluffy, if false clouds will use block-like rendering",
			"Default value is true"
		);
		CONFIG.addEntry("cloudViewDistanceChunks", 9,
			"How far clouds render, as a radius in 32-block cloud tiles.",
			"Clouds draw without fog, so higher values let you watch distant rain fronts approach,",
			"beyond the terrain render distance.",
			"Range 4..16 (16 ~= 512 blocks). Higher = more GPU/CPU.",
			"Default value is 9"
		);
		CONFIG.addEntry("b173RainColor", true,
			"Rain color style.",
			"true = \"b1.7.3 rain color\": the original Beta 1.7.3 light-blue rain hue (faithful to the source mod)",
			"false = modern, more neutral rain tint",
			"Default value is true"
		);
		CONFIG.save();

		fluffyClouds = CONFIG.getBool("fluffyClouds");
		betaRainColor = CONFIG.getBool("b173RainColor");
		cloudViewDistance = clampViewDistance(CONFIG.getInt("cloudViewDistanceChunks"));
	}

	/**
	 * @return {@code true} for the original "b1.7.3 rain color" (Beta 1.7.3 light-blue rain),
	 *         {@code false} for the modern neutral tint.
	 */
	public static boolean betaRainColor() {
		return betaRainColor;
	}

	/** Raw config flag (ignores the fancy-graphics gate that {@link #renderFluffy()} applies). */
	public static boolean isFluffyClouds() {
		return fluffyClouds;
	}

	/** Live-set fluffy clouds and persist. Caller should rebuild clouds (geometry changes). */
	public static void setFluffyClouds(boolean value) {
		fluffyClouds = value;
		CONFIG.set("fluffyClouds", value);
		CONFIG.flush();
	}

	/** Live-set the rain colour style and persist. Applied next frame (read per-frame). */
	public static void setBetaRainColor(boolean value) {
		betaRainColor = value;
		CONFIG.set("b173RainColor", value);
		CONFIG.flush();
	}

	/** Cloud render radius in 32-block tiles (4..16). */
	public static int getCloudViewDistance() {
		return cloudViewDistance;
	}

	/** Live-set the cloud render radius and persist. Caller applies it to the renderer. */
	public static void setCloudViewDistance(int chunks) {
		cloudViewDistance = clampViewDistance(chunks);
		CONFIG.set("cloudViewDistanceChunks", cloudViewDistance);
		CONFIG.flush();
	}

	private static int clampViewDistance(int chunks) {
		if (chunks < 4) return 4;
		if (chunks > 16) return 16;
		return chunks;
	}

	@SuppressWarnings("deprecation")
	public static boolean renderFluffy() {
		if (options == null) {
			options = Minecraft.getMinecraft().gameSettings;
		}
		return fluffyClouds && options.fancyGraphics;
	}

}
