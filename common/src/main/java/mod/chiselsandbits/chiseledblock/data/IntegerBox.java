package mod.chiselsandbits.chiseledblock.data;

import net.minecraft.core.Direction;

public final class IntegerBox {
    public int minX;
    public int minY;
    public int minZ;
    public int maxX;
    public int maxY;
    public int maxZ;

    public IntegerBox(final int x1, final int y1, final int z1, final int x2, final int y2, final int z2) {
        minX = x1;
        maxX = x2;

        minY = y1;
        maxY = y2;

        minZ = z1;
        maxZ = z2;
    }

    public void move(final Direction side, final int scale) {
        minX += side.getStepX() * scale;
        maxX += side.getStepX() * scale;
        minY += side.getStepY() * scale;
        maxY += side.getStepY() * scale;
        minZ += side.getStepZ() * scale;
        maxZ += side.getStepZ() * scale;
    }

    public boolean isBadBitPositions() {
        return minX < 0 || minY < 0 || minZ < 0 || maxX >= 16 || maxY >= 16 || maxZ >= 16;
    }
}
