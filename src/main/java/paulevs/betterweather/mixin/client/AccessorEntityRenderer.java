package paulevs.betterweather.mixin.client;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes EntityRenderer's private far-plane distance and FOV calc so the cloud pass can rebuild
 * the projection with a larger far plane (clouds otherwise z-clip at the terrain render distance).
 * Using the real FOV keeps distant clouds aligned with the world.
 */
@Mixin(EntityRenderer.class)
public interface AccessorEntityRenderer {

	@Accessor("farPlaneDistance")
	float bw_getFarPlaneDistance();

	@Invoker("getFOVModifier")
	float bw_invokeGetFOVModifier(float partialTicks, boolean useFOVSetting);
}
