package mod.chiselsandbits.client.model;

import java.util.ArrayList;
import java.util.List;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockGeometry;
import mod.chiselsandbits.render.chiseledblock.ChiseledEmitter;
import mod.chiselsandbits.render.chiseledblock.ShapeMesh;
import mod.chiselsandbits.render.chiseledblock.ShapeMeshes;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.jetbrains.annotations.Nullable;

/**
 * Bakes chiseled blocks into the chunk mesh from the shared per-shape {@link ShapeMesh}, so they cost nothing per frame.
 * Edge quads go in their side's list and vanilla culls them against full neighbours.
 */
public final class ChiseledBlockStateModel implements DynamicBlockStateModel {
    private final BlockStateModel placeholder;

    public ChiseledBlockStateModel(final BlockStateModel placeholder) {
        this.placeholder = placeholder;
    }

    @Override
    public void collectParts(
            final BlockAndTintGetter level,
            final BlockPos pos,
            final BlockState state,
            final RandomSource random,
            final List<BlockStateModelPart> parts) {
        // The section region holds block entities copied on the main thread; render data is an immutable reference.
        final ShapeMesh mesh = ShapeMeshes.forRenderData(renderData(level, pos));
        if (mesh == null) {
            return;
        }

        final Part part = new Part(placeholder.particleMaterial());
        final ChiseledBlockGeometry.TintCache tints = new ChiseledBlockGeometry.TintCache(level, pos);
        ChiseledEmitter.emitWorld(
                mesh,
                side -> ShapeMeshes.forRenderData(renderData(level, pos.relative(side))),
                side -> false,
                (quad, cullFace, layer) -> part.add(untinted(quad, tints.color(quad)), cullFace));
        parts.add(part);
    }

    @Override
    public Material.Baked particleMaterial() {
        return placeholder.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return placeholder.materialFlags() | BakedQuad.FLAG_TRANSLUCENT | BakedQuad.FLAG_ANIMATED;
    }

    private static @Nullable Object renderData(final BlockAndTintGetter level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityBlockChiseled chiseled ? chiseled.getRenderData() : null;
    }

    /**
     * Vanilla would resolve a tint index against the chiseled block, so drop every packed index and bake the bit's own
     * colour in instead (-1, white, when the bit's block has no tint source).
     */
    private static BakedQuad untinted(final BakedQuad quad, final int tint) {
        if (quad.materialInfo().tintIndex() < 0) {
            return quad;
        }

        final BakedQuad.MaterialInfo info = quad.materialInfo();
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
                new BakedQuad.MaterialInfo(
                        info.sprite(),
                        info.layer(),
                        info.itemRenderType(),
                        -1,
                        info.shade(),
                        info.lightEmission(),
                        info.ambientOcclusion()),
                BakedNormals.UNSPECIFIED,
                BakedColors.of(tint));
    }

    private static final class Part implements BlockStateModelPart {
        private static final int UNCULLED = 6;

        private final Material.Baked particle;

        @SuppressWarnings("unchecked")
        private final List<BakedQuad>[] quads = new List[UNCULLED + 1];

        private int flags;

        Part(final Material.Baked particle) {
            this.particle = particle;
        }

        void add(final BakedQuad quad, final @Nullable Direction cullFace) {
            final int index = cullFace == null ? UNCULLED : cullFace.get3DDataValue();
            if (quads[index] == null) {
                quads[index] = new ArrayList<>();
            }
            quads[index].add(quad);
            flags |= quad.materialInfo().flags();
        }

        @Override
        public List<BakedQuad> getQuads(final @Nullable Direction side) {
            final List<BakedQuad> list = quads[side == null ? UNCULLED : side.get3DDataValue()];
            return list == null ? List.of() : list;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public Material.Baked particleMaterial() {
            return particle;
        }

        @Override
        public int materialFlags() {
            return flags;
        }
    }
}
