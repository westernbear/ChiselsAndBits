package mod.chiselsandbits.chiseledblock.data;

import net.minecraft.world.phys.AABB;

public class BitCollisionIterator extends BitIterator {

    public static final float One16thf = 1.0f / VoxelBlob.dim;
    public static final AABB[] cachedBoxes = new AABB[VoxelBlob.full_size];

    public float physicalX;
    public float physicalY;
    public float physicalZ;

    public float physicalYp1 = One16thf;
    public float physicalZp1 = One16thf;

    @Override
    public boolean hasNext() {
        final boolean r = super.hasNext();
        physicalX = x * One16thf;
        return r;
    }

    @Override
    protected void yPlus() {
        super.yPlus();
        physicalY = y * One16thf;
        physicalYp1 = physicalY + One16thf;
    }

    @Override
    protected void zPlus() {
        super.zPlus();

        physicalZ = z * One16thf;
        physicalZp1 = physicalZ + One16thf;

        physicalY = y * One16thf;
        physicalYp1 = physicalY + One16thf;
    }

    public AABB getBoundingBox() {
        AABB box = cachedBoxes[bit];

        if (box == null) {
            box = cachedBoxes[bit] = new AABB(
                    this.physicalX,
                    this.physicalY,
                    this.physicalZ,
                    this.physicalX + BitCollisionIterator.One16thf,
                    this.physicalYp1,
                    this.physicalZp1);
        }

        return box;
    }
}
