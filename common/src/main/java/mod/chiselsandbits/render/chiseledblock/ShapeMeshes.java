package mod.chiselsandbits.render.chiseledblock;

import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.data.VoxelBlobStateInstance;
import mod.chiselsandbits.chiseledblock.data.VoxelBlobStateReference;
import mod.chiselsandbits.client.culling.MCCullTest;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.jetbrains.annotations.Nullable;

/**
 * Client cache of {@link ShapeMesh}es. A placed shape's mesh lives on its shared {@link VoxelBlobStateInstance}, so
 * every block entity with the same bits reuses one mesh and it goes away with the last of them. Lookups are a field
 * read; there is no size bound to thrash.
 */
public final class ShapeMeshes {
    private static volatile int generation = 1;
    private static volatile int cachesBuiltFor;

    private ShapeMeshes() {}

    /** Drops every cached mesh; called whenever models, layer filters or state ids are rebuilt. */
    public static void invalidate() {
        // ponytail: only the render thread invalidates, so a plain volatile increment is enough.
        generation++;
    }

    /** The mesh for a chiseled block entity's render data, or null when it has no bits yet. */
    public static @Nullable ShapeMesh forRenderData(@Nullable final Object renderData) {
        return renderData instanceof VoxelBlobStateReference reference ? get(reference.getInstance()) : null;
    }

    public static ShapeMesh get(final VoxelBlobStateInstance shape) {
        final int current = generation;
        if (shape.renderCache instanceof ShapeMesh mesh && mesh.generation == current) {
            return mesh;
        }

        // Racing builds of one shape are harmless: both produce the same mesh and the last one is kept.
        final ShapeMesh mesh = build(shape.peekBlob(), current);
        shape.renderCache = mesh;
        return mesh;
    }

    /** Meshes a blob that is not a shared placed shape, such as an item or a placement preview. */
    public static ShapeMesh of(final VoxelBlob blob) {
        return build(blob, generation);
    }

    private static ShapeMesh build(final VoxelBlob blob, final int current) {
        ensureCaches(current);
        return ShapeMesher.mesh(blob, new MCCullTest(), ShapeMeshes::flags, current);
    }

    /**
     * Layer filters and state lookups are rebuilt lazily on first use after a reload, once the new models are live;
     * meshing threads wait for that single rebuild.
     */
    private static void ensureCaches(final int current) {
        if (cachesBuiltFor == current) {
            return;
        }

        synchronized (ShapeMeshes.class) {
            if (cachesBuiltFor != current) {
                ChiseledBlockGeometry.initializeCaches();
                cachesBuiltFor = current;
            }
        }
    }

    static int flags(final int state) {
        int flags = 0;
        for (final ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            if (VoxelBlob.inLayer(layer, state)) {
                flags |= 1 << layer.ordinal();
            }
        }
        if (VoxelBlob.isFluid(state)) {
            flags |= ShapeMesher.FLAG_FLUID;
        }
        if (VoxelBlob.isOpaque(state)) {
            flags |= ShapeMesher.FLAG_OPAQUE;
        }
        return flags;
    }
}
