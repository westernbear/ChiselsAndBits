package mod.chiselsandbits.render.chiseledblock;

import java.util.ArrayList;
import java.util.Random;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob.VisibleFace;
import mod.chiselsandbits.client.culling.ICullTest;
import net.minecraft.core.Direction;

public final class FaceRegionMergeCheck {
    private static final Direction[] FACE_ORDER = {
        Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.NORTH
    };

    private FaceRegionMergeCheck() {}

    public static void main(final String[] args) {
        checkFullPlane();
        checkEastPlane();
        checkStripedPlane();
        checkExtractionMatchesReference();
        checkCullWorkReduced();
    }

    private static void checkFullPlane() {
        final ArrayList<FaceRegion> faces = new ArrayList<>();

        for (int z = 0; z < 16; z++) {
            final CountingFaceRegion row = new CountingFaceRegion(Direction.UP, 1, 2, z * 2 + 1);
            for (int x = 1; x < 16; x++) {
                require(row.extend(face(x, z)), "failed to build a contiguous row");
            }
            faces.add(row);
        }

        CountingFaceRegion.comparisons = 0;
        FaceRegion.merge(faces);

        require(faces.size() == 1, "a full plane must merge to one face");
        require(area(faces) == 256, "a full plane must retain all 256 cells");
        require(CountingFaceRegion.comparisons <= 16, "merge must stay linear");
    }

    private static void checkEastPlane() {
        final ArrayList<FaceRegion> faces = new ArrayList<>();

        for (int z = 0; z < 16; z++) {
            final CountingFaceRegion row = new CountingFaceRegion(Direction.EAST, 2, 1, z * 2 + 1);
            for (int y = 1; y < 16; y++) {
                require(
                        row.extend(new FaceRegion(Direction.EAST, 2, y * 2 + 1, z * 2 + 1, 1, true)),
                        "failed to build an east-facing row");
            }
            faces.add(row);
        }

        CountingFaceRegion.comparisons = 0;
        FaceRegion.merge(faces);

        require(faces.size() == 1, "an east-facing plane must merge to one face");
        require(area(faces) == 256, "an east-facing plane must retain all 256 cells");
        require(CountingFaceRegion.comparisons <= 16, "east-facing merge must stay linear");
    }

    private static void checkStripedPlane() {
        final ArrayList<FaceRegion> faces = new ArrayList<>();

        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x += 2) {
                faces.add(new CountingFaceRegion(Direction.UP, x * 2 + 1, 2, z * 2 + 1));
            }
        }

        CountingFaceRegion.comparisons = 0;
        FaceRegion.merge(faces);

        require(faces.size() == 8, "eight stripes must remain eight faces");
        require(area(faces) == 128, "striped plane must retain all 128 cells");
        require(CountingFaceRegion.comparisons <= 128, "merge must stay linear");
    }

    private static void checkExtractionMatchesReference() {
        assertExtraction(new VoxelBlob(), "empty");

        final VoxelBlob full = new VoxelBlob();
        full.fill(1);
        assertExtraction(full, "full");

        final VoxelBlob singleton = new VoxelBlob();
        singleton.set(7, 8, 9, 2);
        assertExtraction(singleton, "singleton");

        final VoxelBlob axisEdges = new VoxelBlob();
        axisEdges.set(0, 0, 0, 3);
        axisEdges.set(15, 15, 15, 3);
        axisEdges.set(0, 15, 8, 3);
        axisEdges.set(15, 0, 7, 3);
        assertExtraction(axisEdges, "axis edges");

        final VoxelBlob singleStateRandom = new VoxelBlob();
        final Random singleStateRandomSource = new Random(0xb17);
        for (int z = 0; z < VoxelBlob.dim; z++) {
            for (int y = 0; y < VoxelBlob.dim; y++) {
                for (int x = 0; x < VoxelBlob.dim; x++) {
                    if (singleStateRandomSource.nextInt(4) == 0) {
                        singleStateRandom.set(x, y, z, 3);
                    }
                }
            }
        }
        assertExtraction(singleStateRandom, "single-state random");

        final VoxelBlob hiddenAirFaces = new VoxelBlob();
        hiddenAirFaces.fill(1);
        hiddenAirFaces.set(8, 8, 8, 0);
        assertExtraction(hiddenAirFaces, "hidden air faces", (state, neighbor) -> false, (state, neighbor) -> false);

        final VoxelBlob staleOccupancyCache = new VoxelBlob();
        staleOccupancyCache.fill(1);
        staleOccupancyCache.fillAmount(1, 1);
        assertExtraction(staleOccupancyCache, "stale occupancy cache");

        final VoxelBlob checkerboard = new VoxelBlob();
        for (int z = 0; z < VoxelBlob.dim; z++) {
            for (int y = 0; y < VoxelBlob.dim; y++) {
                for (int x = 0; x < VoxelBlob.dim; x++) {
                    checkerboard.set(x, y, z, (x + y + z & 1) + 1);
                }
            }
        }
        assertExtraction(checkerboard, "checkerboard");

        final VoxelBlob randomBlob = new VoxelBlob();
        final Random random = new Random(0x5eed);
        for (int z = 0; z < VoxelBlob.dim; z++) {
            for (int y = 0; y < VoxelBlob.dim; y++) {
                for (int x = 0; x < VoxelBlob.dim; x++) {
                    randomBlob.set(x, y, z, random.nextInt(4));
                }
            }
        }
        assertExtraction(randomBlob, "random");

        final VoxelBlob fourStates = new VoxelBlob();
        for (int z = 0; z < VoxelBlob.dim; z++) {
            for (int y = 0; y < VoxelBlob.dim; y++) {
                for (int x = 0; x < VoxelBlob.dim; x++) {
                    fourStates.set(x, y, z, (x + 2 * y + 3 * z) % 5);
                }
            }
        }
        final int[] orderedRow = {1, 1, 2, 2, 1, 1, 0, 3, 3, 4, 4, 1, 0, 2, 3, 4};
        for (int x = 0; x < orderedRow.length; x++) {
            fourStates.set(x, 7, 9, orderedRow[x]);
        }
        final ICullTest asymmetricCull = (state, neighbor) ->
                state != 0 && state != neighbor && (neighbor == 0 ? (state & 1) != 0 : state < neighbor);
        assertExtraction(fourStates, "four states", asymmetricCull, asymmetricCull);

        final VoxelBlob fourStateDense = new VoxelBlob();
        final Random fourStateRandom = new Random(0x4b17);
        for (int z = 0; z < VoxelBlob.dim; z++) {
            for (int y = 0; y < VoxelBlob.dim; y++) {
                for (int x = 0; x < VoxelBlob.dim; x++) {
                    fourStateDense.set(x, y, z, fourStateRandom.nextInt(4) + 1);
                }
            }
        }
        assertExtraction(fourStateDense, "four dense states", asymmetricCull, asymmetricCull);

        final VoxelBlob lateFifthState = new VoxelBlob();
        for (int index = 0; index < VoxelBlob.full_size; index++) {
            lateFifthState.set(index & 15, index >>> 4 & 15, index >>> 8 & 15, (index & 3) + 1);
        }
        lateFifthState.set(15, 15, 15, 5);
        assertExtraction(lateFifthState, "late fifth state");
    }

    private static void checkCullWorkReduced() {
        final VoxelBlob full = new VoxelBlob();
        full.fill(1);
        final OrderedCullTest referenceTest = new OrderedCullTest();
        final OrderedCullTest optimizedTest = new OrderedCullTest();

        reference(full, referenceTest);
        FaceRegionExtractor.extract(full, optimizedTest);

        require(referenceTest.calls == 6 * 15 * 16 * 16, "reference scan must test every interior directed face");
        require(optimizedTest.calls == 0, "equal-state transitions must skip cull calls");

        final VoxelBlob hole = new VoxelBlob();
        hole.fill(1);
        hole.set(8, 8, 8, 0);
        final OrderedCullTest holeTest = new OrderedCullTest();
        FaceRegionExtractor.extract(hole, holeTest);
        require(holeTest.calls == 1, "single-state extraction must evaluate state-to-air culling once");

        final VoxelBlob checkerboard = new VoxelBlob();
        for (int z = 0; z < VoxelBlob.dim; z++) {
            for (int y = 0; y < VoxelBlob.dim; y++) {
                for (int x = 0; x < VoxelBlob.dim; x++) {
                    checkerboard.set(x, y, z, (x + y + z & 1) + 1);
                }
            }
        }
        final OrderedCullTest paletteTest = new OrderedCullTest();
        FaceRegionExtractor.extract(checkerboard, paletteTest);
        require(paletteTest.calls == 2, "two-state extraction must cache both directed cull pairs");
    }

    private static void assertExtraction(final VoxelBlob blob, final String label) {
        assertExtraction(blob, label, new OrderedCullTest(), new OrderedCullTest());
    }

    private static void assertExtraction(
            final VoxelBlob blob, final String label, final ICullTest referenceTest, final ICullTest optimizedTest) {
        final ArrayList<ArrayList<FaceRegion>> expected = reference(blob, referenceTest);
        final ArrayList<ArrayList<FaceRegion>> actual = FaceRegionExtractor.extract(blob, optimizedTest);

        assertRegionsEqual(expected, actual, label + " row runs");
        mergeAll(expected);
        mergeAll(actual);
        assertRegionsEqual(expected, actual, label + " merged regions");
    }

    private static ArrayList<ArrayList<FaceRegion>> reference(final VoxelBlob blob, final ICullTest test) {
        final ArrayList<ArrayList<FaceRegion>> result = new ArrayList<>();
        final VisibleFace visible = new VisibleFace();

        for (final Direction face : FACE_ORDER) {
            for (int plane = 0; plane < blob.detail; plane++) {
                final ArrayList<FaceRegion> regions = new ArrayList<>(16);

                for (int outer = 0; outer < blob.detail; outer++) {
                    FaceRegion current = null;

                    for (int run = 0; run < blob.detail; run++) {
                        final int x;
                        final int y;
                        final int z;

                        switch (face.getAxis()) {
                            case X:
                                x = plane;
                                y = run;
                                z = outer;
                                break;
                            case Y:
                                x = run;
                                y = plane;
                                z = outer;
                                break;
                            case Z:
                                x = run;
                                y = outer;
                                z = plane;
                                break;
                            default:
                                throw new AssertionError(face);
                        }

                        blob.visibleFace(face, x, y, z, visible, test);
                        if (!visible.visibleFace) {
                            current = null;
                            continue;
                        }

                        final FaceRegion region = new FaceRegion(
                                face,
                                x * 2 + 1 + face.getStepX(),
                                y * 2 + 1 + face.getStepY(),
                                z * 2 + 1 + face.getStepZ(),
                                visible.state,
                                visible.isEdge);
                        if (current == null || !current.extend(region)) {
                            current = region;
                            regions.add(region);
                        }
                    }
                }

                if (!regions.isEmpty()) {
                    result.add(regions);
                }
            }
        }

        return result;
    }

    private static void mergeAll(final ArrayList<ArrayList<FaceRegion>> regions) {
        for (final ArrayList<FaceRegion> plane : regions) {
            FaceRegion.merge(plane);
        }
    }

    private static void assertRegionsEqual(
            final ArrayList<ArrayList<FaceRegion>> expected,
            final ArrayList<ArrayList<FaceRegion>> actual,
            final String label) {
        require(expected.size() == actual.size(), label + " plane count differs");

        for (int plane = 0; plane < expected.size(); plane++) {
            final ArrayList<FaceRegion> expectedPlane = expected.get(plane);
            final ArrayList<FaceRegion> actualPlane = actual.get(plane);
            require(expectedPlane.size() == actualPlane.size(), label + " face count differs at plane " + plane);

            for (int face = 0; face < expectedPlane.size(); face++) {
                require(
                        signature(expectedPlane.get(face)).equals(signature(actualPlane.get(face))),
                        label + " differs at plane " + plane + ", face " + face);
            }
        }
    }

    private static String signature(final FaceRegion face) {
        return face.face
                + ":"
                + face.blockStateID
                + ":"
                + face.isEdge
                + ":"
                + face.getMinX()
                + ":"
                + face.getMinY()
                + ":"
                + face.getMinZ()
                + ":"
                + face.getMaxX()
                + ":"
                + face.getMaxY()
                + ":"
                + face.getMaxZ();
    }

    private static FaceRegion face(final int x, final int z) {
        return new FaceRegion(Direction.UP, x * 2 + 1, 2, z * 2 + 1, 1, true);
    }

    private static int area(final ArrayList<FaceRegion> faces) {
        int area = 0;
        for (final FaceRegion face : faces) {
            switch (face.face) {
                case EAST:
                case WEST:
                    area += ((face.getMaxY() - face.getMinY()) / 2 + 1) * ((face.getMaxZ() - face.getMinZ()) / 2 + 1);
                    break;
                case NORTH:
                case SOUTH:
                    area += ((face.getMaxX() - face.getMinX()) / 2 + 1) * ((face.getMaxY() - face.getMinY()) / 2 + 1);
                    break;
                default:
                    area += ((face.getMaxX() - face.getMinX()) / 2 + 1) * ((face.getMaxZ() - face.getMinZ()) / 2 + 1);
            }
        }
        return area;
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class OrderedCullTest implements ICullTest {
        private int calls;

        @Override
        public boolean isVisible(final int state, final int neighbor) {
            calls++;
            return state != 0 && state != neighbor && (neighbor == 0 || state < neighbor);
        }
    }

    private static final class CountingFaceRegion extends FaceRegion {
        private static int comparisons;

        private CountingFaceRegion(final Direction direction, final int centerX, final int centerY, final int centerZ) {
            super(direction, centerX, centerY, centerZ, 1, true);
        }

        @Override
        public boolean extend(final FaceRegion currentFace) {
            comparisons++;
            return super.extend(currentFace);
        }
    }
}
