package mod.chiselsandbits;

import mod.chiselsandbits.client.model.loader.FabricBakedModelDelegate;
import mod.chiselsandbits.client.model.loader.LegacyItemModelDelegate;
import mod.chiselsandbits.render.SmartModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

public class ChiselsAndBitsModelLoadingPlugin implements ModelLoadingPlugin {
    @Override
    public void initialize(final Context pluginContext) {
        final SmartModelManager.Installed models =
                SmartModelManager.getInstance().install();

        pluginContext.modifyBlockModelAfterBake().register((model, modifierContext) -> {
            if (modifierContext.state().getBlock() != mod.chiselsandbits.registry.ModBlocks.CHISELED_BLOCK.get()) {
                return model;
            }
            return new FabricBakedModelDelegate(models.chiseledModel(), model);
        });

        pluginContext.modifyItemModelAfterBake().register((model, modifierContext) -> {
            final var dynamicModel = models.itemModels().get(modifierContext.itemId());
            if (dynamicModel == null) {
                return model;
            }
            return new LegacyItemModelDelegate(dynamicModel, model, modifierContext.transformation());
        });
    }
}
