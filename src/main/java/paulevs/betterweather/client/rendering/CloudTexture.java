package paulevs.betterweather.client.rendering;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Cloud texture: the {@code cloud.png} ALPHA mask (soft blob shape) combined with a flat RGB
 * that is re-tinted to the current cloud colour every frame.
 *
 * <p>The previous {@code DynamicTexture} + {@code glGetTexImage} read-back path produced a
 * garbage texture (random speckle). This builds an explicit RGBA byte buffer instead - RGB
 * is the cloud tint, alpha is the sprite mask - so the colour is clean and the soft edges are
 * preserved (a fully solid texture rendered as hard blocky faces).
 */
@SideOnly(Side.CLIENT)
public class CloudTexture {
	private static final ResourceLocation CLOUD_LOCATION =
		new ResourceLocation("better_weather", "textures/cloud.png");

	private final int textureID;
	private final int width;
	private final int height;
	private final byte[] alpha;
	private final ByteBuffer pixels;

	public CloudTexture(TextureManager manager) {
		BufferedImage image = loadCloudImage();
		width = image.getWidth();
		height = image.getHeight();
		alpha = new byte[width * height];

		int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
		for (int i = 0; i < argb.length; i++) {
			alpha[i] = (byte) ((argb[i] >> 24) & 0xFF);
		}

		textureID = GL11.glGenTextures();
		pixels = ByteBuffer.allocateDirect(width * height * 4);

		GlStateManager.bindTexture(textureID);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
	}

	public void bindAndUpdate(Vec3d color) {
		byte r = (byte) clampByte(color.x);
		byte g = (byte) clampByte(color.y);
		byte b = (byte) clampByte(color.z);

		pixels.clear();
		for (int i = 0; i < alpha.length; i++) {
			pixels.put(r).put(g).put(b).put(alpha[i]);
		}
		pixels.flip();

		GlStateManager.bindTexture(textureID);
		// GL_RGBA + GL_UNSIGNED_BYTE reads bytes R,G,B,A in order - exactly as written above.
		GL11.glTexImage2D(
			GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
			width, height, 0,
			GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels
		);
	}

	private static int clampByte(double v) {
		int i = (int) (v * 255.0);
		if (i < 0) return 0;
		if (i > 255) return 255;
		return i;
	}

	private static BufferedImage loadCloudImage() {
		try {
			InputStream stream = Minecraft.getMinecraft()
				.getResourceManager()
				.getResource(CLOUD_LOCATION)
				.getInputStream();
			try {
				return ImageIO.read(stream);
			} finally {
				stream.close();
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load cloud texture", e);
		}
	}
}
