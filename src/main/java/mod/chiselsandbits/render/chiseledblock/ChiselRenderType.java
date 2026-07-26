package mod.chiselsandbits.render.chiseledblock;

import java.security.InvalidParameterException;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.data.VoxelType;
import mod.chiselsandbits.client.culling.ICullTest;
import mod.chiselsandbits.client.culling.MCCullTest;
import net.minecraft.client.renderer.RenderType;

public enum ChiselRenderType {
    SOLID(RenderType.solid(), VoxelType.SOLID),
    SOLID_FLUID(RenderType.solid(), VoxelType.FLUID),
    CUTOUT(RenderType.cutout(), null),
    CUTOUT_MIPPED(RenderType.cutoutMipped(), null),
    TRANSLUCENT(RenderType.translucent(), null),
    TRANSLUCENT_FLUID(RenderType.translucent(), VoxelType.FLUID),
    TRIPWIRE(RenderType.tripwire(), null);

    public final RenderType layer;
    public final VoxelType type;

    ChiselRenderType(final RenderType layer, final VoxelType type) {
        this.layer = layer;
        this.type = type;
    }

    public static ChiselRenderType fromLayer(RenderType layerInfo, final boolean isFluid) {
        if (layerInfo == null) {
            layerInfo = RenderType.solid();
        }

        if (ChiselRenderType.CUTOUT.layer.equals(layerInfo)) {
            return CUTOUT;
        } else if (ChiselRenderType.CUTOUT_MIPPED.layer.equals(layerInfo)) {
            return CUTOUT_MIPPED;
        } else if (ChiselRenderType.SOLID.layer.equals(layerInfo)) {
            return isFluid ? SOLID_FLUID : SOLID;
        } else if (ChiselRenderType.TRANSLUCENT.layer.equals(layerInfo)) {
            return isFluid ? TRANSLUCENT_FLUID : TRANSLUCENT;
        } else if (ChiselRenderType.TRIPWIRE.layer.equals(layerInfo)) {
            return TRIPWIRE;
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
