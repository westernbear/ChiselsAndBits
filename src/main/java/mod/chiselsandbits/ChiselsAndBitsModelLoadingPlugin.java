package mod.chiselsandbits;

import mod.chiselsandbits.render.SmartModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

public class ChiselsAndBitsModelLoadingPlugin implements ModelLoadingPlugin {
    @Override
    public void initialize(Context pluginContext) {
        SmartModelManager.getInstance().initialize(pluginContext);
    }
}
