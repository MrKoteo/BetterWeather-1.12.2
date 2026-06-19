package paulevs.betterweather.client.rendering;

import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import paulevs.betterweather.util.BWVecMath.Quaternion;
import paulevs.betterweather.util.BWVecMath.Vec3f;

@SideOnly(Side.CLIENT)
public class FrustumCulling {
	private static final float TO_RADIANS = (float) (Math.PI / 180.0);
	private static final Vec3f[] NORMALS;
	private final Quaternion rotation = new Quaternion(0.0F, 0.0F, 0.0F, 0.0F);
	private final Quaternion rotation2 = new Quaternion(0.0F, 0.0F, 0.0F, 0.0F);
	private final Vec3f[] defaultNormals;
	private final Vec3f[] planes;

	public FrustumCulling() {
		defaultNormals = new Vec3f[4];
		planes = new Vec3f[4];
		for (int i = 0; i < 4; i++) {
			defaultNormals[i] = NORMALS[i].copy();
			planes[i] = NORMALS[i].copy();
		}
	}

	public void setFOV(float angle) {
		for (int i = 0; i < 4; i++) {
			Vec3f original = NORMALS[i];
			Vec3f normal = defaultNormals[i];
			normal.set(original.getX(), original.getY(), original.getZ());
			if (normal.getX() != 0.0F) {
				setRotation(rotation, Vec3f.POSITIVE_Y, normal.getX() > 0.0F ? angle : -angle);
			} else {
				setRotation(rotation, Vec3f.POSITIVE_X, normal.getY() > 0.0F ? -angle : angle);
			}
			normal.rotate(rotation);
		}
	}

	public void rotate(float yaw, float pitch) {
		setRotation(rotation2, Vec3f.POSITIVE_X, pitch * TO_RADIANS);
		setRotation(rotation, Vec3f.POSITIVE_Y, -yaw * TO_RADIANS);
		rotation.hamiltonProduct(rotation2);
		for (int i = 0; i < 4; i++) {
			Vec3f normal = defaultNormals[i];
			planes[i].set(normal.getX(), normal.getY(), normal.getZ());
			planes[i].rotate(rotation);
		}
	}

	public boolean isOutside(Vec3f pos, float distance) {
		for (int i = 0; i < 4; i++) {
			if (planes[i].dot(pos) > distance) return true;
		}
		return false;
	}

	private void setRotation(Quaternion q, Vec3f axis, float angle) {
		float halfAngle = angle * 0.5F;
		float sin = MathHelper.sin(halfAngle);
		q.set(
			axis.getX() * sin,
			axis.getY() * sin,
			axis.getZ() * sin,
			MathHelper.cos(halfAngle)
		);
	}

	static {
		NORMALS = new Vec3f[] {
			new Vec3f( 1.0F, 0.0F, 0.0F),
			new Vec3f(-1.0F, 0.0F, 0.0F),
			new Vec3f( 0.0F, 1.0F, 0.0F),
			new Vec3f( 0.0F, -1.0F, 0.0F)
		};
	}
}
