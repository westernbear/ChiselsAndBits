package mod.chiselsandbits.chiseledblock.iterators;

import mod.chiselsandbits.chiseledblock.iterators.ChiselExtrudeIterator.ChiselExtrudeMaterialIterator;
import mod.chiselsandbits.helpers.IVoxelSrc;
import mod.chiselsandbits.modes.ChiselMode;
import net.minecraft.core.Direction;

public class ChiselTypeIterator extends BaseChiselIterator implements ChiselIterator {

    public final Direction side;
    final ChiselMode mode;
    private final int full_size;
    private final int max_dim;
    private final int original_x;
    private final int original_y;
    private final int original_z;
    private final int parts;
    private int x_range = 1;
    private int y_range = 1;
    private int z_range = 1;
    private int x, y, z;
    private int offset = -1;

    public ChiselTypeIterator(
            final int dim,
            final int x,
            final int y,
            final int z,
            final int x_size,
            final int y_size,
            final int z_size,
            final Direction side) {
        full_size = dim;
        max_dim = dim - 1;
        mode = ChiselMode.DRAWN_REGION;
        this.side = side;

        x_range = x_size;
        y_range = y_size;
        z_range = z_size;
        parts = x_range * y_range * z_range;

        original_x = x;
        original_y = y;
        original_z = z;
    }

    private ChiselTypeIterator(
            final int dim, int x, int y, int z, final IVoxelSrc source, final ChiselMode mode, final Direction side) {
        int offset = 0;
        full_size = dim;
        max_dim = dim - 1;

        this.side = side;
        this.mode = mode;

        switch (mode) {
            case CUBE_SMALL:
                x_range = 3;
                y_range = 3;
                z_range = 3;
                offset = -1;
                parts = x_range * y_range * z_range;
                break;

            case SNAP2:
                x -= x % 2;
                y -= y % 2;
                z -= z % 2;
                x_range = 2;
                y_range = 2;
                z_range = 2;
                parts = x_range * y_range * z_range;
                break;

            case SNAP4:
                x -= x % 4;
                y -= y % 4;
                z -= z % 4;
                x_range = 4;
                y_range = 4;
                z_range = 4;
                parts = x_range * y_range * z_range;
                break;

            case SNAP8:
                x -= x % 8;
                y -= y % 8;
                z -= z % 8;
                x_range = 8;
                y_range = 8;
                z_range = 8;
                parts = x_range * y_range * z_range;
                break;

            case LINE:
                parts = full_size;
                switch (side) {
                    case DOWN:
                    case UP:
                        y = 0;
                        y_range = full_size;
                        break;
                    case NORTH:
                    case SOUTH:
                        z = 0;
                        z_range = full_size;
                        break;
                    case WEST:
                    case EAST:
                        x = 0;
                        x_range = full_size;
                        break;
                    default:
                        throw new NullPointerException();
                }
                break;

            case PLANE:
                parts = full_size * full_size;
                switch (side) {
                    case DOWN:
                    case UP:
                        x = 0;
                        z = 0;
                        x_range = full_size;
                        z_range = full_size;
                        break;
                    case NORTH:
                    case SOUTH:
                        x = 0;
                        y = 0;
                        x_range = full_size;
                        y_range = full_size;
                        break;
                    case WEST:
                    case EAST:
                        y = 0;
                        z = 0;
                        y_range = full_size;
                        z_range = full_size;
                        break;
                    default:
                        throw new NullPointerException();
                }
                break;

            case CUBE_MEDIUM:
                x_range = 5;
                y_range = 5;
                z_range = 5;
                offset = -2;
                parts = x_range * y_range * z_range;
                break;

            case CUBE_LARGE:
                x_range = 7;
                y_range = 7;
                z_range = 7;
                offset = -3;
                parts = x_range * y_range * z_range;
                break;

            case DRAWN_REGION:
            case SINGLE:
                parts = 1;
                break;

            default:
                throw new NullPointerException();
        }

        original_x = Math.max(0, Math.min(full_size - x_range, x + offset));
        original_y = Math.max(0, Math.min(full_size - y_range, y + offset));
        original_z = Math.max(0, Math.min(full_size - z_range, z + offset));
    }

    public static ChiselIterator create(
            final int dim,
            final int x,
            final int y,
            final int z,
            final IVoxelSrc source,
            final ChiselMode mode,
            final Direction side,
            final boolean place) {
        if (mode == ChiselMode.CONNECTED_MATERIAL) {
            return new ChiselExtrudeMaterialIterator(dim, x, y, z, source, mode, side, place);
        }

        if (mode == ChiselMode.CONNECTED_PLANE) {
            return new ChiselExtrudeIterator(dim, x, y, z, source, mode, side, place);
        }

        if (mode == ChiselMode.SAME_MATERIAL) {
            return new ChiselMaterialIterator(dim, x, y, z, source, mode, side, place);
        }

        return new ChiselTypeIterator(dim, x, y, z, source, mode, side);
    }

    @Override
    public boolean hasNext() {
        if (++offset != 0) {

            ++x;

            boolean x_up = false;
            if (x >= x_range) {
                ++y;
                x = 0;
                x_up = true;
            }

            if (y >= y_range && x_up) {
                ++z;
                y = 0;
            }
        }

        return offset < parts;
    }

    @Override
    public int x() {
        return Math.max(0, Math.min(max_dim, original_x + x));
    }

    @Override
    public int y() {
        return Math.max(0, Math.min(max_dim, original_y + y));
    }

    @Override
    public int z() {
        return Math.max(0, Math.min(max_dim, original_z + z));
    }

    @Override
    public Direction side() {
        return side;
    }
}
