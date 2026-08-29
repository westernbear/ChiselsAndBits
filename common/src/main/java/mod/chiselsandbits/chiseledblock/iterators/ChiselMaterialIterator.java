package mod.chiselsandbits.chiseledblock.iterators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mod.chiselsandbits.chiseledblock.data.BitIterator;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.helpers.IVoxelSrc;
import mod.chiselsandbits.modes.ChiselMode;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;

public class ChiselMaterialIterator extends BaseChiselIterator implements ChiselIterator {

    final int INDEX_X = 0;
    final int INDEX_Y = 8;
    final int INDEX_Z = 16;

    // future state.
    Iterator<Integer> list;

    // present state.
    Direction side;
    int value;

    public ChiselMaterialIterator(
            final int dim,
            final int sx,
            final int sy,
            final int sz,
            final IVoxelSrc source,
            final ChiselMode mode,
            final Direction side,
            final boolean place) {
        this.side = side;
        final List<Integer> selectedpositions = new ArrayList<Integer>();

        final int tx = side.getStepX(), ty = side.getStepY(), tz = side.getStepZ();

        int x = sx, y = sy, z = sz;

        int placeoffsetX = 0;
        int placeoffsetY = 0;
        int placeoffsetZ = 0;

        if (place) {
            x -= tx;
            y -= ty;
            z -= tz;
            placeoffsetX = side.getAxis() == Axis.X ? side.getAxisDirection() == AxisDirection.POSITIVE ? 1 : -1 : 0;
            placeoffsetY = side.getAxis() == Axis.Y ? side.getAxisDirection() == AxisDirection.POSITIVE ? 1 : -1 : 0;
            placeoffsetZ = side.getAxis() == Axis.Z ? side.getAxisDirection() == AxisDirection.POSITIVE ? 1 : -1 : 0;
        }

        final int target = source.getSafe(x, y, z);

        final BitIterator bi = new BitIterator();
        while (bi.hasNext()) {
            if (source.getSafe(bi.x - tx, bi.y - ty, bi.z - tz) == target) {
                final int xx = placeoffsetX + bi.x - tx;
                final int yy = placeoffsetY + bi.y - ty;
                final int zz = placeoffsetZ + bi.z - tz;

                if (xx >= 0 && xx < VoxelBlob.dim && yy >= 0 && yy < VoxelBlob.dim && zz >= 0 && zz < VoxelBlob.dim) {
                    selectedpositions.add(createPos(xx, yy, zz));
                }
            }

            if (source.getSafe(bi.x, bi.y, bi.z) == target) {
                final int xx = placeoffsetX + bi.x;
                final int yy = placeoffsetY + bi.y;
                final int zz = placeoffsetZ + bi.z;

                if (xx >= 0 && xx < VoxelBlob.dim && yy >= 0 && yy < VoxelBlob.dim && zz >= 0 && zz < VoxelBlob.dim) {
                    selectedpositions.add(createPos(xx, yy, zz));
                }
            }
        }

        // we are done, drop the list and keep an iterator.
        list = selectedpositions.iterator();
    }

    private int setValue(final int pos, final int idx) {
        return ((byte) pos & 0xff) << idx;
    }

    private int getValue(final int value, final int idx) {
        return (byte) (value >>> idx & 0xff);
    }

    private int createPos(final int x, final int y, final int z) {
        return setValue(x, INDEX_X) | setValue(y, INDEX_Y) | setValue(z, INDEX_Z);
    }

    @Override
    public boolean hasNext() {
        if (list.hasNext()) {
            value = list.next();
            return true;
        }

        return false;
    }

    @Override
    public Direction side() {
        return side;
    }

    @Override
    public int x() {
        return getValue(value, INDEX_X);
    }

    @Override
    public int y() {
        return getValue(value, INDEX_Y);
    }

    @Override
    public int z() {
        return getValue(value, INDEX_Z);
    }
}
