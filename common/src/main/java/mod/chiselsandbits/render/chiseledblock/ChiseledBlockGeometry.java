package mod.chiselsandbits.render.chiseledblock;

import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.helpers.ModUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Loader-neutral helpers for placed chiseled geometry.
 *
 * <p>Quads carry a packed tint reference (state id + tint index); loaders resolve it per block with {@link TintCache}
 * and multiply it into their own quad output.
 */
public final class ChiseledBlockGeometry {

    private ChiseledBlockGeometry() {}

    /** Rebuilds fluid tables, layer filters and the state lookup; run once per reload by {@link ShapeMeshes}. */
    public static void initializeCaches() {
        BlockBitInfo.recalculate();
        VoxelBlob.clearCache();
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

    /** Remembers the colours of one block's few tinted states, so biome blending runs once per state, not per quad. */
    public static final class TintCache {
        private final BlockAndTintGetter level;
        private final BlockPos pos;
        private final int[] keys = new int[8];
        private final int[] colors = new int[8];
        private int size;

        public TintCache(final BlockAndTintGetter level, final BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }

        /** The ARGB tint of {@code quad} at this block, or -1 when it is untinted. */
        public int color(final BakedQuad quad) {
            final int packedTint = quad.materialInfo().tintIndex();
            if (packedTint < 0) {
                return -1;
            }

            for (int index = 0; index < size; index++) {
                if (keys[index] == packedTint) {
                    return colors[index];
                }
            }

            final int color = tintFor(quad, level, pos);
            if (size < keys.length) {
                keys[size] = packedTint;
                colors[size++] = color;
            }
            return color;
        }
    }
}
