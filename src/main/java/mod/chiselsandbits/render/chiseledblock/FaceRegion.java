package mod.chiselsandbits.render.chiseledblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;

public class FaceRegion {
    public final Direction face;
    final int blockStateID;
    final boolean isEdge;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;

    public FaceRegion(
            final Direction myFace,
            final int centerX,
            final int centerY,
            final int centerZ,
            final int blockStateID,
            final boolean isEdgeFace) {
        face = myFace;
        this.blockStateID = blockStateID;
        isEdge = isEdgeFace;
        minX = centerX;
        minY = centerY;
        minZ = centerZ;
        maxX = centerX;
        maxY = centerY;
        maxZ = centerZ;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getStateId() {
        return blockStateID;
    }

    static void merge(final ArrayList<FaceRegion> faces) {
        // Each list contains row-major runs from one face plane.
        final Map<Long, FaceRegion> active = new HashMap<>(faces.size());
        int writeIndex = 0;

        for (final FaceRegion face : faces) {
            final long key = face.mergeKey();
            final FaceRegion previous = active.get(key);

            if (previous == null || !previous.extend(face)) {
                active.put(key, face);
                faces.set(writeIndex++, face);
            }
        }

        faces.subList(writeIndex, faces.size()).clear();
    }

    private long mergeKey() {
        final int min;
        final int max;

        switch (face) {
            case EAST:
            case WEST:
                min = minY;
                max = maxY;
                break;
            default:
                min = minX;
                max = maxX;
        }

        return ((long) blockStateID << 32) | ((long) min << 16) | max;
    }

    boolean extendRow(final int centerX, final int centerY, final int centerZ, final int state) {
        if (state != blockStateID) {
            return false;
        }

        switch (face) {
            case EAST:
            case WEST:
                if (maxY != centerY - 2 || minX != centerX || maxX != centerX || minZ != centerZ || maxZ != centerZ) {
                    return false;
                }
                maxY = centerY;
                return true;
            default:
                if (maxX != centerX - 2 || minY != centerY || maxY != centerY || minZ != centerZ || maxZ != centerZ) {
                    return false;
                }
                maxX = centerX;
                return true;
        }
    }

    public boolean extend(final FaceRegion currentFace) {
        if (currentFace.blockStateID != blockStateID) {
            return false;
        }

        switch (face) {
            case DOWN:
            case UP: {
                final boolean a = maxX == currentFace.minX - 2 && maxZ == currentFace.maxZ && minZ == currentFace.minZ;
                final boolean b = minX == currentFace.maxX + 2 && maxZ == currentFace.maxZ && minZ == currentFace.minZ;
                final boolean c = maxZ == currentFace.minZ - 2 && maxX == currentFace.maxX && minX == currentFace.minX;
                final boolean d = minZ == currentFace.maxZ + 2 && maxX == currentFace.maxX && minX == currentFace.minX;

                if (a || b || c || d) {
                    minX = Math.min(currentFace.minX, minX);
                    minY = Math.min(currentFace.minY, minY);
                    minZ = Math.min(currentFace.minZ, minZ);
                    maxX = Math.max(currentFace.maxX, maxX);
                    maxY = Math.max(currentFace.maxY, maxY);
                    maxZ = Math.max(currentFace.maxZ, maxZ);
                    return true;
                }

                return false;
            }

            case WEST:
            case EAST: {
                final boolean a = maxY == currentFace.minY - 2 && maxZ == currentFace.maxZ && minZ == currentFace.minZ;
                final boolean b = minY == currentFace.maxY + 2 && maxZ == currentFace.maxZ && minZ == currentFace.minZ;
                final boolean c = maxZ == currentFace.minZ - 2 && maxY == currentFace.maxY && minY == currentFace.minY;
                final boolean d = minZ == currentFace.maxZ + 2 && maxY == currentFace.maxY && minY == currentFace.minY;

                if (a || b || c || d) {
                    minX = Math.min(currentFace.minX, minX);
                    minY = Math.min(currentFace.minY, minY);
                    minZ = Math.min(currentFace.minZ, minZ);
                    maxX = Math.max(currentFace.maxX, maxX);
                    maxY = Math.max(currentFace.maxY, maxY);
                    maxZ = Math.max(currentFace.maxZ, maxZ);
                    return true;
                }

                return false;
            }

            case NORTH:
            case SOUTH: {
                final boolean a = maxY == currentFace.minY - 2 && maxX == currentFace.maxX && minX == currentFace.minX;
                final boolean b = minY == currentFace.maxY + 2 && maxX == currentFace.maxX && minX == currentFace.minX;
                final boolean c = maxX == currentFace.minX - 2 && maxY == currentFace.maxY && minY == currentFace.minY;
                final boolean d = minX == currentFace.maxX + 2 && maxY == currentFace.maxY && minY == currentFace.minY;

                if (a || b || c || d) {
                    minX = Math.min(currentFace.minX, minX);
                    minY = Math.min(currentFace.minY, minY);
                    minZ = Math.min(currentFace.minZ, minZ);
                    maxX = Math.max(currentFace.maxX, maxX);
                    maxY = Math.max(currentFace.maxY, maxY);
                    maxZ = Math.max(currentFace.maxZ, maxZ);
                    return true;
                }

                return false;
            }

            default:
                return false;
        }
    }
}
