package paulevs.betterweather.client.config;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Hooks the in-game Mods list "Config" button to {@link BWConfigGui}. Registered via the
 * {@code guiFactory} attribute on the mod's {@code @Mod} annotation. Client-only - the string is
 * never resolved server-side.
 */
@SideOnly(Side.CLIENT)
public class BWGuiFactory implements IModGuiFactory {

	@Override
	public void initialize(Minecraft minecraftInstance) {
	}

	@Override
	public boolean hasConfigGui() {
		return true;
	}

	@Override
	public GuiScreen createConfigGui(GuiScreen parentScreen) {
		return new BWConfigGui(parentScreen);
	}

	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return null;
	}
}
