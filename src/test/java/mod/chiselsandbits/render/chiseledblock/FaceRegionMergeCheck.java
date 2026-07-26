package mod.chiselsandbits.render.chiseledblock;

import java.util.ArrayList;
import net.minecraft.core.Direction;

public final class FaceRegionMergeCheck {
    private FaceRegionMergeCheck() {}

    public static void main(final String[] args) {
        checkFullPlane();
        checkEastPlane();
        checkStripedPlane();
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
