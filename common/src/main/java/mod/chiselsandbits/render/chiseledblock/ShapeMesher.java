package mod.chiselsandbits.render.chiseledblock;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.culling.ICullTest;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a {@link ShapeMesh} from a voxel blob in one pass over all layers.
 *
 * <p>Every palette state gets 16-bit occupancy rows; a face is visible where a row is set and the cell beyond it is
 * air or a state the cull test shows it against. Visible rows are merged with binary greedy meshing: take the run at
 * the lowest set bit, then grow it over every following row that contains the whole run.
 */
public final class ShapeMesher {
    /** Flag bits per state: the low byte holds one bit per {@code ChunkSectionLayer} ordinal. */
    public static final int LAYER_MASK = 0xff;

    public static final int FLAG_FLUID = 1 << 8;
    /** A full opaque cube: it hides every face that touches it. */
    public static final int FLAG_OPAQUE = 1 << 9;

    private static final int DIM = ShapeMesh.DIM;
    private static final Direction[] SIDES = new Direction[ShapeMesh.SIDES];
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    static {
        for (final Direction direction : Direction.values()) {
            SIDES[direction.get3DDataValue()] = direction;
        }
    }

    private ShapeMesher() {}

    /**
     * @param test decides whether a state shows a face against a different, non-opaque neighbour state (0 is air)
     * @param stateFlags {@link #LAYER_MASK} bits, {@link #FLAG_FLUID} and {@link #FLAG_OPAQUE} for a state id; a face
     *     against a {@link #FLAG_OPAQUE} state is always hidden, without asking {@code test}
     */
    public static ShapeMesh mesh(
            final VoxelBlob blob, final ICullTest test, final IntUnaryOperator stateFlags, final int generation) {
        final Scratch scratch = SCRATCH.get();
        try {
            scratch.collect(blob);
            return scratch.build(test, stateFlags, generation);
        } finally {
            scratch.reset();
        }
    }

    /**
     * The rectangles of {@code side} once the boundary cells in {@code covered} are hidden by a neighbour.
     *
     * @return the precomputed rectangles when nothing on the side is covered
     */
    static long @Nullable [] coveredEdgeRects(final ShapeMesh mesh, final int side, final short @Nullable [] covered) {
        final long[] uncovered = mesh.edgeRects[side];
        if (uncovered == null || covered == null) {
            return uncovered;
        }

        final short[] rows = mesh.edgeRows[side];
        boolean hit = false;
        for (int index = 0; index < rows.length && !hit; index++) {
            hit = (rows[index] & covered[index & 15]) != 0;
        }
        if (!hit) {
            return uncovered;
        }

        final int plane = boundary(SIDES[side]);
        final int[] masked = new int[DIM];
        final Longs out = new Longs(uncovered.length);
        for (int entry = 0; entry < mesh.states.length; entry++) {
            int any = 0;
            for (int outer = 0; outer < DIM; outer++) {
                masked[outer] = rows[entry * DIM + outer] & ~covered[outer] & 0xffff;
                any |= masked[outer];
            }
            if (any != 0) {
                greedy(masked, side, plane, entry, out);
            }
        }

        return out.size == 0 ? null : out.toArray();
    }

    static int boundary(final Direction side) {
        return side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? DIM - 1 : 0;
    }

    /** Merges {@code rows} (cleared on return) into rectangles, scanning rows in order and runs from the low bit. */
    private static void greedy(final int[] rows, final int side, final int plane, final int entry, final Longs out) {
        for (int outer = 0; outer < DIM; outer++) {
            int row = rows[outer];
            while (row != 0) {
                final int run = Integer.numberOfTrailingZeros(row);
                final int width = Integer.numberOfTrailingZeros(~(row >>> run));
                final int mask = ((1 << width) - 1) << run;
                int height = 1;
                while (outer + height < DIM && (rows[outer + height] & mask) == mask) {
                    rows[outer + height] &= ~mask;
                    height++;
                }
                row &= ~mask;
                out.add(ShapeMesh.rect(side, plane, run, width, outer, height, entry));
            }
            rows[outer] = 0;
        }
    }

    private static final class Scratch {
        private static final int TABLE_BITS = 13; // twice the 4096 states a blob can hold
        private static final int TABLE = 1 << TABLE_BITS;
        private static final int CELLS = DIM * DIM;

        private final int[] keys = new int[TABLE]; // state + 1, 0 = free
        private final int[] values = new int[TABLE];
        private int[] usedSlots = new int[16];
        private int[] palette = new int[16];
        private int size;
        private int[][] xRows = new int[16][]; // [entry][z << 4 | y], bits over x
        private int[][] yRows = new int[16][]; // [entry][x << 4 | z], bits over y
        private int[][] xRowsByY = new int[16][]; // [entry][y << 4 | z], bits over x; derived in build
        /** Per axis, as [plane << 4 | outer] over the run axis: every bit, and the bits that hide any face. */
        private final int[][] occupied = new int[3][CELLS];

        private final int[][] hiding = new int[3][CELLS];
        private int[] layers = new int[16 * 3]; // [entry * 3 + axis], bit per plane the entry fills
        private int[] translucent = new int[16]; // entries that do not hide every face they touch
        private int translucentCount;
        private int[][] sideRows = new int[16][]; // [entry], the rows of the axis being meshed
        private byte[] visibility = new byte[16 * 17]; // [entry * (size + 1) + neighbour], size = air
        private final int[] plane = new int[DIM];
        private final Longs rects = new Longs(64);
        private final Longs edge = new Longs(64);

        void collect(final VoxelBlob blob) {
            int last = 0;
            int entry = -1;
            int[] ys = null;
            for (int row = 0; row < CELLS; row++) {
                final int y = row & 15;
                final int z = row >>> 4;
                // A row's x bits are mostly one state, so they are gathered in a register and stored per run.
                int bits = 0;
                int bitsEntry = -1;
                for (int x = 0; x < DIM; x++) {
                    final int state = blob.get(x, y, z);
                    if (state == 0) {
                        continue;
                    }

                    if (state != last) {
                        last = state;
                        entry = entry(state);
                        ys = yRows[entry];
                    }
                    if (entry != bitsEntry) {
                        if (bits != 0) {
                            xRows[bitsEntry][row] |= bits;
                        }
                        bits = 0;
                        bitsEntry = entry;
                    }
                    bits |= 1 << x;
                    ys[x << 4 | z] |= 1 << y;
                }
                if (bits != 0) {
                    xRows[bitsEntry][row] |= bits;
                }
            }
        }

        /** Derives the Y-axis rows, per-axis occupancy and plane masks from the collected rows. */
        private void index(final int[] flags) {
            translucentCount = 0;
            for (int entry = 0; entry < size; entry++) {
                final boolean hides = (flags[entry] & FLAG_OPAQUE) != 0;
                if (!hides) {
                    translucent[translucentCount++] = entry;
                }

                final int[] xs = xRows[entry];
                final int[] ys = yRows[entry];
                final int[] xy = xRowsByY[entry];
                int planesX = 0;
                int planesY = 0;
                int planesZ = 0;
                for (int cell = 0; cell < CELLS; cell++) {
                    final int xr = xs[cell];
                    final int yr = ys[cell];
                    final int flipped = (cell & 15) << 4 | cell >>> 4;
                    xy[flipped] = xr;
                    occupied[0][cell] |= yr;
                    occupied[1][flipped] |= xr;
                    occupied[2][cell] |= xr;
                    if (hides) {
                        hiding[0][cell] |= yr;
                        hiding[1][flipped] |= xr;
                        hiding[2][cell] |= xr;
                    }
                    if (xr != 0) {
                        planesY |= 1 << (cell & 15);
                        planesZ |= 1 << (cell >>> 4);
                    }
                    if (yr != 0) {
                        planesX |= 1 << (cell >>> 4);
                    }
                }
                layers[entry * 3] = planesX;
                layers[entry * 3 + 1] = planesY;
                layers[entry * 3 + 2] = planesZ;
            }
        }

        ShapeMesh build(final ICullTest test, final IntUnaryOperator stateFlags, final int generation) {
            final int[] states = Arrays.copyOf(palette, size);
            final int[] flags = new int[size];
            for (int entry = 0; entry < size; entry++) {
                flags[entry] = stateFlags.applyAsInt(states[entry]);
            }
            index(flags);

            final int cells = size * (size + 1);
            if (visibility.length < cells) {
                visibility = new byte[cells];
            } else {
                Arrays.fill(visibility, 0, cells, (byte) 0);
            }

            final long[][] edgeRects = new long[ShapeMesh.SIDES][];
            final short[][] edgeRows = new short[ShapeMesh.SIDES][];
            final short[][] opaqueRows = new short[ShapeMesh.SIDES][];

            for (final Direction direction : SIDES) {
                final int side = direction.get3DDataValue();
                final int axis = direction.getAxis().ordinal();
                final int step = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
                final int boundary = boundary(direction);
                for (int entry = 0; entry < size; entry++) {
                    sideRows[entry] = rows(axis, entry);
                }
                final int[] occupiedRows = occupied[axis];
                final int[] hidingRows = hiding[axis];

                for (int entry = 0; entry < size; entry++) {
                    final int[] own = sideRows[entry];
                    int planes = layers[entry * 3 + axis];
                    while (planes != 0) {
                        final int layer = Integer.numberOfTrailingZeros(planes);
                        planes &= planes - 1;
                        final boolean isEdge = layer == boundary;
                        final int base = layer << 4;
                        final int beyond = layer + step << 4;
                        int any = 0;
                        for (int outer = 0; outer < DIM; outer++) {
                            int visible = own[base | outer];
                            if (visible != 0 && !isEdge) {
                                // Opaque bits hide every face; air and translucent bits ask the memoized cull test.
                                final int next = beyond | outer;
                                final int occupiedNext = occupiedRows[next];
                                final int air = visible & ~occupiedNext;
                                final int touching = visible & occupiedNext & ~hidingRows[next] & ~own[next];
                                visible = air != 0 && isVisible(test, states, entry, size) ? air : 0;
                                if (touching != 0) {
                                    visible |= translucentBits(test, states, entry, touching, next);
                                }
                            }
                            plane[outer] = visible;
                            any |= visible;
                        }
                        if (any == 0) {
                            continue;
                        }

                        if (!isEdge) {
                            greedy(plane, side, layer, entry, rects);
                            continue;
                        }

                        if (edgeRows[side] == null) {
                            edgeRows[side] = new short[size * DIM];
                        }
                        final boolean opaque = (flags[entry] & FLAG_OPAQUE) != 0;
                        if (opaque && opaqueRows[side] == null) {
                            opaqueRows[side] = new short[DIM];
                        }
                        for (int outer = 0; outer < DIM; outer++) {
                            edgeRows[side][entry * DIM + outer] = (short) plane[outer];
                            if (opaque) {
                                opaqueRows[side][outer] |= (short) plane[outer];
                            }
                        }
                        greedy(plane, side, layer, entry, edge);
                    }
                }

                if (edge.size != 0) {
                    edgeRects[side] = edge.toArray();
                    edge.size = 0;
                }
            }

            return new ShapeMesh(generation, states, flags, rects.toArray(), edgeRects, edgeRows, opaqueRows);
        }

        /** The cells of {@code touching}, which meet translucent bits of other states, whose face shows. */
        private int translucentBits(
                final ICullTest test, final int[] states, final int entry, final int touching, final int next) {
            int remaining = touching;
            int visible = 0;
            for (int index = 0; remaining != 0 && index < translucentCount; index++) {
                final int neighbour = translucent[index];
                final int cells = remaining & sideRows[neighbour][next];
                if (cells != 0) {
                    remaining &= ~cells;
                    if (isVisible(test, states, entry, neighbour)) {
                        visible |= cells;
                    }
                }
            }
            return visible;
        }

        private boolean isVisible(final ICullTest test, final int[] states, final int entry, final int neighbour) {
            final int index = entry * (size + 1) + neighbour;
            byte result = visibility[index];
            if (result == 0) {
                result = test.isVisible(states[entry], neighbour == size ? 0 : states[neighbour]) ? (byte) 2 : 1;
                visibility[index] = result;
            }
            return result == 2;
        }

        /** An entry's rows for faces along {@code axis}, indexed [plane << 4 | outer], bits over the run axis. */
        private int[] rows(final int axis, final int entry) {
            return switch (axis) {
                case 0 -> yRows[entry];
                case 1 -> xRowsByY[entry];
                default -> xRows[entry];
            };
        }

        private int entry(final int state) {
            int slot = (state * 0x9E3779B9) >>> (Integer.SIZE - TABLE_BITS);
            while (true) {
                final int key = keys[slot];
                if (key == state + 1) {
                    return values[slot];
                }
                if (key == 0) {
                    keys[slot] = state + 1;
                    values[slot] = add(state, slot);
                    return values[slot];
                }
                slot = slot + 1 & TABLE - 1;
            }
        }

        private int add(final int state, final int slot) {
            if (size == palette.length) {
                final int grown = size * 2;
                palette = Arrays.copyOf(palette, grown);
                usedSlots = Arrays.copyOf(usedSlots, grown);
                xRows = Arrays.copyOf(xRows, grown);
                yRows = Arrays.copyOf(yRows, grown);
                xRowsByY = Arrays.copyOf(xRowsByY, grown);
                sideRows = Arrays.copyOf(sideRows, grown);
                translucent = Arrays.copyOf(translucent, grown);
                layers = Arrays.copyOf(layers, grown * 3);
            }
            if (xRows[size] == null) {
                xRows[size] = new int[CELLS];
                yRows[size] = new int[CELLS];
                xRowsByY[size] = new int[CELLS];
            }
            palette[size] = state;
            usedSlots[size] = slot;
            return size++;
        }

        void reset() {
            for (int entry = 0; entry < size; entry++) {
                keys[usedSlots[entry]] = 0;
                Arrays.fill(xRows[entry], 0);
                Arrays.fill(yRows[entry], 0);
            }
            for (int axis = 0; axis < 3; axis++) {
                Arrays.fill(occupied[axis], 0);
                Arrays.fill(hiding[axis], 0);
            }
            size = 0;
            rects.size = 0;
            edge.size = 0;
        }
    }

    static final class Longs {
        long[] items;
        int size;

        Longs(final int capacity) {
            items = new long[Math.max(capacity, 4)];
        }

        void add(final long value) {
            if (size == items.length) {
                items = Arrays.copyOf(items, size * 2);
            }
            items[size++] = value;
        }

        long[] toArray() {
            return Arrays.copyOf(items, size);
        }
    }
}
