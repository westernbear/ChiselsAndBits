package mod.chiselsandbits.render.bit;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.HashMap;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.model.baked.BaseSmartModel;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.events.TickHandler;
import mod.chiselsandbits.interfaces.ICacheClearable;
import mod.chiselsandbits.items.ItemChiseledBit;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.render.ModelCombined;
import mod.chiselsandbits.render.chiseledblock.ChiselRenderType;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockBakedModel;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BitItemSmartModel extends BaseSmartModel implements ICacheClearable {
    private static final HashMap<Integer, LegacyBakedModel> modelCache = new HashMap<>();
    private static final HashMap<Integer, LegacyBakedModel> largeModelCache = new HashMap<>();

    private static final NonNullList<ItemStack> alternativeStacks = NonNullList.create();

    private LegacyBakedModel getCachedModel(int stateID, final boolean large) {
        if (stateID == 0) {
            // We are running an empty bit, for display purposes.
            // Lets loop:
            if (alternativeStacks.isEmpty()) {
                ModItems.ITEM_BLOCK_BIT.get().fillItemCategory(alternativeStacks);
            }

            final int alternativeIndex =
                    (int) ((Math.floor(TickHandler.getClientTicks() / 20d)) % alternativeStacks.size());

            stateID = ItemChiseledBit.getStackState(alternativeStacks.get(alternativeIndex));
        }

        final HashMap<Integer, LegacyBakedModel> target = large ? largeModelCache : modelCache;
        LegacyBakedModel out = target.get(stateID);

        if (out == null) {
            if (large) {
                final VoxelBlob blob = new VoxelBlob();
                blob.fill(stateID);
                ChiselRenderType[] layers = ChiselRenderType.values();
                LegacyBakedModel[] models = new LegacyBakedModel[layers.length];
                for (int i = 0; i < layers.length; i++) {
                    ChiselRenderType layer = layers[i];
                    models[i] = new ChiseledBlockBakedModel(stateID, layer, blob, DefaultVertexFormat.BLOCK, true);
                }
                out = new ModelCombined(models);
            } else {
                out = new BitItemBaked(stateID);
            }

            target.put(stateID, out);
        }

        return out;
    }

    public LegacyBakedModel func_239290_a_(
            final LegacyBakedModel originalModel, final ItemStack stack, final Level world, final LivingEntity entity) {
        return getCachedModel(ItemChiseledBit.getStackState(stack), ClientSide.instance.holdingShift());
    }

    @Override
    public LegacyBakedModel resolve(LegacyBakedModel originalModel, ItemStack stack, Level world, LivingEntity entity) {
        return func_239290_a_(originalModel, stack, world, entity);
    }

    @Override
    public void clearCache() {
        modelCache.clear();
        largeModelCache.clear();
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }
}
