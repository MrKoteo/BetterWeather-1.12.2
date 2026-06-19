package paulevs.betterweather.config;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CommonConfig {
	private static final Config CONFIG = new Config(new File("config/better_weather/common.cfg"));

	/** Original Beta 1.7.3 cloud layer height. */
	public static final int CLOUD_HEIGHT_B173 = 108;
	/** 1.12.2 vanilla cloud layer height. */
	public static final int CLOUD_HEIGHT_VANILLA = 128;

	private static boolean useVanillaClouds;
	private static String cloudHeightPreset;
	private static int cloudHeightCustom;
	private static int cloudHeight;
	private static double cloudsSpeed;
	private static boolean eternalRain;
	private static boolean eternalThunder;
	private static boolean frequentRain;
	private static int rodCheckSide;
	private static int lightningChance;
	private static String noRainDimsRaw;
	private static String noThunderDimsRaw;
	private static String eternalRainDimsRaw;
	private static String eternalThunderDimsRaw;
	private static Set<Integer> noRainDims;
	private static Set<Integer> noThunderDims;
	private static Set<Integer> eternalRainDims;
	private static Set<Integer> eternalThunderDims;

	public static void init() {
		CONFIG.addEntry("useVanillaClouds", false,
			"Use vanilla clouds texture as a base map for clouds",
			"Default value is false"
		);
		CONFIG.addEntry("cloudHeightPreset", "vanilla",
			"Cloud layer height preset. One of: b173, vanilla, custom",
			"  b173    = " + CLOUD_HEIGHT_B173 + " (original Beta 1.7.3 cloud height)",
			"  vanilla = " + CLOUD_HEIGHT_VANILLA + " (1.12.2 vanilla cloud height)",
			"  custom  = use cloudHeightCustom below",
			"Affects both cloud rendering AND localized weather sampling, so sim and visuals agree.",
			"Default value is vanilla"
		);
		CONFIG.addEntry("cloudHeightCustom", 160,
			"Custom cloud height in blocks, used only when cloudHeightPreset=custom",
			"Valid range 0..255 (1.12.2 world height). The 1.12.2 world is twice as tall as",
			"Beta 1.7.3, so raising this lifts the clouds to feel less low.",
			"Default value is 160"
		);
		CONFIG.addEntry("cloudsSpeed", 0.001F,
			"Clouds speed in ticks per chunk, larger values will cause clouds move faster",
			"Default value is 0.001"
		);
		CONFIG.addEntry("eternalRain", false,
			"Makes weather in the whole world rain only",
			"Default value is false"
		);
		CONFIG.addEntry("eternalThunder", false,
			"Makes weather in the whole world thunderstorm",
			"Default value is false"
		);
		CONFIG.addEntry("frequentRain", false,
			"Makes rain more frequent instead of vanilla behaviour",
			"Default value is false"
		);
		CONFIG.addEntry("rodCheckSide", 32,
			"Distance in blocks (from the rod block) that will be protected from lightnings",
			"The area is square with center on rod block and radius equal to this number",
			"Max value is " + Short.MAX_VALUE + " and min is 0",
			"Default value is 32"
		);
		CONFIG.addEntry("lightningChance", 300,
			"Chance that lighting will happen in this chunk (during thunderstorm)",
			"The actual chance is calculated as 1/lightningChance",
			"Smaller values will result with more lighting and visa versa",
			"Max value is " + Short.MAX_VALUE + " and min is 1",
			"Default value is 300"
		);
		CONFIG.addEntry("noRainDims", "",
			"Comma-separated dimension IDs where rain is disabled",
			"Example: \"-1,1\" disables rain in Nether and End",
			"Default value is \"\" (no dimensions disabled)"
		);
		CONFIG.addEntry("noThunderDims", "",
			"Comma-separated dimension IDs where thunder is disabled",
			"Example: \"-1,1\" disables thunder in Nether and End",
			"Default value is \"\" (no dimensions disabled)"
		);
		CONFIG.addEntry("eternalRainDims", "",
			"Comma-separated dimension IDs where rain is always active",
			"Example: \"0\" makes the Overworld always rainy",
			"Default value is \"\" (no dimensions set to eternal rain)"
		);
		CONFIG.addEntry("eternalThunderDims", "",
			"Comma-separated dimension IDs where thunder is always active",
			"Example: \"0\" makes the Overworld always thundering",
			"Default value is \"\" (no dimensions set to eternal thunder)"
		);
		CONFIG.save();

		useVanillaClouds = CONFIG.getBool("useVanillaClouds");
		cloudHeightPreset = normalizePreset(CONFIG.getString("cloudHeightPreset"));
		cloudHeightCustom = clampHeight(CONFIG.getInt("cloudHeightCustom"));
		cloudHeight = resolveCloudHeight(cloudHeightPreset, cloudHeightCustom);
		cloudsSpeed = CONFIG.getFloat("cloudsSpeed");
		eternalRain = CONFIG.getBool("eternalRain");
		eternalThunder = CONFIG.getBool("eternalThunder");
		frequentRain = CONFIG.getBool("frequentRain");
		rodCheckSide = CONFIG.getInt("rodCheckSide");
		lightningChance = CONFIG.getInt("lightningChance");
		noRainDimsRaw = CONFIG.getString("noRainDims");
		noThunderDimsRaw = CONFIG.getString("noThunderDims");
		eternalRainDimsRaw = CONFIG.getString("eternalRainDims");
		eternalThunderDimsRaw = CONFIG.getString("eternalThunderDims");

		if (rodCheckSide > Short.MAX_VALUE) rodCheckSide = Short.MAX_VALUE;
		if (rodCheckSide < 0) rodCheckSide = 0;

		if (lightningChance > Short.MAX_VALUE) lightningChance = Short.MAX_VALUE;
		if (lightningChance < 1) rodCheckSide = 1;

		noRainDims = parseDimSet(noRainDimsRaw);
		noThunderDims = parseDimSet(noThunderDimsRaw);
		eternalRainDims = parseDimSet(eternalRainDimsRaw);
		eternalThunderDims = parseDimSet(eternalThunderDimsRaw);
	}

	public static boolean useVanillaClouds() {
		return useVanillaClouds;
	}

	/**
	 * Resolved cloud layer height (blocks) from the preset/custom config. Used by both the
	 * renderers and the localized-weather sampling so visuals and simulation always agree.
	 */
	public static int getCloudHeight() {
		return cloudHeight;
	}

	/** Active preset: {@code "b173"}, {@code "vanilla"} or {@code "custom"}. */
	public static String getCloudHeightPreset() {
		return cloudHeightPreset;
	}

	/** Custom height value (only applied when the preset is {@code "custom"}). */
	public static int getCloudHeightCustom() {
		return cloudHeightCustom;
	}

	/** Live-set the preset, recompute the active height, and persist. */
	public static void setCloudHeightPreset(String preset) {
		cloudHeightPreset = normalizePreset(preset);
		cloudHeight = resolveCloudHeight(cloudHeightPreset, cloudHeightCustom);
		CONFIG.set("cloudHeightPreset", cloudHeightPreset);
		CONFIG.flush();
	}

	/** Live-set the custom height, recompute the active height, and persist. */
	public static void setCloudHeightCustom(int height) {
		cloudHeightCustom = clampHeight(height);
		cloudHeight = resolveCloudHeight(cloudHeightPreset, cloudHeightCustom);
		CONFIG.set("cloudHeightCustom", cloudHeightCustom);
		CONFIG.flush();
	}

	/** Live-set the cloud scroll speed and persist. */
	public static void setCloudsSpeed(double speed) {
		cloudsSpeed = speed;
		CONFIG.set("cloudsSpeed", (float) speed);
		CONFIG.flush();
	}

	private static String normalizePreset(String preset) {
		String key = preset == null ? "" : preset.trim().toLowerCase();
		if (key.equals("b173") || key.equals("custom") || key.equals("vanilla")) {
			return key;
		}
		return "vanilla";
	}

	private static int clampHeight(int height) {
		if (height < 0) return 0;
		if (height > 255) return 255;
		return height;
	}

	private static int resolveCloudHeight(String preset, int custom) {
		switch (normalizePreset(preset)) {
			case "b173":
				return CLOUD_HEIGHT_B173;
			case "custom":
				return clampHeight(custom);
			case "vanilla":
			default:
				return CLOUD_HEIGHT_VANILLA;
		}
	}

	public static double getCloudsSpeed() {
		return cloudsSpeed;
	}

	public static boolean isEternalRain() {
		return eternalRain;
	}

	public static boolean isEternalThunder() {
		return eternalThunder;
	}

	public static boolean isFrequentRain() {
		return frequentRain;
	}

	public static short getRodCheckSide() {
		return (short) rodCheckSide;
	}

	public static int getLightningChance() {
		return lightningChance;
	}

	/**
	 * Get the set of dimension IDs where rain is disabled.
	 * @return unmodifiable {@link Set} of dimension IDs
	 */
	public static Set<Integer> getNoRainDims() {
		return noRainDims;
	}

	/**
	 * Get the set of dimension IDs where thunder is disabled.
	 * @return unmodifiable {@link Set} of dimension IDs
	 */
	public static Set<Integer> getNoThunderDims() {
		return noThunderDims;
	}

	/**
	 * Get the set of dimension IDs where rain is always active.
	 * @return unmodifiable {@link Set} of dimension IDs
	 */
	public static Set<Integer> getEternalRainDims() {
		return eternalRainDims;
	}

	/**
	 * Get the set of dimension IDs where thunder is always active.
	 * @return unmodifiable {@link Set} of dimension IDs
	 */
	public static Set<Integer> getEternalThunderDims() {
		return eternalThunderDims;
	}

	/**
	 * Parse a comma-separated string of dimension IDs into a set of integers.
	 * Blank entries and whitespace are ignored.
	 * @param raw comma-separated dimension IDs, may be empty
	 * @return unmodifiable {@link Set} of parsed dimension IDs, never null
	 */
	private static Set<Integer> parseDimSet(String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return Collections.emptySet();
		}
		Set<Integer> set = new HashSet<>();
		for (String part : raw.split(",")) {
			String trimmed = part.trim();
			if (trimmed.isEmpty()) continue;
			try {
				set.add(Integer.parseInt(trimmed));
			} catch (NumberFormatException ignored) {
				// skip malformed entries
			}
		}
		return Collections.unmodifiableSet(set);
	}
}
