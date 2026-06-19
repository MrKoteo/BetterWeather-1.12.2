package paulevs.betterweather.client.rendering;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import paulevs.betterweather.config.ClientConfig;
import paulevs.betterweather.util.BWMath;
import paulevs.betterweather.util.BWVecMath.Vec3f;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class CloudChunk {
	private static final float[] RAIN_COLOR = new float[] { 66F / 255F, 74F / 255F, 74F / 255F };
	private static final float[] DARK_COLOR = new float[] { 150F / 255F, 176F / 255F, 211F / 255F };
	private static final Random RANDOM = new Random(0);
	private static final Vec3f POS = new Vec3f();

	private static final int STRIDE = DefaultVertexFormats.POSITION_TEX_COLOR.getSize();

	private final VertexBuffer vertexBuffer;

	private boolean needUpdate = true;
	private boolean isEmpty = true;
	private int chunkX = Integer.MIN_VALUE;
	private int chunkZ = Integer.MIN_VALUE;
	private int posX;
	private int posZ;

	public CloudChunk() {
		// 1.12.2's Tessellator draws via vertex-array uploads, which a GL display list does
		// NOT reliably capture (it baked garbage -> speckle/jagged/rubberbanding). Use a VBO,
		// which is the engine's own persistent geometry path.
		vertexBuffer = new VertexBuffer(DefaultVertexFormats.POSITION_TEX_COLOR);
	}

	public void checkIfNeedUpdate(int x, int z) {
		needUpdate = chunkX != x || chunkZ != z;
	}

	public void forceUpdate() {
		chunkX = Integer.MIN_VALUE;
		chunkZ = Integer.MIN_VALUE;
		isEmpty = true;
	}

	public boolean needUpdate() {
		return needUpdate;
	}

	public void setRenderPosition(int chunkX, int chunkZ) {
		this.posX = chunkX << 5;
		this.posZ = chunkZ << 5;
	}

	public void update(int chunkX, int chunkZ, short[] data) {
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		isEmpty = true;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

		for (short i = 0; i < 8192; i++) {
			if (data[i] == CloudRenderer.EMPTY_CLOUD) continue;

			byte x = (byte) (i & 15);
			byte y = (byte) ((i >> 4) & 31);
			byte z = (byte) (i >> 9);

			boolean canDraw = x == 0 || x == 15 || y == 0 || y == 31 || z == 0 || z == 15;
			if (!canDraw) {
				canDraw = data[i + 1] == CloudRenderer.EMPTY_CLOUD || data[i - 1] == CloudRenderer.EMPTY_CLOUD ||
					data[i + 16] == CloudRenderer.EMPTY_CLOUD || data[i - 16] == CloudRenderer.EMPTY_CLOUD ||
					data[i + 512] == CloudRenderer.EMPTY_CLOUD || data[i - 512] == CloudRenderer.EMPTY_CLOUD;
			}

			if (!canDraw) continue;

			RANDOM.setSeed(BWMath.hashCode(x, y, z));
			float deltaBrightness = ((data[i] & 15) + RANDOM.nextFloat()) / 15F;
			float deltaWetness = (((data[i] >> 4) & 15) + RANDOM.nextFloat()) / 15F;
			float deltaThunder = ((data[i] >> 8) & 15) / 15F;
			deltaBrightness *= (1 - deltaWetness) * 0.5F + 0.5F;
			deltaThunder = BWMath.lerp(deltaThunder, 1F, 0.5F);

			float r = BWMath.lerp(deltaWetness, RAIN_COLOR[0], DARK_COLOR[0]);
			float g = BWMath.lerp(deltaWetness, RAIN_COLOR[1], DARK_COLOR[1]);
			float b = BWMath.lerp(deltaWetness, RAIN_COLOR[2], DARK_COLOR[2]);
			r = BWMath.lerp(deltaBrightness, r, 1F) * deltaThunder;
			g = BWMath.lerp(deltaBrightness, g, 1F) * deltaThunder;
			b = BWMath.lerp(deltaBrightness, b, 1F) * deltaThunder;

			// deltaBrightness can exceed 1, pushing the colour above 1.0. StationAPI's
			// Tessellator.color clamped; 1.12.2 BufferBuilder.color does (int)(c*255) with NO
			// clamp, so >1.0 overflows the byte and wraps to near-0 - the bright cells turned
			// into black/cyan/blue speckle. Clamp to [0,1] before emitting.
			r = BWMath.clamp(r, 0F, 1F);
			g = BWMath.clamp(g, 0F, 1F);
			b = BWMath.clamp(b, 0F, 1F);

			if (ClientConfig.renderFluffy()) {
				makeFluffyCloudBlock(buffer, x, y, z, r, g, b);
			}
			else {
				makeNormalCloudBlock(buffer, x, y, z, data, i, r, g, b);
			}
			isEmpty = false;
		}

		buffer.finishDrawing();
		vertexBuffer.bufferData(buffer.getByteBuffer());
	}

	public void render(double entityX, double entityZ, float height, FrustumCulling culling, float distanceSqr) {
		if (isEmpty) return;
		float dx = (float) (posX - entityX);
		float dz = (float) (posZ - entityZ);
		POS.set(dx + 16, height + 16, dz + 16);
		if (POS.getX() * POS.getX() + POS.getZ() * POS.getZ() > distanceSqr) return;
		if (culling.isOutside(POS, 24)) return;
		GL11.glPushMatrix();
		GL11.glTranslatef(dx, height, dz);
		GL11.glScalef(2, 2, 2);

		vertexBuffer.bindBuffer();
		GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GlStateManager.glVertexPointer(3, GL11.GL_FLOAT, STRIDE, 0);
		GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GlStateManager.glTexCoordPointer(2, GL11.GL_FLOAT, STRIDE, 12);
		GlStateManager.glEnableClientState(GL11.GL_COLOR_ARRAY);
		GlStateManager.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, STRIDE, 20);
		vertexBuffer.drawArrays(GL11.GL_QUADS);
		vertexBuffer.unbindBuffer();
		GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);
		GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GlStateManager.glDisableClientState(GL11.GL_COLOR_ARRAY);

		GL11.glPopMatrix();
	}

	private void makeFluffyCloudBlock(BufferBuilder buffer, int x, int y, int z, float cr, float cg, float cb) {
		float px = x + RANDOM.nextFloat() * 0.1F - 0.05F;
		float py = y + RANDOM.nextFloat() * 0.1F - 0.05F;
		float pz = z + RANDOM.nextFloat() * 0.1F - 0.05F;

		buffer.pos(px - 0.207107F, py + 1.207107F, pz + 0.5F).tex(0.0F, 0.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px + 1.207107F, py - 0.207107F, pz + 0.5F).tex(1.0F, 0.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px + 1.207107F, py - 0.207107F, pz - 1.5F).tex(1.0F, 1.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px - 0.207107F, py + 1.207107F, pz - 1.5F).tex(0.0F, 1.0F).color(cr, cg, cb, 1.0F).endVertex();

		buffer.pos(px + 1.207107F, py + 1.207107F, pz + 0.5F).tex(0.0F, 0.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px - 0.207107F, py - 0.207107F, pz + 0.5F).tex(1.0F, 0.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px - 0.207107F, py - 0.207107F, pz - 1.5F).tex(1.0F, 1.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px + 1.207107F, py + 1.207107F, pz - 1.5F).tex(0.0F, 1.0F).color(cr, cg, cb, 1.0F).endVertex();

		buffer.pos(px + 1.5F, py + 1.207107F, pz + 0.207107F).tex(0.0F, 0.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px + 1.5F, py - 0.207107F, pz - 1.207107F).tex(1.0F, 0.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px - 0.5F, py - 0.207107F, pz - 1.207107F).tex(1.0F, 1.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px - 0.5F, py + 1.207107F, pz + 0.207107F).tex(0.0F, 1.0F).color(cr, cg, cb, 1.0F).endVertex();

		buffer.pos(px + 1.5F, py + 1.207107F, pz - 1.207107F).tex(0.0F, 0.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px + 1.5F, py - 0.207107F, pz + 0.207107F).tex(1.0F, 0.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px - 0.5F, py - 0.207107F, pz + 0.207107F).tex(1.0F, 1.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px - 0.5F, py + 1.207107F, pz - 1.207107F).tex(0.0F, 1.0F).color(cr, cg, cb, 1.0F).endVertex();

		buffer.pos(px + 1.207107F, py - 0.5F, pz + 0.207107F).tex(0.0F, 0.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px - 0.207107F, py - 0.5F, pz - 1.207107F).tex(1.0F, 0.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px - 0.207107F, py + 1.5F, pz - 1.207107F).tex(1.0F, 1.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px + 1.207107F, py + 1.5F, pz + 0.207107F).tex(0.0F, 1.0F).color(cr, cg, cb, 1.0F).endVertex();

		buffer.pos(px + 1.207107F, py - 0.5F, pz - 1.207107F).tex(0.0F, 0.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px - 0.207107F, py - 0.5F, pz + 0.207107F).tex(1.0F, 0.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px - 0.207107F, py + 1.5F, pz + 0.207107F).tex(1.0F, 1.0F).color(cr, cg, cb, 1.0F).endVertex();
		buffer.pos(px + 1.207107F, py + 1.5F, pz - 1.207107F).tex(0.0F, 1.0F).color(cr, cg, cb, 1.0F).endVertex();
	}

	private void makeNormalCloudBlock(BufferBuilder buffer, int x, int y, int z, short[] data, int index, float cr, float cg, float cb) {
		if (x == 0 || data[index - 1] == CloudRenderer.EMPTY_CLOUD) {
			buffer.pos(x, y, z).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x, y + 1, z).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x, y + 1, z + 1).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x, y, z + 1).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
		}
		if (x == 15 || data[index + 1] == CloudRenderer.EMPTY_CLOUD) {
			buffer.pos(x + 1, y, z).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x + 1, y + 1, z).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x + 1, y + 1, z + 1).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x + 1, y, z + 1).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
		}

		if (y == 0 || data[index - 16] == CloudRenderer.EMPTY_CLOUD) {
			buffer.pos(x, y, z).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x + 1, y, z).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x + 1, y, z + 1).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x, y, z + 1).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
		}
		if (y == 31 || data[index + 16] == CloudRenderer.EMPTY_CLOUD) {
			buffer.pos(x, y + 1, z).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x + 1, y + 1, z).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x + 1, y + 1, z + 1).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x, y + 1, z + 1).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
		}

		if (z == 0 || data[index - 512] == CloudRenderer.EMPTY_CLOUD) {
			buffer.pos(x, y, z).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x, y + 1, z).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x + 1, y + 1, z).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x + 1, y, z).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
		}
		if (z == 15 || data[index + 512] == CloudRenderer.EMPTY_CLOUD) {
			buffer.pos(x, y, z + 1).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x, y + 1, z + 1).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x + 1, y + 1, z + 1).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
			buffer.pos(x + 1, y, z + 1).tex(0.5F, 0.5F).color(cr, cg, cb, 1.0F).endVertex();
		}
	}
}
