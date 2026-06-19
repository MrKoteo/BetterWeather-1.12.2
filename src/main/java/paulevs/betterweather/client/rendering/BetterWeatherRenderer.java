package paulevs.betterweather.client.rendering;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import paulevs.betterweather.api.WeatherAPI;
import paulevs.betterweather.mixin.client.AccessorEntityRenderer;
import paulevs.betterweather.util.BWMath;

import java.nio.FloatBuffer;

@SideOnly(Side.CLIENT)
public class BetterWeatherRenderer {
	private static final CloudRenderer CLOUD_RENDERER = new CloudRenderer();
	private static final WeatherRenderer WEATHER_RENDERER = new WeatherRenderer();
	private static float fogDistance = 1F;
	private static boolean isInWater;
	private static boolean initialized;
	public static float fogColorR;
	public static float fogColorG;
	public static float fogColorB;
	// Live diagnostic snapshot of the last fog computation, read by /bwdebug.
	public static float debugRainDensity;
	public static float debugInCloud;
	public static float debugFogDistance;
	// Debug bisect toggles (/bwtoggle).
	public static boolean debugSkipClouds;
	public static boolean debugSkipWeather;
	public static boolean debugSkipFog;
	public static boolean debugSkipRainStrength;
	// When true, the cancel-mixins let vanilla run (vanilla clouds / vanilla rain-snow).
	public static boolean debugLetVanillaClouds;
	public static boolean debugLetRainSnow;
	// TEMP instrumentation: set by F6, dumps render-time GL state at the START of our pass
	// (= what the previous frame's world+hand render left). Strip after diagnosis.
	public static boolean dumpRenderEntry;

	private static void dumpGl(String label) {
		FloatBuffer buf = BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(GL11.GL_CURRENT_COLOR, buf);
		float cr = buf.get(0), cg = buf.get(1), cb = buf.get(2), ca = buf.get(3);
		boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		float fogStart = GL11.glGetFloat(GL11.GL_FOG_START);
		float fogEnd = GL11.glGetFloat(GL11.GL_FOG_END);
		System.out.println(String.format(
			"[BWRENDER] %s hideGUI=%s currentColor=(%.2f,%.2f,%.2f,%.2f) lighting=%s fog=%s "
				+ "fogStart=%.1f fogEnd=%.1f blend=%s alphaTest=%s depthTest=%s depthMask=%s tex2D=%s",
			label,
			Minecraft.getMinecraft().gameSettings.hideGUI,
			cr, cg, cb, ca,
			GL11.glIsEnabled(GL11.GL_LIGHTING),
			GL11.glIsEnabled(GL11.GL_FOG),
			fogStart, fogEnd,
			GL11.glIsEnabled(GL11.GL_BLEND),
			GL11.glIsEnabled(GL11.GL_ALPHA_TEST),
			GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
			depthMask,
			GL11.glIsEnabled(GL11.GL_TEXTURE_2D)));
	}

	public static void update(TextureManager manager) {
		CLOUD_RENDERER.update(manager);
		WEATHER_RENDERER.update(manager);
	}

	public static void renderBeforeWater(float delta, Minecraft minecraft) {
		if (minecraft.world.provider.doesWaterVaporize()) return;
		isInWater = minecraft.getRenderViewEntity().isInsideOfMaterial(Material.WATER);
		if (!isInWater) return;
		render(delta, minecraft);
	}

	public static void renderAfterWater(float delta, Minecraft minecraft) {
		if (minecraft.world.provider.doesWaterVaporize()) return;
		if (isInWater) return;
		render(delta, minecraft);
	}

	private static void render(float delta, Minecraft minecraft) {
		if (!initialized) {
			update(minecraft.getTextureManager());
			initialized = true;
		}
		if (dumpRenderEntry) dumpGl("ENTRY");
		if (!debugSkipClouds) renderCloudsExtendedFar(delta, minecraft);
		if (!debugSkipWeather) WEATHER_RENDERER.render(delta, minecraft);
		if (dumpRenderEntry) {
			dumpGl("EXIT ");
			dumpRenderEntry = false;
		}

		// The cloud VBO / weather Tessellator draws set GL's current color via the per-vertex
		// color attribute (last vertex = a dim value). GlStateManager never sees that, so its
		// cache still believes the color is white and any plain color(1,1,1,1) reset is a no-op
		// cache hit - the dark current-color then leaks into the NEXT frame's sky pass (sky uses
		// the current color; terrain uses the lightmap, which is why only the sky went black).
		// The hand-render pass normally busts this, but it is skipped when the GUI is hidden (F1).
		// resetColor() invalidates the cache so the following color() actually reaches GL.
		GlStateManager.resetColor();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/**
	 * Draw clouds with a far clip plane large enough to reach the cloud view distance. The world
	 * projection's far plane is sized to the terrain render distance, so distant clouds otherwise
	 * z-clip ("disappear into the fog"). Only widens the plane when clouds exceed the world far - 	 * at default view distance the world projection is used unchanged.
	 */
	private static void renderCloudsExtendedFar(float delta, Minecraft minecraft) {
		float cloudFar = CLOUD_RENDERER.getViewDistanceBlocks() + 64F;
		AccessorEntityRenderer acc = (AccessorEntityRenderer) minecraft.entityRenderer;
		float worldFar = acc.bw_getFarPlaneDistance() * MathHelper.SQRT_2;
		if (cloudFar <= worldFar) {
			CLOUD_RENDERER.render(delta, minecraft);
			return;
		}
		float fov = acc.bw_invokeGetFOVModifier(delta, true);
		float aspect = (float) minecraft.displayWidth / (float) minecraft.displayHeight;

		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.pushMatrix();
		GlStateManager.loadIdentity();
		Project.gluPerspective(fov, aspect, 0.05F, cloudFar);
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);

		CLOUD_RENDERER.render(delta, minecraft);

		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.popMatrix();
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
	}

	public static void changeFogDensity(float[] color, Minecraft mc) {
		Entity entity = mc.getRenderViewEntity();
		float density = WeatherAPI.getRainDensity(
			mc.world,
			entity.posX,
			entity.posY,
			entity.posZ,
			true
		);
		if (density == 0) return;
		density = 1F - density * 0.75F;
		color[0] *= density;
		color[1] *= density;
		color[2] *= density;
	}

	public static void updateFogColor(Minecraft minecraft, float delta) {
		if (debugSkipFog) return;
		fogDistance = 1F;

		Entity entity = minecraft.getRenderViewEntity();
		float rainDensity = WeatherAPI.getRainDensity(
			minecraft.world,
			entity.posX,
			entity.posY,
			entity.posZ,
			true
		);
		fogDistance = 1F - rainDensity * 0.75F;

		fogColorR *= fogDistance;
		fogColorG *= fogDistance;
		fogColorB *= fogDistance;

		float inCloud = WeatherAPI.inCloud(
			minecraft.world,
			entity.posX,
			entity.posY,
			entity.posZ
		);

		debugRainDensity = rainDensity;
		debugInCloud = inCloud;
		debugFogDistance = fogDistance;

		if (inCloud > 0) {
			Vec3d fogColor = minecraft.world.getSkyColor(entity, delta);
			fogDistance = BWMath.lerp(inCloud, fogDistance, 0.02F);
			fogColorR = BWMath.lerp(inCloud, fogColorR, (float) fogColor.x);
			fogColorG = BWMath.lerp(inCloud, fogColorG, (float) fogColor.y);
			fogColorB = BWMath.lerp(inCloud, fogColorB, (float) fogColor.z);
		}
	}

	public static void updateFogDepth(float defaultFogDistance) {
		CloudRenderer.fogDistance = defaultFogDistance;
		if (debugSkipFog) return;
		// Exact "== 1" almost never holds after float math (tiny rain-density residue or
		// inCloud-lerp leftover), so the compressed cloud fog kept re-applying every frame
		// and the white haze never cleared. Treat anything near 1 as "no weather fog" and
		// leave vanilla's fog (already set this frame before RenderFogEvent) untouched.
		if (fogDistance >= 0.999F) return;
		// Use GlStateManager, NOT raw GL11.glFogf. GlStateManager caches fog start/end and
		// skips the GL call when the value looks unchanged. Writing raw GL behind its back
		// left the cache stale: on exiting a cloud, vanilla's setFogStart(vanillaValue) saw
		// "unchanged" vs its cache and never restored GL, so our short cloud fog stuck until
		// something forced a real change (re-entering the cloud Y). Routing through
		// GlStateManager keeps the cache coherent so vanilla corrects it the next frame.
		GlStateManager.setFogStart(defaultFogDistance * fogDistance * 0.25F);
		GlStateManager.setFogEnd(defaultFogDistance * fogDistance);
	}

	public static void updateClouds() {
		CLOUD_RENDERER.updateAll();
	}

	public static void setCloudViewDistance(int chunks) {
		CLOUD_RENDERER.setViewDistance(chunks);
	}
}
