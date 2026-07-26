package mod.chiselsandbits.client.model.baked;

import java.util.List;
import java.util.Set;
import mod.chiselsandbits.client.model.data.IModelData;
import mod.chiselsandbits.render.chiseledblock.ChiselRenderType;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DataAwareBakedModel extends LegacyBakedModel {

    List<BakedQuad> getQuads(
            @Nullable final BlockState state,
            @Nullable final Direction side,
            @NotNull final RandomSource rand,
            @NotNull final IModelData extraData,
            @NotNull final ChiselRenderType renderType);

    @Deprecated
    default void updateModelData(
            @NotNull final BlockAndTintGetter world,
            @NotNull final BlockPos pos,
            @NotNull final BlockState state,
            @NotNull final IModelData modelData) {}

    default Set<ChiselRenderType> getRenderTypes(
            @NotNull final BlockAndTintGetter world,
            @NotNull final BlockPos pos,
            @NotNull final BlockState state,
            @NotNull IModelData modelData) {
        return Set.of();
    }
}
