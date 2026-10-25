package mod.chiselsandbits.render;

import java.util.Map;
import mod.chiselsandbits.client.model.baked.BaseSmartModel;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.render.bit.BitItemSmartModel;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockSmartModel;
import mod.chiselsandbits.render.patterns.PrintSmartModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-neutral holder for the split 26.2 C&amp;B model adapters.
 *
 * <p>Each loader installs the models from its model hook and wraps the vanilla block/item models with them.
 */
public final class SmartModelManager {

    private static final SmartModelManager INSTANCE = new SmartModelManager();

    private volatile Installed installed;

    private SmartModelManager() {}

    public static SmartModelManager getInstance() {
        return INSTANCE;
    }

    /** Creates (once per resource reload) the dynamic models and the item-id to model map. */
    public Installed install() {
        final ChiseledBlockSmartModel chiseledModel = new ChiseledBlockSmartModel();
        final BitItemSmartModel bitModel = new BitItemSmartModel();
        chiseledModel.clearCache();
        bitModel.clearCache();

        final Map<Identifier, BaseSmartModel> itemModels = Map.of(
                itemId(ModItems.ITEM_BLOCK_BIT.get()), bitModel,
                itemId(ModItems.ITEM_CHISELED_BLOCK.get()), chiseledModel,
                itemId(ModItems.ITEM_POSITIVE_PRINT_WRITTEN.get()),
                        new PrintSmartModel("positiveprint", ModItems.ITEM_POSITIVE_PRINT_WRITTEN.get()),
                itemId(ModItems.ITEM_NEGATIVE_PRINT_WRITTEN.get()),
                        new PrintSmartModel("negativeprint", ModItems.ITEM_NEGATIVE_PRINT_WRITTEN.get()),
                itemId(ModItems.ITEM_MIRROR_PRINT_WRITTEN.get()),
                        new PrintSmartModel("mirrorprint", ModItems.ITEM_MIRROR_PRINT_WRITTEN.get()));

        final Installed result = new Installed(chiseledModel, bitModel, itemModels);
        installed = result;
        return result;
    }

    public @Nullable Installed installed() {
        return installed;
    }

    /**
     * Resolves the same generated C&amp;B geometry used by the 26.2 item-model adapter. Placement previews need this
     * quad model directly instead of an already-submitted item render state.
     */
    @Nullable
    public LegacyBakedModel resolveLegacyItemModel(
            final ItemStack stack, @Nullable final ClientLevel level, @Nullable final LivingEntity entity) {
        final Installed current = installed;
        if (current == null) {
            return null;
        }

        final BaseSmartModel dynamicModel = current.itemModels().get(itemId(stack.getItem()));
        if (dynamicModel == null) {
            return null;
        }

        final LegacyBakedModel resolved = dynamicModel.resolve(NullBakedModel.instance, stack, level, entity);
        if (resolved == null || resolved == NullBakedModel.instance || resolved == dynamicModel) {
            return null;
        }

        return resolved;
    }

    public static Identifier itemId(final net.minecraft.world.item.Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public record Installed(
            ChiseledBlockSmartModel chiseledModel,
            BitItemSmartModel bitModel,
            Map<Identifier, BaseSmartModel> itemModels) {}
}
