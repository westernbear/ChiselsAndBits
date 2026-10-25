package mod.chiselsandbits.client.model.loader;

import java.util.List;
import java.util.function.Predicate;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.model.baked.DataAwareBakedModel;
import mod.chiselsandbits.client.model.data.IModelData;
import mod.chiselsandbits.interfaces.ICacheClearable;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockGeometry;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Bridges the cached C&B geometry into Minecraft 26.2's block-state model pipeline. */
public final class FabricBakedModelDelegate implements BlockStateModel, FabricBlockStateModel, ICacheClearable {
    private final DataAwareBakedModel wrapped;
    private final BlockStateModel fallback;
    private boolean cached;

    public FabricBakedModelDelegate(final DataAwareBakedModel wrapped, final BlockStateModel fallback) {
        this.wrapped = wrapped;
        this.fallback = fallback;
    }

    @Override
    public void collectParts(final RandomSource random, final List<BlockStateModelPart> output) {
        // Dynamic geometry needs level and block-entity context, supplied by emitQuads.
    }

    @Override
    public Material.Baked particleMaterial() {
        return fallback.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return fallback.materialFlags() | BakedQuad.FLAG_TRANSLUCENT | BakedQuad.FLAG_ANIMATED;
    }

    @Override
    public void emitQuads(
            final QuadEmitter emitter,
            final BlockAndTintGetter level,
            final BlockPos pos,
            final BlockState state,
            final RandomSource random,
            final Predicate<@Nullable Direction> cullTest) {
        initializeCaches();

        if (!(level.getBlockEntity(pos) instanceof TileEntityBlockChiseled blockEntity)
                || !(blockEntity.getRenderData() instanceof IModelData modelData)) {
            emitFallback(emitter, level, pos, state, random, cullTest);
            return;
        }

        for (final ChiseledBlockGeometry.RenderedQuad rendered :
                ChiseledBlockGeometry.collect(wrapped, level, pos, state, random, cullTest, modelData)) {
            emitter.fromBakedQuad(rendered.quad());
            emitter.chunkLayer(rendered.layer());
            emitter.cullFace(rendered.cullFace());
            if (rendered.tintColor() != -1) {
                emitter.tintIndex(-1);
                emitter.multiplyColor(rendered.tintColor());
            }
            emitter.emit();
        }
    }

    private void emitFallback(
            final QuadEmitter emitter,
            final BlockAndTintGetter level,
            final BlockPos pos,
            final BlockState state,
            final RandomSource random,
            final Predicate<@Nullable Direction> cullTest) {
        ((FabricBlockStateModel) (Object) fallback).emitQuads(emitter, level, pos, state, random, cullTest);
    }

    private void initializeCaches() {
        if (!cached) {
            BlockBitInfo.recalculate();
            VoxelBlob.clearCache();
            cached = true;
        }
    }

    @Override
    public void clearCache() {
        if (wrapped instanceof ICacheClearable cache) {
            cache.clearCache();
        }
        cached = false;
    }
}
