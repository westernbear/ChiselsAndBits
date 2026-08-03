package mod.chiselsandbits.render.chiseledblock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.culling.ICullTest;
import net.minecraft.core.Direction;

final class FaceRegionExtractor {
    private static final int MAX_FAST_STATES = 4;
    // ponytail: sparse surfaces are allocation-bound; retune only from an end-to-end client benchmark.
    private static final int MIN_FAST_OCCUPIED = VoxelBlob.full_size * 7 / 8;

    private FaceRegionExtractor() {}

    static ArrayList<ArrayList<FaceRegion>> extract(final VoxelBlob blob, final ICullTest test) {
        final ArrayList<ArrayList<FaceRegion>> fastResult = extractSingleState(blob, test);
        if (fastResult != null) {
            return fastResult;
        }

        final ArrayList<ArrayList<FaceRegion>> paletteResult = extractSmallPalette(blob, test);
        if (paletteResult != null) {
            return paletteResult;
        }

        return extractScalar(blob, test);
    }

    private static ArrayList<ArrayList<FaceRegion>> extractScalar(final VoxelBlob blob, final ICullTest test) {
        final ArrayList<ArrayList<FaceRegion>> result = new ArrayList<>();
        processXFaces(blob, test, result);
        processYFaces(blob, test, result);
        processZFaces(blob, test, result);
        return result;
    }

    private static ArrayList<ArrayList<FaceRegion>> extractSingleState(final VoxelBlob blob, final ICullTest test) {
        if (blob.detail != VoxelBlob.dim) {
            return null;
        }

        final int state = blob.getSingleNonAirState();
        if (state == 0) {
            return new ArrayList<>();
        }
        if (state < 0) {
            return null;
        }

        final BitSet occupied = blob.getNoneAir();
        final int occupiedCount = occupied.cardinality();

        final int dimension = blob.detail;
        final int[] xRows = new int[dimension * dimension];
        final int[] yzRows = new int[dimension * dimension];
        final long[] words = occupied.toLongArray();

        for (int row = 0; row < xRows.length; row++) {
            final int wordIndex = row >>> 2;
            if (wordIndex < words.length) {
                xRows[row] = (int) (words[wordIndex] >>> ((row & 3) << 4)) & 0xffff;
            }
        }

        for (int index = occupied.nextSetBit(0); index >= 0; index = occupied.nextSetBit(index + 1)) {
            final int x = index & 15;
            final int y = index >>> 4 & 15;
            final int z = index >>> 8 & 15;

            yzRows[x * dimension + z] |= 1 << y;
        }

        final boolean interiorVisible = occupiedCount == VoxelBlob.full_size || test.isVisible(state, 0);
        final ArrayList<ArrayList<FaceRegion>> result = new ArrayList<>();
        processSingleStateXFaces(yzRows, state, interiorVisible, result);
        processSingleStateYFaces(xRows, state, interiorVisible, result);
        processSingleStateZFaces(xRows, state, interiorVisible, result);
        return result;
    }

    private static ArrayList<ArrayList<FaceRegion>> extractSmallPalette(final VoxelBlob blob, final ICullTest test) {
        if (blob.detail != VoxelBlob.dim || blob.getNoneAir().cardinality() < MIN_FAST_OCCUPIED) {
            return null;
        }

        final int dimension = blob.detail;
        final int[] states = new int[MAX_FAST_STATES];
        final int[][] xRows = new int[MAX_FAST_STATES][dimension * dimension];
        final int[][] yzRows = new int[MAX_FAST_STATES][dimension * dimension];
        int stateCount = 0;

        for (int z = 0; z < dimension; z++) {
            for (int y = 0; y < dimension; y++) {
                for (int x = 0; x < dimension; x++) {
                    final int state = blob.get(x, y, z);
                    if (state == 0) {
                        continue;
                    }

                    int stateIndex = 0;
                    while (stateIndex < stateCount && states[stateIndex] != state) {
                        stateIndex++;
                    }
                    if (stateIndex == stateCount) {
                        if (stateCount == MAX_FAST_STATES) {
                            return null;
                        }
                        states[stateCount++] = state;
                    }

                    xRows[stateIndex][z * dimension + y] |= 1 << x;
                    yzRows[stateIndex][x * dimension + z] |= 1 << y;
                }
            }
        }

        if (stateCount < 2) {
            return null;
        }

        final byte[][] visibility = new byte[stateCount][stateCount + 1];
        final ArrayList<ArrayList<FaceRegion>> result = new ArrayList<>();
        processPaletteXFaces(yzRows, states, stateCount, test, visibility, result);
        processPaletteYFaces(xRows, states, stateCount, test, visibility, result);
        processPaletteZFaces(xRows, states, stateCount, test, visibility, result);
        return result;
    }

    private static void processPaletteXFaces(
            final int[][] rows,
            final int[] states,
            final int stateCount,
            final ICullTest test,
            final byte[][] visibility,
            final ArrayList<ArrayList<FaceRegion>> result) {
        final int dimension = VoxelBlob.dim;
        final ArrayList<FaceRegion>[] east = planes(dimension);
        final ArrayList<FaceRegion>[] west = planes(dimension);
        final int[] eastMasks = new int[stateCount];
        final int[] westMasks = new int[stateCount];

        for (int x = 0; x <= dimension; x++) {
            for (int z = 0; z < dimension; z++) {
                fillVisibleMaskPair(
                        rows,
                        x == 0 ? -1 : (x - 1) * dimension + z,
                        x == dimension ? -1 : x * dimension + z,
                        states,
                        stateCount,
                        test,
                        visibility,
                        eastMasks,
                        westMasks);
                if (x > 0) {
                    appendMasks(east, x - 1, Direction.EAST, z, eastMasks, states, stateCount, x == dimension);
                }
                if (x < dimension) {
                    appendMasks(west, x, Direction.WEST, z, westMasks, states, stateCount, x == 0);
                }
            }
        }

        appendPlanes(result, east);
        appendPlanes(result, west);
    }

    private static void processPaletteYFaces(
            final int[][] rows,
            final int[] states,
            final int stateCount,
            final ICullTest test,
            final byte[][] visibility,
            final ArrayList<ArrayList<FaceRegion>> result) {
        final int dimension = VoxelBlob.dim;
        final ArrayList<FaceRegion>[] up = planes(dimension);
        final ArrayList<FaceRegion>[] down = planes(dimension);
        final int[] upMasks = new int[stateCount];
        final int[] downMasks = new int[stateCount];

        for (int y = 0; y <= dimension; y++) {
            for (int z = 0; z < dimension; z++) {
                fillVisibleMaskPair(
                        rows,
                        y == 0 ? -1 : z * dimension + y - 1,
                        y == dimension ? -1 : z * dimension + y,
                        states,
                        stateCount,
                        test,
                        visibility,
                        upMasks,
                        downMasks);
                if (y > 0) {
                    appendMasks(up, y - 1, Direction.UP, z, upMasks, states, stateCount, y == dimension);
                }
                if (y < dimension) {
                    appendMasks(down, y, Direction.DOWN, z, downMasks, states, stateCount, y == 0);
                }
            }
        }

        appendPlanes(result, up);
        appendPlanes(result, down);
    }

    private static void processPaletteZFaces(
            final int[][] rows,
            final int[] states,
            final int stateCount,
            final ICullTest test,
            final byte[][] visibility,
            final ArrayList<ArrayList<FaceRegion>> result) {
        final int dimension = VoxelBlob.dim;
        final ArrayList<FaceRegion>[] south = planes(dimension);
        final ArrayList<FaceRegion>[] north = planes(dimension);
        final int[] southMasks = new int[stateCount];
        final int[] northMasks = new int[stateCount];

        for (int z = 0; z <= dimension; z++) {
            for (int y = 0; y < dimension; y++) {
                fillVisibleMaskPair(
                        rows,
                        z == 0 ? -1 : (z - 1) * dimension + y,
                        z == dimension ? -1 : z * dimension + y,
                        states,
                        stateCount,
                        test,
                        visibility,
                        southMasks,
                        northMasks);
                if (z > 0) {
                    appendMasks(south, z - 1, Direction.SOUTH, y, southMasks, states, stateCount, z == dimension);
                }
                if (z < dimension) {
                    appendMasks(north, z, Direction.NORTH, y, northMasks, states, stateCount, z == 0);
                }
            }
        }

        appendPlanes(result, south);
        appendPlanes(result, north);
    }

    private static void fillVisibleMaskPair(
            final int[][] rows,
            final int lowerRow,
            final int upperRow,
            final int[] states,
            final int stateCount,
            final ICullTest test,
            final byte[][] visibility,
            final int[] positiveMasks,
            final int[] negativeMasks) {
        if (lowerRow < 0) {
            for (int state = 0; state < stateCount; state++) {
                negativeMasks[state] = rows[state][upperRow];
            }
            return;
        }
        if (upperRow < 0) {
            for (int state = 0; state < stateCount; state++) {
                positiveMasks[state] = rows[state][lowerRow];
            }
            return;
        }

        int lowerOccupied = 0;
        int upperOccupied = 0;
        for (int state = 0; state < stateCount; state++) {
            lowerOccupied |= rows[state][lowerRow];
            upperOccupied |= rows[state][upperRow];
        }

        fillVisibleMasks(rows, lowerRow, upperRow, upperOccupied, states, stateCount, test, visibility, positiveMasks);
        fillVisibleMasks(rows, upperRow, lowerRow, lowerOccupied, states, stateCount, test, visibility, negativeMasks);
    }

    private static void fillVisibleMasks(
            final int[][] rows,
            final int row,
            final int neighborRow,
            final int neighborOccupied,
            final int[] states,
            final int stateCount,
            final ICullTest test,
            final byte[][] visibility,
            final int[] masks) {
        for (int state = 0; state < stateCount; state++) {
            final int current = rows[state][row];
            if (current == 0) {
                masks[state] = 0;
                continue;
            }

            int mask = 0;
            final int air = current & ~neighborOccupied;
            if (air != 0 && isVisible(test, visibility, states, state, stateCount)) {
                mask = air;
            }

            for (int neighbor = 0; neighbor < stateCount; neighbor++) {
                if (neighbor == state) {
                    continue;
                }

                final int candidates = current & rows[neighbor][neighborRow];
                if (candidates != 0 && isVisible(test, visibility, states, state, neighbor)) {
                    mask |= candidates;
                }
            }
            masks[state] = mask;
        }
    }

    private static boolean isVisible(
            final ICullTest test, final byte[][] visibility, final int[] states, final int state, final int neighbor) {
        byte result = visibility[state][neighbor];
        if (result == 0) {
            result = (byte)
                    (test.isVisible(states[state], neighbor == visibility[state].length - 1 ? 0 : states[neighbor])
                            ? 2
                            : 1);
            visibility[state][neighbor] = result;
        }
        return result == 2;
    }

    private static void appendMasks(
            final ArrayList<FaceRegion>[] planes,
            final int plane,
            final Direction face,
            final int outer,
            final int[] masks,
            final int[] states,
            final int stateCount,
            final boolean edge) {
        int remaining = 0;
        for (int state = 0; state < stateCount; state++) {
            remaining |= masks[state];
        }

        while (remaining != 0) {
            final int start = Integer.numberOfTrailingZeros(remaining);
            final int bit = 1 << start;
            int state = 0;
            while ((masks[state] & bit) == 0) {
                state++;
            }

            final int length = Integer.numberOfTrailingZeros(~(masks[state] >>> start));
            final int runMask = ((1 << length) - 1) << start;
            appendMask(planes, plane, face, outer, runMask, states[state], edge);
            remaining &= ~runMask;
        }
    }

    private static void processSingleStateXFaces(
            final int[] rows,
            final int state,
            final boolean interiorVisible,
            final ArrayList<ArrayList<FaceRegion>> result) {
        final int dimension = VoxelBlob.dim;
        final ArrayList<FaceRegion>[] east = planes(dimension);
        final ArrayList<FaceRegion>[] west = planes(dimension);

        for (int x = 0; x < dimension; x++) {
            final boolean eastEdge = x == dimension - 1;
            final boolean westEdge = x == 0;

            for (int z = 0; z < dimension; z++) {
                final int current = rows[x * dimension + z];
                final int eastMask =
                        eastEdge ? current : interiorVisible ? current & ~rows[(x + 1) * dimension + z] : 0;
                final int westMask =
                        westEdge ? current : interiorVisible ? current & ~rows[(x - 1) * dimension + z] : 0;

                appendMask(east, x, Direction.EAST, z, eastMask, state, eastEdge);
                appendMask(west, x, Direction.WEST, z, westMask, state, westEdge);
            }
        }

        appendPlanes(result, east);
        appendPlanes(result, west);
    }

    private static void processSingleStateYFaces(
            final int[] rows,
            final int state,
            final boolean interiorVisible,
            final ArrayList<ArrayList<FaceRegion>> result) {
        final int dimension = VoxelBlob.dim;
        final ArrayList<FaceRegion>[] up = planes(dimension);
        final ArrayList<FaceRegion>[] down = planes(dimension);

        for (int y = 0; y < dimension; y++) {
            final boolean upEdge = y == dimension - 1;
            final boolean downEdge = y == 0;

            for (int z = 0; z < dimension; z++) {
                final int current = rows[z * dimension + y];
                final int upMask = upEdge ? current : interiorVisible ? current & ~rows[z * dimension + y + 1] : 0;
                final int downMask = downEdge ? current : interiorVisible ? current & ~rows[z * dimension + y - 1] : 0;

                appendMask(up, y, Direction.UP, z, upMask, state, upEdge);
                appendMask(down, y, Direction.DOWN, z, downMask, state, downEdge);
            }
        }

        appendPlanes(result, up);
        appendPlanes(result, down);
    }

    private static void processSingleStateZFaces(
            final int[] rows,
            final int state,
            final boolean interiorVisible,
            final ArrayList<ArrayList<FaceRegion>> result) {
        final int dimension = VoxelBlob.dim;
        final ArrayList<FaceRegion>[] south = planes(dimension);
        final ArrayList<FaceRegion>[] north = planes(dimension);

        for (int z = 0; z < dimension; z++) {
            final boolean southEdge = z == dimension - 1;
            final boolean northEdge = z == 0;

            for (int y = 0; y < dimension; y++) {
                final int current = rows[z * dimension + y];
                final int southMask =
                        southEdge ? current : interiorVisible ? current & ~rows[(z + 1) * dimension + y] : 0;
                final int northMask =
                        northEdge ? current : interiorVisible ? current & ~rows[(z - 1) * dimension + y] : 0;

                appendMask(south, z, Direction.SOUTH, y, southMask, state, southEdge);
                appendMask(north, z, Direction.NORTH, y, northMask, state, northEdge);
            }
        }

        appendPlanes(result, south);
        appendPlanes(result, north);
    }

    private static void appendMask(
            final ArrayList<FaceRegion>[] planes,
            final int plane,
            final Direction face,
            final int outer,
            int mask,
            final int state,
            final boolean edge) {
        ArrayList<FaceRegion> faces = null;

        while (mask != 0) {
            final int start = Integer.numberOfTrailingZeros(mask);
            final int length = Integer.numberOfTrailingZeros(~(mask >>> start));
            final int runMask = ((1 << length) - 1) << start;
            final int x;
            final int y;
            final int z;

            switch (face.getAxis()) {
                case X:
                    x = plane;
                    y = start;
                    z = outer;
                    break;
                case Y:
                    x = start;
                    y = plane;
                    z = outer;
                    break;
                case Z:
                    x = start;
                    y = outer;
                    z = plane;
                    break;
                default:
                    throw new AssertionError(face);
            }

            if (faces == null) {
                faces = planes[plane];
                if (faces == null) {
                    faces = new ArrayList<>(16);
                    planes[plane] = faces;
                }
            }

            faces.add(FaceRegion.createRow(
                    face,
                    x * 2 + 1 + face.getStepX(),
                    y * 2 + 1 + face.getStepY(),
                    z * 2 + 1 + face.getStepZ(),
                    state,
                    edge,
                    length));
            mask &= ~runMask;
        }
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
