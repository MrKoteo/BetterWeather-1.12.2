package paulevs.betterweather.client.rendering;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
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

import java.util.Random;

@SideOnly(Side.CLIENT)
public class WeatherRenderer {
	private static final float TO_RADIANS = (float) (Math.PI / 180);
	private static final ResourceLocation RAIN_TEXTURE = new ResourceLocation("textures/environment/rain.png");
	private static final ResourceLocation SNOW_TEXTURE = new ResourceLocation("textures/environment/snow.png");
	private static final ResourceLocation WATER_CIRCLES_TEXTURE = new ResourceLocation("better_weather", "textures/water_circles.png");

	private final float[] randomOffset;
	private final byte[] randomIndex;
	// Reused across all per-column lookups on the (single-threaded) render path to avoid
	// allocating thousands of BlockPos per frame - that GC churn was pushing frametime past
	// the vsync step and halving FPS during rain.
	private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
	// Blue-channel multiplier for rain verts, refreshed per frame from config.
	// 1.0 = "b1.7.3 rain color" (original Beta 1.7.3 light-blue hue); <1 = modern neutral tint.
	// Placeholder modern value, tunable when the modern look is designed.
	private static final float MODERN_RAIN_BLUE = 0.85F;
	private float rainBlueScale = 1F;

	public WeatherRenderer() {
		randomOffset = new float[256];
		randomIndex = new byte[256];
		Random random = new Random(0);
		for (short i = 0; i < 256; i++) {
			randomOffset[i] = random.nextFloat();
			randomIndex[i] = (byte) random.nextInt(4);
		}
	}

	public void update(TextureManager manager) {
		// Textures are bound via ResourceLocation in 1.12.2 - no pre-fetch needed.
	}

	public void render(float delta, Minecraft minecraft) {
		Entity entity = minecraft.getRenderViewEntity();
		double x = BWMath.lerp(delta, (float) entity.lastTickPosX, (float) entity.posX);
		double y = BWMath.lerp(delta, (float) entity.lastTickPosY, (float) entity.posY);
		double z = BWMath.lerp(delta, (float) entity.lastTickPosZ, (float) entity.posZ);

		int ix = MathHelper.floor(entity.posX);
		int iy = MathHelper.floor(entity.posY);
		int iz = MathHelper.floor(entity.posZ);

		int radius = minecraft.gameSettings.fancyGraphics ? 10 : 5;
		int radiusCenter = radius / 2 - 1;
		float sampleHeight = CommonConfig.useVanillaClouds() ? 2.5F : 8.5F;
		World level = minecraft.world;
		int rainTop = (int) (WeatherAPI.getCloudHeight(level) + sampleHeight);

		if (iy - rainTop > 40) return;

		rainBlueScale = ClientConfig.betaRainColor() ? 1F : MODERN_RAIN_BLUE;

		float vOffset = (float) (((double) level.getTotalWorldTime() + delta) * 0.05 % 1.0);
		Vec3d pos = getPosition(entity);
		Vec3d dir = getViewDirection(entity);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();

		GlStateManager.disableCull();
		GL11.glNormal3f(0.0F, 1.0F, 0.0F);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01F);
		// POSITION_TEX_COLOR rain quads carry no normals and no lightmap coords. Without
		// disabling GL_LIGHTING + the lightmap unit (GL_TEXTURE1, left bound by terrain), the
		// quads get modulated to black - same trap the clouds hit (CLOUD_RENDER_NOTES #3).
		// Light is already baked into the vertex color via getLightBrightness below.
		GlStateManager.disableLighting();
		GlStateManager.enableTexture2D();
		minecraft.entityRenderer.disableLightmap();
		GlStateManager.color(1F, 1F, 1F, 1F);

		TextureManager textureManager = minecraft.getTextureManager();
		textureManager.bindTexture(RAIN_TEXTURE);

		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.setTranslation(-x, -y, -z);

		for (byte dx = (byte) -radius; dx <= radius; dx++) {
			int wx = (ix & -4) + (dx << 2);
			for (byte dz = (byte) -radius; dz <= radius; dz++) {
				if (Math.abs(dx) < radiusCenter && Math.abs(dz) < radiusCenter) continue;
				int wz = (iz & -4) + (dz << 2);
				renderLargeSection(level, wx, iy, wz, pos, dir, rainTop, buffer, vOffset, false);
			}
		}

		for (byte dx = (byte) -radius; dx <= radius; dx++) {
			int wx = ix + dx;
			for (byte dz = (byte) -radius; dz <= radius; dz++) {
				int wz = iz + dz;
				renderNormalSection(level, wx, iy, wz, pos, dir, rainTop, buffer, vOffset, false);
			}
		}

		tessellator.draw();

		textureManager.bindTexture(WATER_CIRCLES_TEXTURE);
		vOffset = (float) (((double) level.getTotalWorldTime() + delta) * 0.07 % 1.0);

		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

		for (byte dx = (byte) -radius; dx <= radius; dx++) {
			int wx = ix + dx;
			for (byte dz = (byte) -radius; dz <= radius; dz++) {
				int wz = iz + dz;
				renderWaterCircles(level, wx, iy, wz, pos, dir, buffer, vOffset, radius);
			}
		}

		tessellator.draw();

		textureManager.bindTexture(SNOW_TEXTURE);
		vOffset = (float) (((double) level.getTotalWorldTime() + delta) * 0.002 % 1.0);

		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

		for (byte dx = (byte) -radius; dx <= radius; dx++) {
			int wx = (ix & -4) + (dx << 2);
			for (byte dz = (byte) -radius; dz <= radius; dz++) {
				if (Math.abs(dx) < radiusCenter && Math.abs(dz) < radiusCenter) continue;
				int wz = (iz & -4) + (dz << 2);
				renderLargeSection(level, wx, iy, wz, pos, dir, rainTop, buffer, vOffset, true);
			}
		}

		for (byte dx = (byte) -radius; dx <= radius; dx++) {
			int wx = ix + dx;
			for (byte dz = (byte) -radius; dz <= radius; dz++) {
				int wz = iz + dz;
				renderNormalSection(level, wx, iy, wz, pos, dir, rainTop, buffer, vOffset, true);
			}
		}

		buffer.setTranslation(0.0, 0.0, 0.0);
		tessellator.draw();

		// End with the lightmap unit DISABLED, matching vanilla renderRainSnow (ends on
		// disableLightmap, EntityRenderer line 1757). Leaving it ENABLED was the F1 "dark
		// patches" bug: GL_TEXTURE1 stayed on, and only renderHand's disableLightmap (line 827,
		// gated on !hideGUI) cleared it - so F1 skipped that and the stale lightmap darkened the
		// next frame's sky/clouds. (The F6 dump missed it: it only probes active unit TEXTURE0.)
		minecraft.entityRenderer.disableLightmap();
		// Do NOT enable GL_LIGHTING here. Vanilla renderRainSnow never touches it - GL_LIGHTING is
		// OFF during world render and must stay off. Enabling it leaked into the next frame when
		// the HUD was hidden (F1): the hand pass that would have cleared it via
		// RenderHelper.disableStandardItemLighting() is skipped, so the normal-less sky/world quads
		// shaded dark. Leave lighting in the OFF state we entered with.
		GlStateManager.enableCull();
		GlStateManager.disableBlend();
		GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
		// Reset the current vertex color to white. The quad draws leave a tinted "current
		// color"; the hand-render pass normally clears it, but with the GUI hidden (F1) that
		// pass is skipped, so the leftover tint dimmed/flickered the view (worse looking down,
		// where more rain columns draw). Reset explicitly so state never leaks past us.
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	private void renderLargeSection(World level, int x, int y, int z, Vec3d pos, Vec3d dir, int rainTop, BufferBuilder buffer, float vOffset, boolean snow) {
		int terrain = WeatherAPI.getRainHeight(level, x, z);
		if (terrain - y > 40) return;

		boolean visible = pointIsVisible(pos, dir, x + 0.5, terrain, z + 0.5);
		visible |= pointIsVisible(pos, dir, x + 0.5, y, z + 0.5);
		visible |= pointIsVisible(pos, dir, x + 0.5, rainTop, z + 0.5);
		if (!visible) return;

		if (!WeatherAPI.isRaining(level, x, terrain, z)) return;
		if (level.getBiome(scratchPos.setPos(x, 0, z)).getEnableSnow() != snow) return;

		float v1 = randomOffset[(x & 15) << 4 | (z & 15)] + vOffset;
		float v2 = ((rainTop - terrain) * 0.0625F + v1);

		float light = level.getLightBrightness(scratchPos.setPos(x, terrain, z));
		// Rain gets the b1.7.3 blue tint via the texture; snow stays white. Modern mode
		// dampens the blue channel. Snow is never tinted.
		float lightBlue = snow ? light : light * rainBlueScale;
		float alpha = WeatherAPI.sampleFront(level, x, z, 0.1F);
		alpha = BWMath.clamp((alpha - 0.2F) * 2, 0.5F, 1F);

		float u1 = ((x + z) & 3) * 0.25F;
		float u2 = u1 + 0.25F;

		float dx = (float) (pos.x - (x + 0.5));
		float dz = (float) (pos.z - (z + 0.5));
		float l = dx * dx + dz * dz;
		if (l > 0) {
			l = MathHelper.sqrt(l) / 0.5F;
			dx /= l;
			dz /= l;
			float v = dx;
			dx = -dz;
			dz = v;
		}
		else {
			dx = 0.5F;
			dz = 0;
		}

		double x1 = x + 0.5 + dx;
		double x2 = x + 0.5 - dx;
		double z1 = z + 0.5 + dz;
		double z2 = z + 0.5 - dz;

		buffer.pos(x1, terrain, z1).tex(u1, v1).color(light, light, lightBlue, alpha).endVertex();
		buffer.pos(x1, rainTop, z1).tex(u1, v2).color(light, light, lightBlue, alpha).endVertex();
		buffer.pos(x2, rainTop, z2).tex(u2, v2).color(light, light, lightBlue, alpha).endVertex();
		buffer.pos(x2, terrain, z2).tex(u2, v1).color(light, light, lightBlue, alpha).endVertex();
	}

	private void renderNormalSection(World level, int x, int y, int z, Vec3d pos, Vec3d dir, int rainTop, BufferBuilder buffer, float vOffset, boolean snow) {
		int terrain = WeatherAPI.getRainHeight(level, x, z);
		if (terrain - y > 40) return;

		boolean visible = pointIsVisible(pos, dir, x + 0.5, terrain, z + 0.5);
		visible |= pointIsVisible(pos, dir, x + 0.5, y, z + 0.5);
		visible |= pointIsVisible(pos, dir, x + 0.5, rainTop, z + 0.5);
		if (!visible) return;

		if (!WeatherAPI.isRaining(level, x, terrain, z)) return;
		if (level.getBiome(scratchPos.setPos(x, 0, z)).getEnableSnow() != snow) return;

		float v1 = randomOffset[(x & 15) << 4 | (z & 15)] + vOffset;
		float v2 = (rainTop - terrain) * 0.0625F + v1;

		float light = level.getLightBrightness(scratchPos.setPos(x, terrain, z));
		// Rain gets the b1.7.3 blue tint via the texture; snow stays white. Modern mode
		// dampens the blue channel. Snow is never tinted.
		float lightBlue = snow ? light : light * rainBlueScale;
		float alpha = WeatherAPI.sampleFront(level, x, z, 0.1F);
		alpha = BWMath.clamp((alpha - 0.2F) * 2, 0.5F, 1F);

		float u1 = ((x + z) & 3) * 0.25F;
		float u2 = u1 + 0.25F;

		float dx = (float) (pos.x - (x + 0.5));
		float dz = (float) (pos.z - (z + 0.5));
		float l = dx * dx + dz * dz;
		if (l > 0) {
			l = MathHelper.sqrt(l) / 0.5F;
			dx /= l;
			dz /= l;
			float v = dx;
			dx = -dz;
			dz = v;
		}
		else {
			dx = 0.5F;
			dz = 0;
		}

		double x1 = x + 0.5 + dx;
		double x2 = x + 0.5 - dx;
		double z1 = z + 0.5 + dz;
		double z2 = z + 0.5 - dz;

		buffer.pos(x1, terrain, z1).tex(u1, v1).color(light, light, lightBlue, alpha).endVertex();
		buffer.pos(x1, rainTop, z1).tex(u1, v2).color(light, light, lightBlue, alpha).endVertex();
		buffer.pos(x2, rainTop, z2).tex(u2, v2).color(light, light, lightBlue, alpha).endVertex();
		buffer.pos(x2, terrain, z2).tex(u2, v1).color(light, light, lightBlue, alpha).endVertex();
	}

	private void renderWaterCircles(World level, int x, int y, int z, Vec3d pos, Vec3d dir, BufferBuilder buffer, float vOffset, float radius) {
		int height = level.getHeight(x, z);
		if (height - y > 40 || y - height > 40) return;
		if (!pointIsVisible(pos, dir, x + 0.5, height, z + 0.5)) return;
		if (level.getBlockState(scratchPos.setPos(x, height - 1, z)).getMaterial() != Material.WATER) return;
		if (!WeatherAPI.isRaining(level, x, height, z)) return;

		float dx = (float) (x - pos.x);
		float dy = (float) (y - pos.y);
		float dz = (float) (z - pos.z);
		float alpha = 1F - MathHelper.sqrt(dx * dx + dy * dy + dz * dz) / radius;
		alpha = alpha * 4F;
		if (alpha <= 0.01F) return;
		if (alpha > 1F) alpha = 1F;

		float light = level.getLightBrightness(scratchPos.setPos(x, height, z));

		float u1 = 0;
		float u2 = 1;
		vOffset += randomOffset[(x & 15) << 4 | (z & 15)];
		float v1 = MathHelper.floor(vOffset * 6F) / 6F;
		float v2 = v1 + 1F / 6F;

		byte index = randomIndex[(x & 15) << 4 | (z & 15)];
		if ((index & 1) == 0) {
			u2 = 0;
			u1 = 1;
		}
		if (index > 1) {
			float value = v1;
			v1 = v2;
			v2 = value;
		}

		buffer.pos(x, height, z).tex(u1, v1).color(light, light, light, alpha).endVertex();
		buffer.pos(x, height, z + 1).tex(u1, v2).color(light, light, light, alpha).endVertex();
		buffer.pos(x + 1, height, z + 1).tex(u2, v2).color(light, light, light, alpha).endVertex();
		buffer.pos(x + 1, height, z).tex(u2, v1).color(light, light, light, alpha).endVertex();
	}

	private Vec3d getPosition(Entity entity) {
		return new Vec3d(entity.posX, entity.posY, entity.posZ);
	}

	private Vec3d getViewDirection(Entity entity) {
		float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw);
		float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch);

		yaw = -yaw * TO_RADIANS - (float) Math.PI;
		float cosYaw = MathHelper.cos(yaw);
		float sinYaw = MathHelper.sin(yaw);
		float cosPitch = -MathHelper.cos(-pitch * TO_RADIANS);

		return new Vec3d(
			sinYaw * cosPitch,
			(MathHelper.sin(-pitch * ((float) Math.PI / 180))),
			cosYaw * cosPitch
		);
	}

	private boolean pointIsVisible(Vec3d position, Vec3d normal, double x, double y, double z) {
		return normal.x * (x - position.x) + normal.y * (y - position.y) + normal.z * (z - position.z) > 0;
	}
}
