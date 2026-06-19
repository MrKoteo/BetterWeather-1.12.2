package paulevs.betterweather.client;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import paulevs.betterweather.client.rendering.BWLightningRenderer;
import paulevs.betterweather.entity.EntityBWLightning;

@SideOnly(Side.CLIENT)
public class RenderBWLightningFactory implements IRenderFactory<EntityBWLightning> {

    @Override
    public Render<? super EntityBWLightning> createRenderFor(RenderManager manager) {
        return new Render<EntityBWLightning>(manager) {
            @Override
            public void doRender(EntityBWLightning entity, double x, double y, double z,
                    float entityYaw, float partialTicks) {
                BWLightningRenderer.render(
                    entity,
                    (float) x, (float) y, (float) z,
                    this.renderManager.renderEngine);
            }

            @Override
            protected ResourceLocation getEntityTexture(EntityBWLightning entity) {
                return null;
            }
        };
    }
}
