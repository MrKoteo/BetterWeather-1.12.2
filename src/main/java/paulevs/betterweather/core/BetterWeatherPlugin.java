package paulevs.betterweather.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class BetterWeatherPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {

    // MixinBooter owns Mixin in the LaunchClassLoader. Do not call
    // MixinBootstrap.init()/Mixins.addConfiguration() here; implement
    // IEarlyMixinLoader and let MixinBooter queue the configs.
    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList(
            "mixins.better_weather.json",
            "mixins.better_weather.client.json"
        );
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
