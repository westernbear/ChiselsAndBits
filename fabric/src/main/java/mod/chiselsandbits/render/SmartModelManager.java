package mod.chiselsandbits.render;

import java.util.Map;
import mod.chiselsandbits.client.model.baked.BaseSmartModel;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.client.model.loader.FabricBakedModelDelegate;
import mod.chiselsandbits.client.model.loader.LegacyItemModelDelegate;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.render.bit.BitItemSmartModel;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockSmartModel;
import mod.chiselsandbits.render.patterns.PrintSmartModel;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Installs the split 26.2 block/item model adapters on every resource reload. */
public final class SmartModelManager {

    private static final SmartModelManager INSTANCE = new SmartModelManager();
    private volatile Map<Identifier, BaseSmartModel> itemModels = Map.of();

    private SmartModelManager() {}

    public static SmartModelManager getInstance() {
        return INSTANCE;
    }

    public void initialize(final ModelLoadingPlugin.Context context) {
        final ChiseledBlockSmartModel chiseledModel = new ChiseledBlockSmartModel();
        final BitItemSmartModel bitModel = new BitItemSmartModel();
        chiseledModel.clearCache();
        bitModel.clearCache();

        context.modifyBlockModelAfterBake().register((model, modifierContext) -> {
            if (modifierContext.state().getBlock() != ModBlocks.CHISELED_BLOCK.get()) {
                return model;
            }

            return new FabricBakedModelDelegate(chiseledModel, model);
        });

        final Map<Identifier, BaseSmartModel> itemModels = Map.of(
                itemId(ModItems.ITEM_BLOCK_BIT.get()), bitModel,
                itemId(ModItems.ITEM_CHISELED_BLOCK.get()), chiseledModel,
                itemId(ModItems.ITEM_POSITIVE_PRINT_WRITTEN.get()),
                        new PrintSmartModel("positiveprint", ModItems.ITEM_POSITIVE_PRINT_WRITTEN.get()),
                itemId(ModItems.ITEM_NEGATIVE_PRINT_WRITTEN.get()),
                        new PrintSmartModel("negativeprint", ModItems.ITEM_NEGATIVE_PRINT_WRITTEN.get()),
                itemId(ModItems.ITEM_MIRROR_PRINT_WRITTEN.get()),
                        new PrintSmartModel("mirrorprint", ModItems.ITEM_MIRROR_PRINT_WRITTEN.get()));
        this.itemModels = itemModels;

        context.modifyItemModelAfterBake().register((model, modifierContext) -> {
            final BaseSmartModel dynamicModel = itemModels.get(modifierContext.itemId());
            if (dynamicModel == null) {
                return model;
            }

            return new LegacyItemModelDelegate(dynamicModel, model, modifierContext.transformation());
        });
    }

    /**
     * Resolves the same generated C&B geometry used by the 26.2 item-model
     * adapter. Placement previews need this quad model directly instead of an
     * already-submitted item render state.
     */
    @Nullable
    public LegacyBakedModel resolveLegacyItemModel(
            final ItemStack stack, @Nullable final ClientLevel level, @Nullable final LivingEntity entity) {
        final BaseSmartModel dynamicModel = itemModels.get(itemId(stack.getItem()));
        if (dynamicModel == null) {
            return null;
        }

        final LegacyBakedModel resolved = dynamicModel.resolve(NullBakedModel.instance, stack, level, entity);
        if (resolved == null || resolved == NullBakedModel.instance || resolved == dynamicModel) {
            return null;
        }

        return resolved;
    }

    private static Identifier itemId(final net.minecraft.world.item.Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
