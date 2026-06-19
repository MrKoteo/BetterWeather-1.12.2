package paulevs.betterweather.client.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import paulevs.betterweather.api.WeatherAPI;
import paulevs.betterweather.config.ClientConfig;
import paulevs.betterweather.config.CommonConfig;
import paulevs.betterweather.util.BWMath;
import paulevs.betterweather.util.BWPerlinNoise;
import paulevs.betterweather.util.MathUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

@SideOnly(Side.CLIENT)
public class CloudRenderer {
	public static final short EMPTY_CLOUD = (short) 0xF000;

	private static final BWPerlinNoise NOISE = new BWPerlinNoise(new Random(0));
	private static final short[] CLOUD_DATA = new short[8192];

	private static final int MAX_UPDATES_PER_FRAME = 6;
	// Cloud tiles are allocated once for MAX_RADIUS; viewRadius (live, from config) controls how
	// many actually render. Clouds draw fog-less (vanilla disables fog before our pass), so a
	// larger viewRadius shows distant rain fronts approaching well beyond the terrain render
	// distance, instead of being culled to it.
	private static final int MAX_RADIUS = 16;
	private static final int SIDE = MAX_RADIUS * 2 + 1;
	private static final int CAPACITY = SIDE * SIDE;
	public static float fogDistance;
	private int viewRadius = 9;
	// Client-smoothed cloud-scroll clock; decouples cloud drift from server tick hitches.
	private static double smoothTime = Double.NaN;
	// Debug bisect (/bwtoggle scroll): freeze cloud drift to isolate scroll vs chunk-rebuild
	// as the cause of rubber-banding.
	public static boolean debugFreezeScroll;

	private final CloudChunk[] chunks = new CloudChunk[CAPACITY];
	private final FrustumCulling culling = new FrustumCulling();
	private final int[][] offsets;

	private CloudTexture cloudTexture;

	public CloudRenderer() {
		for (short i = 0; i < chunks.length; i++) {
			chunks[i] = new CloudChunk();
		}

		List<int[]> offsets = new ArrayList<>(CAPACITY);
		for (byte x = -MAX_RADIUS; x <= MAX_RADIUS; x++) {
			for (byte z = -MAX_RADIUS; z <= MAX_RADIUS; z++) {
				offsets.add(new int[] { x, z });
			}
		}
		offsets.sort((v1, v2) -> {
			int d1 = v1[0] * v1[0] + v1[1] * v1[1];
			int d2 = v2[0] * v2[0] + v2[1] * v2[1];
			return Integer.compare(d1, d2);
		});
		this.offsets = offsets.toArray(new int[0][]);
		culling.setFOV((float) Math.toRadians(60F));
		viewRadius = clampRadius(ClientConfig.getCloudViewDistance());
	}

	/** Live-set how many cloud tiles render (radius, in 32-block tiles). Clamped to MAX_RADIUS. */
	public void setViewDistance(int chunks) {
		viewRadius = clampRadius(chunks);
	}

	/** Current cloud cull radius in blocks (viewRadius tiles * 32). */
	public int getViewDistanceBlocks() {
		return viewRadius * 32;
	}

	private static int clampRadius(int r) {
		if (r < 1) return 1;
		if (r > MAX_RADIUS) return MAX_RADIUS;
		return r;
	}

	public void update(TextureManager manager) {
		if (cloudTexture == null) {
			cloudTexture = new CloudTexture(manager);
		}
	}

	private int getIndex(int x, int y) {
		return MathUtil.wrap(x, SIDE) * SIDE + MathUtil.wrap(y, SIDE);
	}

	public void render(float delta, Minecraft minecraft) {
		Entity entity = minecraft.getRenderViewEntity();
		double entityX = BWMath.lerp(delta, (float) entity.lastTickPosX, (float) entity.posX);
		double entityY = BWMath.lerp(delta, (float) entity.lastTickPosY, (float) entity.posY);
		double entityZ = BWMath.lerp(delta, (float) entity.lastTickPosZ, (float) entity.posZ);
		float height = (float) (WeatherAPI.getCloudHeight(minecraft.world) - entityY);

		int centerX = MathHelper.floor(entityX / 32);
		int centerZ = MathHelper.floor(entityZ / 32);

		// Cloud scroll was tied directly to world time, which freezes during server tick
		// hitches (e.g. chunk gen -> "skipping N ticks") then jumps, snapping the clouds
		// ("rubber-banding"). Drive it from a client-smoothed clock that caps how far it can
		// advance per frame: normal play tracks exactly (<1 tick/frame), but a multi-tick
		// jump is spread over frames instead of snapped. A huge delta (teleport / dimension
		// change) snaps to resync.
		double targetTime = (double) minecraft.world.getTotalWorldTime() + delta;
		if (Double.isNaN(smoothTime) || Math.abs(targetTime - smoothTime) > 200.0) {
			smoothTime = targetTime;
		}
		else if (debugFreezeScroll) {
			// hold smoothTime -> clouds stop drifting
		}
		else {
			double diff = targetTime - smoothTime;
			if (diff > 1.0) diff = 1.0;
			else if (diff < -1.0) diff = -1.0;
			smoothTime += diff;
		}

		double moveDelta = smoothTime * CommonConfig.getCloudsSpeed();
		int worldOffset = (int) Math.floor(moveDelta);
		entityZ -= (moveDelta - worldOffset) * 32;

		// POSITION_TEX_COLOR quads carry no normals; if lighting is still enabled from a
		// prior pass the away-facing faces shade to black ("black bits"). Force it off and
		// reset color to white so the vertex colors aren't multiplied by stale GL color.
		GlStateManager.disableCull();
		// Blend on with the cloud sprite's alpha mask gives soft rounded puffs; a fully opaque
		// texture renders hard blocky faces.
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(
			GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
			GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.disableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		// The lightmap (GL_TEXTURE1) is left bound by terrain rendering; without disabling
		// it the clouds get modulated by stale lightmap coords, blackening some faces (tops).
		minecraft.entityRenderer.disableLightmap();

		// Tint with the vanilla cloud colour, not the sky colour: 1.12.2's sky colour is far
		// more saturated than Beta's, which made the clouds read as solid blue blobs. Cloud
		// colour is white-ish and time-of-day aware (white by day, warm at sunset).
		Vec3d skyColor = minecraft.world.getCloudColour(delta);
		cloudTexture.bindAndUpdate(skyColor);

		// Cull frustum must track the real view FOV, not a fixed 60°, or chunks at the
		// screen edge pop in/out at high FOV. Use the wider of vertical/horizontal + margin.
		float fov = minecraft.gameSettings.fovSetting;
		float aspect = (float) minecraft.displayWidth / (float) Math.max(1, minecraft.displayHeight);
		float vHalf = fov * 0.5F;
		float hHalf = (float) Math.toDegrees(Math.atan(Math.tan(Math.toRadians(vHalf)) * aspect));
		culling.setFOV((float) Math.toRadians(Math.max(vHalf, hHalf) + 15F));
		culling.rotate(entity.rotationYaw, entity.rotationPitch);

		// Rebuild budget per frame. Was 1, which made clouds "teleport"/pop in when crossing a
		// 32-block grid boundary dirtied a whole row of chunks: only one rebuilt per frame and
		// un-built chunks render invisible until their turn. offsets are sorted nearest-first,
		// so the closest (most visible) chunks are rebuilt first. A small budget catches up in
		// a few frames without a large single-frame hitch.
		int updateBudget = MAX_UPDATES_PER_FRAME;
		int vr = viewRadius;
		int vrSqr = vr * vr;
		// Hard cull at the view radius (in blocks), decoupled from fog so clouds extend past the
		// terrain render distance. offsets are sorted nearest-first, so once one is beyond the
		// radius every remaining one is too - break.
		float distance = vr * 32F;
		distance *= distance;

		for (int[] offset : offsets) {
			if (offset[0] * offset[0] + offset[1] * offset[1] > vrSqr) break;
			int cx = centerX + offset[0];
			int cz = centerZ + offset[1];
			int movedZ = cz - worldOffset;
			CloudChunk chunk = chunks[getIndex(cx, movedZ)];
			chunk.setRenderPosition(cx, cz);
			chunk.checkIfNeedUpdate(cx, movedZ);
			if (updateBudget > 0 && chunk.needUpdate()) {
				updateData(minecraft.world, cx, movedZ);
				chunk.update(cx, movedZ, CLOUD_DATA);
				updateBudget--;
			}
			if (!chunk.needUpdate()) {
				chunk.render(entityX, entityZ, height, culling, distance);
			}
		}

		// End with the lightmap unit DISABLED, matching vanilla renderRainSnow (which ends on
		// disableLightmap, EntityRenderer line 1757) and the hand pass's own teardown. We
		// disabled it at the top of the pass; leaving it ENABLED here was the F1 "dark patches"
		// bug: enableLightmap leaves GL_TEXTURE1 on, and only EntityRenderer.renderHand's
		// disableLightmap (line 827) turned it back off - that call is gated on !hideGUI, so
		// pressing F1 skipped it and the stale lightmap modulated the next frame's sky/clouds
		// dark (worse looking down). The F6 GL dump never caught it because it only probes the
		// active unit (TEXTURE0); the leak is on TEXTURE1.
		minecraft.entityRenderer.disableLightmap();
		// Do NOT enable GL_LIGHTING here. Vanilla's weather/cloud passes (renderRainSnow,
		// renderClouds) never touch GL_LIGHTING - it is already OFF during world render and must
		// stay off. Enabling it leaked: with the HUD shown the hand pass's
		// RenderHelper.disableStandardItemLighting() turned it back off, but with the HUD hidden
		// (F1) that pass is skipped, so lighting stayed ON into the next frame's sky/world render
		// - whose quads carry no normals and shaded dark (direction-dependent dimming). Leave it
		// in the OFF state we entered with.
		GlStateManager.enableCull();
		// Restore blend OFF and color to white. Leaving blend enabled here leaked: the GUI
		// pass normally resets it every frame, but with the HUD hidden (F1) nothing did, so
		// after cloud geometry drew the screen stayed dimmed/flickering until something else
		// toggled blend. Reset to the world-default state our pass started from.
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	public void updateAll() {
		Arrays.stream(chunks).forEach(CloudChunk::forceUpdate);
	}

	private void updateData(World level, int cx, int cz) {
		final int posX = cx << 4;
		final int posZ = cz << 4;

		IntStream.range(0, 8192).parallel().forEach(index -> {
			int x = index & 15;
			int y = (index >> 4) & 31;
			int z = index >> 9;

			x |= posX;
			z |= posZ;

			float rainFront = WeatherAPI.sampleFront(level, x, z, 0.2);
			float density = WeatherAPI.getCloudDensity(x << 1, y << 1, z << 1, rainFront);
			float coverage = WeatherAPI.getCoverage(rainFront);

			if (density < coverage) {
				CLOUD_DATA[index] = EMPTY_CLOUD;
			}
			else {
				CLOUD_DATA[index] = (short) ((byte) (rainFront * 15) << 4);
				byte thunder = (byte) (WeatherAPI.sampleThunderstorm(level, x, z, 0.1) * rainFront * 15);
				CLOUD_DATA[index] |= (short) (thunder << 8);
			}
		});

		IntStream.range(0, 8192).parallel().forEach(index -> {
			if (CLOUD_DATA[index] == EMPTY_CLOUD) return;

			int x = index & 15;
			int y = (index >> 4) & 31;
			int z = index >> 9;

			x |= cx;
			z |= cz;

			byte light = 15;
			for (byte i = 1; i < 15; i++) {
				if (y + i > 31) break;
				int index2 = index + (i << 4);
				if (CLOUD_DATA[index2] != EMPTY_CLOUD) light--;
			}

			if (light > 0) {
				light = (byte) (light - NOISE.sample(x * 0.3, y * 0.3, z * 0.3));
			}

			CLOUD_DATA[index] |= light;
		});
	}
}
