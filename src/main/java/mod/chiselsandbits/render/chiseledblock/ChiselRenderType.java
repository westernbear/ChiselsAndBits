package mod.chiselsandbits.render.chiseledblock;

import java.security.InvalidParameterException;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.data.VoxelType;
import mod.chiselsandbits.client.culling.ICullTest;
import mod.chiselsandbits.client.culling.MCCullTest;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public enum ChiselRenderType {
    SOLID(ChunkSectionLayer.SOLID, VoxelType.SOLID),
    SOLID_FLUID(ChunkSectionLayer.SOLID, VoxelType.FLUID),
    CUTOUT(ChunkSectionLayer.CUTOUT, null),
    CUTOUT_MIPPED(ChunkSectionLayer.CUTOUT, null),
    TRANSLUCENT(ChunkSectionLayer.TRANSLUCENT, null),
    TRANSLUCENT_FLUID(ChunkSectionLayer.TRANSLUCENT, VoxelType.FLUID),
    TRIPWIRE(ChunkSectionLayer.CUTOUT, null);

    public final ChunkSectionLayer layer;
    public final VoxelType type;

    ChiselRenderType(final ChunkSectionLayer layer, final VoxelType type) {
        this.layer = layer;
        this.type = type;
    }

    public static ChiselRenderType fromLayer(ChunkSectionLayer layerInfo, final boolean isFluid) {
        if (layerInfo == null) {
            layerInfo = ChunkSectionLayer.SOLID;
        }

        if (layerInfo == ChunkSectionLayer.CUTOUT) {
            return CUTOUT;
        } else if (layerInfo == ChunkSectionLayer.SOLID) {
            return isFluid ? SOLID_FLUID : SOLID;
        } else if (layerInfo == ChunkSectionLayer.TRANSLUCENT) {
            return isFluid ? TRANSLUCENT_FLUID : TRANSLUCENT;
        }

        throw new InvalidParameterException();
    }

    public boolean simulateFilter(final VoxelBlob vb) {
        if (vb == null) {
            return false;
        }

        if (vb.simulateFilter(layer)) {
            if (type != null) {
                return vb.simulateFilterFluids(type == VoxelType.FLUID);
            }

            return true;
        }
        return false;
    }

    public boolean filter(final VoxelBlob vb) {
        if (vb == null) {
            return false;
        }

        if (vb.filter(layer)) {
            if (type != null) {
                return vb.filterFluids(type == VoxelType.FLUID);
            }

            return true;
        }
        return false;
    }

    public ICullTest getTest() {
        return new MCCullTest();
    }
}
