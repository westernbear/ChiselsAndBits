package mod.chiselsandbits.render.chiseledblock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.model.baked.DataAwareBakedModel;
import mod.chiselsandbits.client.model.data.IModelData;
import mod.chiselsandbits.helpers.ModUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-neutral extraction of the dynamic C&amp;B block geometry.
 *
 * <p>Produced quads carry the resolved per-bit tint colour; consumers either multiply it into their own emitter
 * (Fabric) or remap it into a tint-layer array (NeoForge {@code submitBlockModel}).
 */
public final class ChiseledBlockGeometry {

    public record RenderedQuad(
            BakedQuad quad,
            int tintColor,
            ChunkSectionLayer layer,
            @Nullable Direction cullFace) {}

    private ChiseledBlockGeometry() {}

    public static void initializeCaches() {
        BlockBitInfo.recalculate();
        VoxelBlob.clearCache();
    }

    public static List<RenderedQuad> collect(
            final DataAwareBakedModel wrapped,
            final BlockAndTintGetter level,
            final BlockPos pos,
            final BlockState state,
            final RandomSource random,
            final Predicate<@Nullable Direction> cullTest,
            final IModelData data) {
        final List<RenderedQuad> quads = new ArrayList<>();

        synchronized (wrapped) {
            wrapped.updateModelData(level, pos, state, data);
            for (final ChiselRenderType renderType : wrapped.getRenderTypes(level, pos, state, data)) {
                collectSide(quads, wrapped, level, pos, state, random, data, renderType, null);
                for (final Direction direction : Direction.values()) {
                    if (!cullTest.test(direction)) {
                        collectSide(quads, wrapped, level, pos, state, random, data, renderType, direction);
                    }
                }
            }
        }

        return quads;
    }

    private static void collectSide(
            final List<RenderedQuad> output,
            final DataAwareBakedModel wrapped,
            final BlockAndTintGetter level,
            final BlockPos pos,
            final BlockState state,
            final RandomSource random,
            final IModelData data,
            final ChiselRenderType renderType,
            @Nullable final Direction side) {
        for (final BakedQuad quad : wrapped.getQuads(state, side, random, data, renderType)) {
            output.add(new RenderedQuad(quad, tintFor(quad, level, pos), renderType.layer, side));
        }
    }

    /** Resolves the packed C&amp;B tint reference (state id + tint index) into an ARGB colour. */
    public static int tintFor(final BakedQuad quad, final BlockAndTintGetter level, final BlockPos pos) {
        final int packedTint = quad.materialInfo().tintIndex();
        if (packedTint < 0) {
            return -1;
        }

        final BlockState containedState = ModUtil.getStateById(packedTint >>> 8);
        final int tintIndex = packedTint & 0xff;
        final BlockTintSource tintSource =
                Minecraft.getInstance().getBlockColors().getTintSource(containedState, tintIndex);
        int color = tintSource == null ? -1 : tintSource.colorInWorld(containedState, level, pos);
        if ((color & 0xff000000) == 0) {
            color |= 0xff000000;
        }
        return color;
    }

    public static BakedQuad withTintIndex(final BakedQuad quad, final int tintIndex) {
        final BakedQuad.MaterialInfo material = quad.materialInfo();
        final BakedQuad.MaterialInfo remapped = new BakedQuad.MaterialInfo(
                material.sprite(),
                material.layer(),
                material.itemRenderType(),
                tintIndex,
                material.shade(),
                material.lightEmission());

        return new BakedQuad(
                quad.position0(),
                quad.position1(),
                quad.position2(),
                quad.position3(),
                quad.packedUV0(),
                quad.packedUV1(),
                quad.packedUV2(),
                quad.packedUV3(),
                quad.direction(),
                remapped);
    }
}
