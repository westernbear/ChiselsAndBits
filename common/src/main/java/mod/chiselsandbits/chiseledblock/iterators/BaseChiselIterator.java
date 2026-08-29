package mod.chiselsandbits.chiseledblock.iterators;

import mod.chiselsandbits.chiseledblock.data.IntegerBox;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import net.minecraft.world.phys.AABB;

public abstract class BaseChiselIterator implements ChiselIterator {

    @Override
    public IntegerBox getVoxelBox(final VoxelBlob vb, final boolean boundSolids) {
        final IntegerBox box = new IntegerBox(0, 0, 0, 0, 0, 0);

        boolean started = false;
        while (hasNext()) {
            if (vb.get(x(), y(), z()) != 0 == boundSolids) {
                if (started) {
                    box.minX = Math.min(box.minX, x());
                    box.minY = Math.min(box.minY, y());
                    box.minZ = Math.min(box.minZ, z());
                    box.maxX = Math.max(box.maxX, x());
                    box.maxY = Math.max(box.maxY, y());
                    box.maxZ = Math.max(box.maxZ, z());
                } else {
                    started = true;
                    box.minX = x();
                    box.minY = y();
                    box.minZ = z();
                    box.maxX = x();
                    box.maxY = y();
                    box.maxZ = z();
                }
            }
        }

        if (started) {
            return box;
        } else {
            return null;
        }
    }

    @Override
    public AABB getBoundingBox(final VoxelBlob vb, final boolean boundSolids) {
        final float One16thf = 1.0f / vb.detail;
        final IntegerBox box = getVoxelBox(vb, boundSolids);

        if (box != null) {
            return new AABB(
                    box.minX * One16thf,
                    box.minY * One16thf,
                    box.minZ * One16thf,
                    (box.maxX + 1) * One16thf,
                    (box.maxY + 1) * One16thf,
                    (box.maxZ + 1) * One16thf);
        } else {
            return null;
        }
    }
}
