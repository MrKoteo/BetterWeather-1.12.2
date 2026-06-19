package paulevs.betterweather.client.rendering;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import paulevs.betterweather.api.WeatherAPI;
import paulevs.betterweather.config.CommonConfig;
import paulevs.betterweather.entity.EntityBWLightning;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class BWLightningRenderer {
	private static final Random RANDOM = new Random(0);
	private static final ResourceLocation LIGHTNING_TEXTURE = new ResourceLocation("better_weather", "textures/lightning.png");

	public static void render(EntityBWLightning entity, float x, float y, float z, TextureManager manager) {
		manager.bindTexture(LIGHTNING_TEXTURE);

		float y2 = CommonConfig.useVanillaClouds() ? 2.5F : 8.5F;
		y2 += WeatherAPI.getCloudHeight(entity.world);
		y2 = (float) (y2 - entity.posY) + y;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();

		float dx = x;
		float dz = z;
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

		float x1 = x + 0.5F + dx;
		float x2 = x + 0.5F - dx;
		float z1 = z + 0.5F + dz;
		float z2 = z + 0.5F - dz;
		float x1_2 = x + 0.5F + dx * 0.5F;
		float x2_2 = x + 0.5F - dx * 0.5F;
		float z1_2 = z + 0.5F + dz * 0.5F;
		float z2_2 = z + 0.5F - dz * 0.5F;

		int sectionCount = MathHelper.floor((y2 - y) / 8F + 1);
		float secDelta = (y2 - y) / sectionCount;

		GlStateManager.disableCull();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01F);

		GlStateManager.color(1F, 1F, 1F, 1F);

		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

		float dx1 = 0;
		float dz1 = 0;
		float dx2 = 0;
		float dz2 = 0;

		RANDOM.setSeed(entity.getEntityId());

		for (int i = 0; i < sectionCount; i++) {
			y2 = y + secDelta;

			buffer.pos(x1 + dx1,  y, z1 + dz1).tex(0F, 0F).color(1F, 1F, 1F, 1F).endVertex();
			buffer.pos(x1 + dx2, y2, z1 + dz2).tex(0F, 1F).color(1F, 1F, 1F, 1F).endVertex();
			buffer.pos(x2 + dx2, y2, z2 + dz2).tex(1F, 1F).color(1F, 1F, 1F, 1F).endVertex();
			buffer.pos(x2 + dx1,  y, z2 + dz1).tex(1F, 0F).color(1F, 1F, 1F, 1F).endVertex();

			if (i > 0 && RANDOM.nextInt(3) == 0) {
				float dist = RANDOM.nextFloat() * 15;
				float dx3 = dx * dist;
				float dz3 = dz * dist;

				buffer.pos(x1_2 + dx3,  y, z1_2 + dz3).tex(0F, 0F).color(1F, 1F, 1F, 1F).endVertex();
				buffer.pos(x1_2 + dx2, y2, z1_2 + dz2).tex(0F, 1F).color(1F, 1F, 1F, 1F).endVertex();
				buffer.pos(x2_2 + dx2, y2, z2_2 + dz2).tex(1F, 1F).color(1F, 1F, 1F, 1F).endVertex();
				buffer.pos(x2_2 + dx3,  y, z2_2 + dz3).tex(1F, 0F).color(1F, 1F, 1F, 1F).endVertex();

				if (RANDOM.nextBoolean()) {
					dist = RANDOM.nextFloat() * 15;
					float dx4 = dx * dist;
					float dz4 = dz * dist;
					float y3 = y - secDelta;

					buffer.pos(x1_2 + dx4, y3, z1_2 + dz4).tex(0F, 0F).color(1F, 1F, 1F, 1F).endVertex();
					buffer.pos(x1_2 + dx3,  y, z1_2 + dz3).tex(0F, 1F).color(1F, 1F, 1F, 1F).endVertex();
					buffer.pos(x2_2 + dx3,  y, z2_2 + dz3).tex(1F, 1F).color(1F, 1F, 1F, 1F).endVertex();
					buffer.pos(x2_2 + dx4, y3, z2_2 + dz4).tex(1F, 0F).color(1F, 1F, 1F, 1F).endVertex();
				}
			}

			dx1 = dx2;
			dz1 = dz2;
			float dist = RANDOM.nextFloat() * 7;
			dx2 = dx * dist;
			dz2 = dz * dist;

			y = y2;
		}

		tessellator.draw();

		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
	}
}
