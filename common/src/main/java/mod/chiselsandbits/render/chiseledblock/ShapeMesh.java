package mod.chiselsandbits.render.chiseledblock;

/**
 * Immutable greedy mesh of one voxel shape, shared by every block entity with that shape.
 *
 * <p>Interior faces are stored as merged rectangles. Faces on the block boundary are stored twice: as rectangles for a
 * side nothing covers, and as raw per-state bit rows so a placed block can drop the cells a neighbouring chiseled block
 * covers and merge the rest again.
 *
 * <p>A rectangle is packed into a {@code long}: side (3 bits, {@code Direction#get3DDataValue}), plane (4), run start
 * (4), width - 1 (4), outer start (4), height - 1 (4) and palette entry (the rest). The run axis is y for x-facing
 * sides and x otherwise; the outer axis is z for x- and y-facing sides and y for z-facing sides.
 */
public final class ShapeMesh {
    static final int SIDES = 6;
    static final int DIM = 16;

    final int generation;
    /** Palette state ids. */
    final int[] states;
    /** Per palette entry {@link ShapeMesher} flags. */
    final int[] flags;
    /** Interior rectangles. */
    final long[] rects;
    /** Per side, the rectangles of an uncovered side; null when the side has no faces. */
    final long[][] edgeRects;
    /** Per side, boundary cells as {@code [entry * 16 + outer]} bit rows over the run axis; null when empty. */
    final short[][] edgeRows;
    /** Per side, the 16 rows of boundary cells holding opaque bits; null when there are none. */
    final short[][] opaqueRows;

    ShapeMesh(
            final int generation,
            final int[] states,
            final int[] flags,
            final long[] rects,
            final long[][] edgeRects,
            final short[][] edgeRows,
            final short[][] opaqueRows) {
        this.generation = generation;
        this.states = states;
        this.flags = flags;
        this.rects = rects;
        this.edgeRects = edgeRects;
        this.edgeRows = edgeRows;
        this.opaqueRows = opaqueRows;
    }

    public boolean isEmpty() {
        if (rects.length != 0) {
            return false;
        }

        for (final long[] side : edgeRects) {
            if (side != null) {
                return false;
            }
        }

        return true;
    }

    /** Rectangles emitted when no side is covered; each becomes one quad per texture layer. */
    public int rectCount() {
        int count = rects.length;
        for (final long[] side : edgeRects) {
            count += side == null ? 0 : side.length;
        }
        return count;
    }

    static long rect(
            final int side,
            final int plane,
            final int run,
            final int width,
            final int outer,
            final int height,
            final int entry) {
        return side
                | plane << 3
                | run << 7
                | (width - 1) << 11
                | outer << 15
                | (long) (height - 1) << 19
                | (long) entry << 23;
    }

    static int side(final long rect) {
        return (int) rect & 7;
    }

    static int plane(final long rect) {
        return (int) (rect >>> 3) & 15;
    }

    static int run(final long rect) {
        return (int) (rect >>> 7) & 15;
    }

    static int width(final long rect) {
        return ((int) (rect >>> 11) & 15) + 1;
    }

    static int outer(final long rect) {
        return (int) (rect >>> 15) & 15;
    }

    static int height(final long rect) {
        return ((int) (rect >>> 19) & 15) + 1;
    }

    static int entry(final long rect) {
        return (int) (rect >>> 23);
    }
}
