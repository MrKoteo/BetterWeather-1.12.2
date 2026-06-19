package paulevs.betterweather.util;

/**
 * Minimal mutable Vec3f and Quaternion classes for FrustumCulling.
 * Only the methods used by FrustumCulling are implemented.
 * Java 8 - no records, no var.
 */
public final class BWVecMath {
	private BWVecMath() {}

	/** Mutable 3D float vector. */
	public static class Vec3f {
		public float x, y, z;

		public static final Vec3f POSITIVE_X = new Vec3f(1.0F, 0.0F, 0.0F);
		public static final Vec3f POSITIVE_Y = new Vec3f(0.0F, 1.0F, 0.0F);

		public Vec3f() {}

		public Vec3f(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public void set(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public Vec3f copy() {
			return new Vec3f(x, y, z);
		}

		public float getX() { return x; }
		public float getY() { return y; }
		public float getZ() { return z; }

		/** Dot product. */
		public float dot(Vec3f other) {
			return x * other.x + y * other.y + z * other.z;
		}

		/**
		 * Rotate this vector by a unit quaternion.
		 * Uses the standard rotation-matrix form of q.
		 */
		public void rotate(Quaternion q) {
			float qx = q.x, qy = q.y, qz = q.z, qw = q.w;
			float xx = 2.0F * qx * qx;
			float yy = 2.0F * qy * qy;
			float zz = 2.0F * qz * qz;
			float xy = 2.0F * qx * qy;
			float xz = 2.0F * qx * qz;
			float yz = 2.0F * qy * qz;
			float wx = 2.0F * qw * qx;
			float wy = 2.0F * qw * qy;
			float wz = 2.0F * qw * qz;

			float nx = x * (1.0F - yy - zz) + y * (xy - wz) + z * (xz + wy);
			float ny = x * (xy + wz) + y * (1.0F - xx - zz) + z * (yz - wx);
			float nz = x * (xz - wy) + y * (yz + wx) + z * (1.0F - xx - yy);

			this.x = nx;
			this.y = ny;
			this.z = nz;
		}
	}

	/** Mutable quaternion (x,y,z,w). */
	public static class Quaternion {
		public float x, y, z, w;

		public Quaternion() {}

		public Quaternion(float x, float y, float z, float w) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.w = w;
		}

		public void set(float x, float y, float z, float w) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.w = w;
		}

		/**
		 * Hamilton product: this = this * other.
		 */
		public void hamiltonProduct(Quaternion other) {
			float ow = other.w, ox = other.x, oy = other.y, oz = other.z;
			float nw = w * ow - x * ox - y * oy - z * oz;
			float nx = w * ox + x * ow + y * oz - z * oy;
			float ny = w * oy - x * oz + y * ow + z * ox;
			float nz = w * oz + x * oy - y * ox + z * ow;
			this.w = nw;
			this.x = nx;
			this.y = ny;
			this.z = nz;
		}
	}
}
