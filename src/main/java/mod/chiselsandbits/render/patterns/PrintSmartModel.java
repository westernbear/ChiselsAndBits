package mod.chiselsandbits.render.patterns;

import java.util.Set;
import java.util.WeakHashMap;
import mod.chiselsandbits.client.model.baked.BaseSmartModel;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.client.model.data.IModelData;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.interfaces.IPatternItem;
import mod.chiselsandbits.render.NullBakedModel;
import mod.chiselsandbits.render.chiseledblock.ChiselRenderType;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class PrintSmartModel extends BaseSmartModel {

    final IPatternItem item;
    final String name;
    WeakHashMap<ItemStack, PrintBaked> cache = new WeakHashMap<ItemStack, PrintBaked>();

    public PrintSmartModel(final String name, final IPatternItem item) {
        this.name = name;
        this.item = item;
    }

    @Override
    public LegacyBakedModel resolve(LegacyBakedModel originalModel, ItemStack stack, Level world, LivingEntity entity) {
        if (ClientSide.instance.holdingShift()) {
            PrintBaked npb = cache.get(stack);

            if (npb == null) {
                cache.put(stack, npb = new PrintBaked(name, item, stack));
            }

            return npb;
        }

        return NullBakedModel.instance;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public Set<ChiselRenderType> getRenderTypes(
            @NotNull BlockAndTintGetter world,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @NotNull IModelData modelData) {
        return Set.of(ChiselRenderType.SOLID);
    }
}
