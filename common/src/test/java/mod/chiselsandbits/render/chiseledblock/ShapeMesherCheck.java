package mod.chiselsandbits.render.chiseledblock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.culling.ICullTest;
import mod.chiselsandbits.render.chiseledblock.baseline.LegacyFaceRegion;
import mod.chiselsandbits.render.chiseledblock.baseline.LegacyFaceRegionExtractor;
import net.minecraft.core.Direction;

public final class ShapeMesherCheck {
    private static final ICullTest ORDERED =
            (state, neighbor) -> state != 0 && state != neighbor && (neighbor == 0 || state < neighbor);
    private static final ICullTest ASYMMETRIC = (state, neighbor) ->
            state != 0 && state != neighbor && (neighbor == 0 ? (state & 1) != 0 : state < neighbor);
    /** Odd states are opaque; every state renders in layer 0. */
    private static final IntUnaryOperator FLAGS = state -> 1 | ((state & 1) != 0 ? ShapeMesher.FLAG_OPAQUE : 0);

    private ShapeMesherCheck() {}

    public static void main(final String[] args) {
        final ArrayList<VoxelBlob> blobs = fixtures();
        for (int index = 0; index < blobs.size(); index++) {
            checkCoverage(blobs.get(index), ORDERED, "fixture " + index + " ordered");
            checkCoverage(blobs.get(index), ASYMMETRIC, "fixture " + index + " asymmetric");
        }
        checkCullWork();
        checkCoveredEdges(blobs);
        checkNotWorseThanBaseline(blobs);
        checkFullBlockIsSixQuads();
    }

    private static ArrayList<VoxelBlob> fixtures() {
        final ArrayList<VoxelBlob> blobs = new ArrayList<>();
        blobs.add(new VoxelBlob());

        final VoxelBlob full = new VoxelBlob();
        full.fill(1);
        blobs.add(full);

        final VoxelBlob singleton = new VoxelBlob();
        singleton.set(7, 8, 9, 2);
        blobs.add(singleton);

        final VoxelBlob hole = new VoxelBlob();
        hole.fill(1);
        hole.set(8, 8, 8, 0);
        blobs.add(hole);

        final VoxelBlob checkerboard = new VoxelBlob();
        forEach((x, y, z) -> checkerboard.set(x, y, z, (x + y + z & 1) + 1));
        blobs.add(checkerboard);

        final Random random = new Random(0x5eed);
        for (final int states : new int[] {2, 4, 6, 24}) {
            for (final int airOneIn : new int[] {0, 2, 4}) {
                final VoxelBlob blob = new VoxelBlob();
                forEach((x, y, z) -> {
                    if (airOneIn == 0 || random.nextInt(airOneIn) != 0) {
                        blob.set(x, y, z, random.nextInt(states) + 1);
                    }
                });
                blobs.add(blob);
            }
        }

        final VoxelBlob slabs = new VoxelBlob();
        forEach((x, y, z) -> slabs.set(x, y, z, y < 8 ? 3 : (x < 5 ? 0 : 4)));
        blobs.add(slabs);
        return blobs;
    }

    /** The mesh covers exactly the reference visible faces, once each, with the right state and edge rows. */
    private static void checkCoverage(final VoxelBlob blob, final ICullTest test, final String label) {
        final ShapeMesh mesh = ShapeMesher.mesh(blob, test, FLAGS, 1);
        final Set<Long> expected = reference(blob, test);
        final Set<Long> actual = new HashSet<>();

        forEachCell(mesh, mesh.rects, (side, x, y, z, entry) -> {
            require(!isBoundary(side, x, y, z), label + ": interior rect on the boundary");
            require(actual.add(cell(side, x, y, z, mesh.states[entry])), label + ": overlapping interior faces");
        });
        for (int side = 0; side < ShapeMesh.SIDES; side++) {
            forEachCell(mesh, mesh.edgeRects[side], (rectSide, x, y, z, entry) -> {
                require(isBoundary(rectSide, x, y, z), label + ": edge rect off the boundary");
                require(actual.add(cell(rectSide, x, y, z, mesh.states[entry])), label + ": overlapping edge faces");
                require(edgeRowHas(mesh, rectSide, entry, x, y, z), label + ": edge rect outside its edge rows");
            });
        }

        require(expected.equals(actual), label + ": faces differ (" + expected.size() + " vs " + actual.size() + ")");
        require(mesh.isEmpty() == expected.isEmpty(), label + ": isEmpty mismatch");
    }

    private static void checkCullWork() {
        final VoxelBlob full = new VoxelBlob();
        full.fill(1);
        require(cullCalls(full) == 0, "equal-state transitions must skip cull calls");

        final VoxelBlob hole = new VoxelBlob();
        hole.fill(1);
        hole.set(8, 8, 8, 0);
        require(cullCalls(hole) == 1, "state-to-air culling must be evaluated once");

        final VoxelBlob checkerboard = new VoxelBlob();
        forEach((x, y, z) -> checkerboard.set(x, y, z, (x + y + z & 1) + 1));
        require(cullCalls(checkerboard) == 1, "an opaque neighbour hides faces without a cull call");

        final VoxelBlob translucent = new VoxelBlob();
        forEach((x, y, z) -> translucent.set(x, y, z, (x + y + z & 1) * 2 + 2));
        require(cullCalls(translucent) == 2, "two translucent states must cache both directed cull pairs");
    }

    private static int cullCalls(final VoxelBlob blob) {
        final int[] calls = {0};
        ShapeMesher.mesh(
                blob,
                (state, neighbor) -> {
                    calls[0]++;
                    return ORDERED.isVisible(state, neighbor);
                },
                FLAGS,
                1);
        return calls[0];
    }

    /** Covered edges keep exactly the boundary cells the neighbour does not hide. */
    private static void checkCoveredEdges(final ArrayList<VoxelBlob> blobs) {
        final Random random = new Random(0xed6e);
        for (final VoxelBlob blob : blobs) {
            final ShapeMesh mesh = ShapeMesher.mesh(blob, ORDERED, FLAGS, 1);
            for (int side = 0; side < ShapeMesh.SIDES; side++) {
                if (mesh.edgeRects[side] == null) {
                    continue;
                }

                final short[] covered = new short[ShapeMesh.DIM];
                for (int outer = 0; outer < covered.length; outer++) {
                    covered[outer] = (short) (random.nextBoolean() ? random.nextInt(1 << 16) : 0);
                }

                final Set<Long> expected = new HashSet<>();
                forEachCell(mesh, mesh.edgeRects[side], (rectSide, x, y, z, entry) -> {
                    final int[] uv = runOuter(rectSide, x, y, z);
                    if ((covered[uv[1]] & 1 << uv[0]) == 0) {
                        expected.add(cell(rectSide, x, y, z, mesh.states[entry]));
                    }
                });

                final Set<Long> actual = new HashSet<>();
                forEachCell(
                        mesh,
                        ShapeMesher.coveredEdgeRects(mesh, side, covered),
                        (rectSide, x, y, z, entry) -> require(
                                actual.add(cell(rectSide, x, y, z, mesh.states[entry])), "covered edge overlap"));
                require(expected.equals(actual), "covered edge cells differ on side " + side);
                require(
                        ShapeMesher.coveredEdgeRects(mesh, side, null) == mesh.edgeRects[side],
                        "an uncovered side must reuse its rects");
            }
        }
    }

    /** Across the fixtures, greedy merging must not need more quads than the old equal-span merge. */
    private static void checkNotWorseThanBaseline(final ArrayList<VoxelBlob> blobs) {
        int greedy = 0;
        int baseline = 0;
        for (final VoxelBlob blob : blobs) {
            greedy += ShapeMesher.mesh(blob, ORDERED, FLAGS, 1).rectCount();
            for (final ArrayList<LegacyFaceRegion> plane : LegacyFaceRegionExtractor.extract(blob, ORDERED)) {
                LegacyFaceRegion.merge(plane);
                baseline += plane.size();
            }
        }
        require(greedy <= baseline, "greedy merge produced " + greedy + " quads, baseline " + baseline);
    }

    private static void checkFullBlockIsSixQuads() {
        final VoxelBlob full = new VoxelBlob();
        full.fill(1);
        final ShapeMesh mesh = ShapeMesher.mesh(full, ORDERED, FLAGS, 1);
        require(mesh.rects.length == 0 && mesh.rectCount() == 6, "a full block must be six edge quads");
        for (int side = 0; side < ShapeMesh.SIDES; side++) {
            for (int outer = 0; outer < ShapeMesh.DIM; outer++) {
                require(mesh.opaqueRows[side][outer] == (short) 0xffff, "full opaque side must be fully covered");
            }
        }
    }

    private static Set<Long> reference(final VoxelBlob blob, final ICullTest test) {
        final Set<Long> faces = new HashSet<>();
        forEach((x, y, z) -> {
            final int state = blob.get(x, y, z);
            if (state == 0) {
                return;
            }
            for (final Direction direction : Direction.values()) {
                final int nx = x + direction.getStepX();
                final int ny = y + direction.getStepY();
                final int nz = z + direction.getStepZ();
                final boolean inside = nx >= 0 && nx < 16 && ny >= 0 && ny < 16 && nz >= 0 && nz < 16;
                final int neighbor = inside ? blob.get(nx, ny, nz) : 0;
                final boolean hidden = (FLAGS.applyAsInt(neighbor) & ShapeMesher.FLAG_OPAQUE) != 0;
                if (!inside || state != neighbor && !hidden && test.isVisible(state, neighbor)) {
                    faces.add(cell(direction.get3DDataValue(), x, y, z, state));
                }
            }
        });
        return faces;
    }

    private static void forEachCell(final ShapeMesh mesh, final long[] rects, final CellConsumer consumer) {
        if (rects == null) {
            return;
        }
        for (final long rect : rects) {
            final int side = ShapeMesh.side(rect);
            final int plane = ShapeMesh.plane(rect);
            for (int run = ShapeMesh.run(rect); run < ShapeMesh.run(rect) + ShapeMesh.width(rect); run++) {
                for (int outer = ShapeMesh.outer(rect);
                        outer < ShapeMesh.outer(rect) + ShapeMesh.height(rect);
                        outer++) {
                    require(run < 16 && outer < 16, "rect leaves the block");
                    final int[] xyz =
                            switch (side >> 1) {
                                case 2 -> new int[] {plane, run, outer};
                                case 0 -> new int[] {run, plane, outer};
                                default -> new int[] {run, outer, plane};
                            };
                    consumer.accept(side, xyz[0], xyz[1], xyz[2], ShapeMesh.entry(rect));
                }
            }
        }
    }

    private static int[] runOuter(final int side, final int x, final int y, final int z) {
        return switch (side >> 1) {
            case 2 -> new int[] {y, z};
            case 0 -> new int[] {x, z};
            default -> new int[] {x, y};
        };
    }

    private static boolean edgeRowHas(
            final ShapeMesh mesh, final int side, final int entry, final int x, final int y, final int z) {
        final int[] uv = runOuter(side, x, y, z);
        return (mesh.edgeRows[side][entry * 16 + uv[1]] & 1 << uv[0]) != 0;
    }

    private static boolean isBoundary(final int side, final int x, final int y, final int z) {
        final Direction direction = Direction.from3DDataValue(side);
        final int coordinate =
                switch (direction.getAxis()) {
                    case X -> x;
                    case Y -> y;
                    case Z -> z;
                };
        return coordinate == ShapeMesher.boundary(direction);
    }

    private static long cell(final int side, final int x, final int y, final int z, final int state) {
        return (long) state << 16 | side << 12 | x << 8 | y << 4 | z;
    }

    private static void forEach(final XyzConsumer consumer) {
        for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    consumer.accept(x, y, z);
                }
            }
        }
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private interface XyzConsumer {
        void accept(int x, int y, int z);
    }

    private interface CellConsumer {
        void accept(int side, int x, int y, int z, int entry);
    }
}
