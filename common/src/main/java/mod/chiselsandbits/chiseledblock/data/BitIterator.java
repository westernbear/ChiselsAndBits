package mod.chiselsandbits.chiseledblock.data;

import net.minecraft.core.BlockPos;

public class BitIterator {

    private static final int zInc = 1 << 8;
    private static final int yInc = 1 << 4;

    private final int xMin;
    private final int yMin;
    private final int yMinOffset;

    private final int xMax;
    private final int yMax;
    private final int zMax;
    // read-only outputs.
    public int x = -1;
    public int y;
    public int z;
    protected int bit;
    private int zOffset = 0;
    private int yOffset = 0;
    private int combined = 0;

    public BitIterator() {
        yMinOffset = 0;
        xMin = 0;
        yMin = 0;
        xMax = VoxelBlob.dim;
        yMax = yInc * VoxelBlob.dim;
        zMax = zInc * VoxelBlob.dim;
    }

    public BitIterator(final BlockPos a, final BlockPos b) {
        xMin = x = clampToRange(Math.min(a.getX(), b.getX()));
        yMin = y = clampToRange(Math.min(a.getY(), b.getY()));
        z = clampToRange(Math.min(a.getZ(), b.getZ()));
        x -= 1;

        yOffset = yMinOffset = yInc * y;
        zOffset = zInc * z;

        xMax = clampToRange(Math.max(a.getX(), b.getX())) + 1;
        yMax = yInc * (clampToRange(Math.max(a.getY(), b.getY())) + 1);
        zMax = zInc * (clampToRange(Math.max(a.getZ(), b.getZ())) + 1);

        combined = zOffset | yOffset;
    }

    public int clampToRange(final int a) {
        return Math.max(0, Math.min(15, a));
    }

    protected void yPlus() {
        x = xMin;

        ++y;
        yOffset += yInc;
    }

    protected void zPlus() {
        y = yMin;
        yOffset = yMinOffset;

        ++z;
        zOffset += zInc;
    }

    public boolean hasNext() {
        ++x;

        if (x >= xMax) {
            yPlus();

            if (yOffset >= yMax) {
                zPlus();

                if (zOffset >= zMax) {
                    done();
                    return false;
                }
            }

            combined = zOffset | yOffset;
        }

        bit = combined | x;
        return true;
    }

    protected void done() {}

    public int getNext(final VoxelBlob blob) {
        return blob.getBit(bit);
    }

    public void setNext(final VoxelBlob blob, final int value) {
        blob.putBit(bit, value);
    }
}
