package mod.chiselsandbits.render.chiseledblock;

import java.util.ArrayList;
import java.util.Arrays;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.culling.ICullTest;
import net.minecraft.core.Direction;

final class FaceRegionExtractor {

    private FaceRegionExtractor() {}

    static ArrayList<ArrayList<FaceRegion>> extract(final VoxelBlob blob, final ICullTest test) {
        final ArrayList<ArrayList<FaceRegion>> result = new ArrayList<>();
        processXFaces(blob, test, result);
        processYFaces(blob, test, result);
        processZFaces(blob, test, result);
        return result;
    }

    private static void processXFaces(
            final VoxelBlob blob, final ICullTest test, final ArrayList<ArrayList<FaceRegion>> result) {
        final int dimension = blob.detail;
        final ArrayList<FaceRegion>[] east = planes(dimension);
        final ArrayList<FaceRegion>[] west = planes(dimension);
        final FaceRegion[] eastRuns = new FaceRegion[dimension];
        final FaceRegion[] westRuns = new FaceRegion[dimension];

        for (int z = 0; z < dimension; z++) {
            Arrays.fill(eastRuns, null);
            Arrays.fill(westRuns, null);

            for (int y = 0; y < dimension; y++) {
                int previous = 0;

                for (int x = 0; x <= dimension; x++) {
                    final int current = x == dimension ? 0 : blob.get(x, y, z);

                    if (x > 0) {
                        final int plane = x - 1;
                        final boolean edge = x == dimension;
                        if (edge ? previous != 0 : isVisible(test, previous, current)) {
                            append(east, eastRuns, plane, Direction.EAST, x - 1, y, z, previous, edge);
                        } else {
                            eastRuns[plane] = null;
                        }
                    }

                    if (x < dimension) {
                        final int plane = x;
                        final boolean edge = x == 0;
                        if (edge ? current != 0 : isVisible(test, current, previous)) {
                            append(west, westRuns, plane, Direction.WEST, x, y, z, current, edge);
                        } else {
                            westRuns[plane] = null;
                        }
                    }

                    previous = current;
                }
            }
        }

        appendPlanes(result, east);
        appendPlanes(result, west);
    }

    private static void processYFaces(
            final VoxelBlob blob, final ICullTest test, final ArrayList<ArrayList<FaceRegion>> result) {
        final int dimension = blob.detail;
        final ArrayList<FaceRegion>[] up = planes(dimension);
        final ArrayList<FaceRegion>[] down = planes(dimension);
        final FaceRegion[] upRuns = new FaceRegion[dimension];
        final FaceRegion[] downRuns = new FaceRegion[dimension];

        for (int z = 0; z < dimension; z++) {
            Arrays.fill(upRuns, null);
            Arrays.fill(downRuns, null);

            for (int x = 0; x < dimension; x++) {
                int previous = 0;

                for (int y = 0; y <= dimension; y++) {
                    final int current = y == dimension ? 0 : blob.get(x, y, z);

                    if (y > 0) {
                        final int plane = y - 1;
                        final boolean edge = y == dimension;
                        if (edge ? previous != 0 : isVisible(test, previous, current)) {
                            append(up, upRuns, plane, Direction.UP, x, y - 1, z, previous, edge);
                        } else {
                            upRuns[plane] = null;
                        }
                    }

                    if (y < dimension) {
                        final int plane = y;
                        final boolean edge = y == 0;
                        if (edge ? current != 0 : isVisible(test, current, previous)) {
                            append(down, downRuns, plane, Direction.DOWN, x, y, z, current, edge);
                        } else {
                            downRuns[plane] = null;
                        }
                    }

                    previous = current;
                }
            }
        }

        appendPlanes(result, up);
        appendPlanes(result, down);
    }

    private static void processZFaces(
            final VoxelBlob blob, final ICullTest test, final ArrayList<ArrayList<FaceRegion>> result) {
        final int dimension = blob.detail;
        final ArrayList<FaceRegion>[] south = planes(dimension);
        final ArrayList<FaceRegion>[] north = planes(dimension);
        final FaceRegion[] southRuns = new FaceRegion[dimension];
        final FaceRegion[] northRuns = new FaceRegion[dimension];

        for (int y = 0; y < dimension; y++) {
            Arrays.fill(southRuns, null);
            Arrays.fill(northRuns, null);

            for (int x = 0; x < dimension; x++) {
                int previous = 0;

                for (int z = 0; z <= dimension; z++) {
                    final int current = z == dimension ? 0 : blob.get(x, y, z);

                    if (z > 0) {
                        final int plane = z - 1;
                        final boolean edge = z == dimension;
                        if (edge ? previous != 0 : isVisible(test, previous, current)) {
                            append(south, southRuns, plane, Direction.SOUTH, x, y, z - 1, previous, edge);
                        } else {
                            southRuns[plane] = null;
                        }
                    }

                    if (z < dimension) {
                        final int plane = z;
                        final boolean edge = z == 0;
                        if (edge ? current != 0 : isVisible(test, current, previous)) {
                            append(north, northRuns, plane, Direction.NORTH, x, y, z, current, edge);
                        } else {
                            northRuns[plane] = null;
                        }
                    }

                    previous = current;
                }
            }
        }

        appendPlanes(result, south);
        appendPlanes(result, north);
    }

    private static boolean isVisible(final ICullTest test, final int state, final int neighbor) {
        return state != 0 && state != neighbor && test.isVisible(state, neighbor);
    }

    private static void append(
            final ArrayList<FaceRegion>[] planes,
            final FaceRegion[] runs,
            final int plane,
            final Direction face,
            final int x,
            final int y,
            final int z,
            final int state,
            final boolean edge) {
        final int centerX = x * 2 + 1 + face.getStepX();
        final int centerY = y * 2 + 1 + face.getStepY();
        final int centerZ = z * 2 + 1 + face.getStepZ();
        final FaceRegion current = runs[plane];

        if (current != null && current.extendRow(centerX, centerY, centerZ, state)) {
            return;
        }

        final FaceRegion region = new FaceRegion(face, centerX, centerY, centerZ, state, edge);
        ArrayList<FaceRegion> faces = planes[plane];
        if (faces == null) {
            faces = new ArrayList<>(16);
            planes[plane] = faces;
        }
        faces.add(region);
        runs[plane] = region;
    }

    private static void appendPlanes(
            final ArrayList<ArrayList<FaceRegion>> result, final ArrayList<FaceRegion>[] planes) {
        for (final ArrayList<FaceRegion> faces : planes) {
            if (faces != null) {
                result.add(faces);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<FaceRegion>[] planes(final int dimension) {
        return (ArrayList<FaceRegion>[]) new ArrayList<?>[dimension];
    }
}
