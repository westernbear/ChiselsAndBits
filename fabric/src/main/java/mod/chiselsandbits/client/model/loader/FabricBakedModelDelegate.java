package mod.chiselsandbits.client.model.loader;

import java.util.List;
import java.util.function.Predicate;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.client.model.baked.DataAwareBakedModel;
import mod.chiselsandbits.interfaces.ICacheClearable;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockGeometry;
import mod.chiselsandbits.render.chiseledblock.ChiseledEmitter;
import mod.chiselsandbits.render.chiseledblock.ShapeMesh;
import mod.chiselsandbits.render.chiseledblock.ShapeMeshes;
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

/** Bakes chiseled blocks into Minecraft 26.2's chunk mesh from the shared per-shape {@link ShapeMesh}. */
public final class FabricBakedModelDelegate implements BlockStateModel, FabricBlockStateModel, ICacheClearable {
    private final DataAwareBakedModel wrapped;
    private final BlockStateModel fallback;

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
        if (!(level.getBlockEntity(pos) instanceof TileEntityBlockChiseled)) {
            emitFallback(emitter, level, pos, state, random, cullTest);
            return;
        }

        // Render data is snapshotted on the main thread, so these reads are safe on meshing workers.
        final ShapeMesh mesh = ShapeMeshes.forRenderData(level.getBlockEntityRenderData(pos));
        if (mesh == null) {
            return;
        }

        final ChiseledBlockGeometry.TintCache tints = new ChiseledBlockGeometry.TintCache(level, pos);
        ChiseledEmitter.emitWorld(
                mesh,
                side -> ShapeMeshes.forRenderData(level.getBlockEntityRenderData(pos.relative(side))),
                cullTest::test,
                (quad, cullFace, layer) -> {
                    emitter.fromBakedQuad(quad);
                    emitter.chunkLayer(layer);
                    emitter.cullFace(cullFace);
                    // The packed tint index means nothing to the chiseled block's colours; bake the bit's colour in.
                    if (quad.materialInfo().tintIndex() >= 0) {
                        emitter.tintIndex(-1);
                        final int tint = tints.color(quad);
                        if (tint != -1) {
                            emitter.multiplyColor(tint);
                        }
                    }
                    emitter.emit();
                });
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

    @Override
    public void clearCache() {
        if (wrapped instanceof ICacheClearable cache) {
            cache.clearCache();
        }
    }
}
