package mod.chiselsandbits.render.chiseledblock;

import com.mojang.math.Quadrant;
import java.util.function.Function;
import java.util.function.Predicate;
import mod.chiselsandbits.chiseledblock.data.VoxelType;
import mod.chiselsandbits.render.helpers.ModelQuadLayer;
import mod.chiselsandbits.render.helpers.ModelUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Turns {@link ShapeMesh} rectangles into baked quads, one per texture layer of the bit's block face. */
public final class ChiseledEmitter {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Direction[] SIDES = new Direction[ShapeMesh.SIDES];
    private static final ChunkSectionLayer[] LAYERS = ChunkSectionLayer.values();
    private static final ModelQuadLayer[] NO_LAYERS = new ModelQuadLayer[0];
    /** Every corner a quad can have: bits sit on a 1/16 grid, so all quads share these 17^3 positions. */
    private static final Vector3fc[] GRID = new Vector3fc[17 * 17 * 17];

    static {
        for (final Direction direction : Direction.values()) {
            SIDES[direction.get3DDataValue()] = direction;
        }
        for (int x = 0; x <= 16; x++) {
            for (int y = 0; y <= 16; y++) {
                for (int z = 0; z <= 16; z++) {
                    GRID[(x * 17 + y) * 17 + z] = new Vector3f(x / 16.0f, y / 16.0f, z / 16.0f);
                }
            }
        }
    }

    private ChiseledEmitter() {}

    /** Receives each emitted quad; {@code cullFace} is the block side an edge quad lies on, null inside the block. */
    public interface Sink {
        void accept(BakedQuad quad, @Nullable Direction cullFace, ChunkSectionLayer layer);
    }

    /**
     * Emits a placed block in every layer its bits use.
     *
     * @param neighbours the mesh of the chiseled block beside a side, or null; its opaque bits hide our edge cells
     * @param culled true when vanilla culling hides a whole side
     */
    public static void emitWorld(
            final ShapeMesh mesh,
            final Function<Direction, @Nullable ShapeMesh> neighbours,
            final Predicate<Direction> culled,
            final Sink sink) {
        final Emission emission = new Emission(mesh, null, sink);
        emission.rects(mesh.rects, null);

        for (int side = 0; side < ShapeMesh.SIDES; side++) {
            if (mesh.edgeRects[side] == null) {
                continue;
            }

            final Direction direction = SIDES[side];
            if (culled.test(direction)) {
                continue;
            }

            final ShapeMesh neighbour = neighbours.apply(direction);
            final short[] covered = neighbour == null
                    ? null
                    : neighbour.opaqueRows[direction.getOpposite().get3DDataValue()];
            final long[] rects = ShapeMesher.coveredEdgeRects(mesh, side, covered);
            if (rects != null) {
                emission.rects(rects, direction);
            }
        }
    }

    /** Emits the bits of one legacy render type with every side uncovered, for items, previews and storage. */
    public static void emitType(final ShapeMesh mesh, final ChiselRenderType type, final Sink sink) {
        final Emission emission = new Emission(mesh, type, sink);
        emission.rects(mesh.rects, null);
        for (int side = 0; side < ShapeMesh.SIDES; side++) {
            if (mesh.edgeRects[side] != null) {
                emission.rects(mesh.edgeRects[side], SIDES[side]);
            }
        }
    }

    private static final class Emission {
        private final ShapeMesh mesh;
        private final @Nullable ChiselRenderType type;
        private final Sink sink;
        /** Face layers per (entry, face, layer), resolved once per emission. */
        private final ModelQuadLayer[][] faceLayers;

        private final int[] from = new int[3];
        private final int[] to = new int[3];
        private final float[] uvs = new float[8];
        private final Vector3fc[] positions = new Vector3fc[4];
        private final long[] packedUvs = new long[4];

        Emission(final ShapeMesh mesh, final @Nullable ChiselRenderType type, final Sink sink) {
            this.mesh = mesh;
            this.type = type;
            this.sink = sink;
            faceLayers = new ModelQuadLayer[mesh.states.length * ShapeMesh.SIDES * LAYERS.length][];
        }

        void rects(final long[] rects, final @Nullable Direction cullFace) {
            for (final long rect : rects) {
                rect(rect, cullFace);
            }
        }

        private void rect(final long rect, final @Nullable Direction cullFace) {
            final int entry = ShapeMesh.entry(rect);
            final int flags = mesh.flags[entry];
            if (type != null && !matches(type, flags)) {
                return;
            }

            final Direction face = SIDES[ShapeMesh.side(rect)];
            bounds(rect, face);

            for (final ChunkSectionLayer layer : LAYERS) {
                if ((flags & 1 << layer.ordinal()) == 0 || type != null && layer != type.layer) {
                    continue;
                }

                for (final ModelQuadLayer source : layers(entry, face, layer)) {
                    sink.accept(quad(source, mesh.states[entry], face, layer), cullFace, layer);
                }
            }
        }

        private ModelQuadLayer[] layers(final int entry, final Direction face, final ChunkSectionLayer layer) {
            final int index = (entry * ShapeMesh.SIDES + face.get3DDataValue()) * LAYERS.length + layer.ordinal();
            ModelQuadLayer[] resolved = faceLayers[index];
            if (resolved == null) {
                resolved = ModelUtil.getCachedFace(mesh.states[entry], RANDOM, face, layer);
                faceLayers[index] = resolved = resolved == null ? NO_LAYERS : resolved;
            }
            return resolved;
        }

        /** Pixel-space corners of the rectangle, matching the old region min/max conversion. */
        private void bounds(final long rect, final Direction face) {
            final int depth =
                    ShapeMesh.plane(rect) + (face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0);
            final int run = ShapeMesh.run(rect);
            final int outer = ShapeMesh.outer(rect);
            final int runEnd = run + ShapeMesh.width(rect);
            final int outerEnd = outer + ShapeMesh.height(rect);

            switch (face.getAxis()) {
                case X -> set(depth, run, outer, depth, runEnd, outerEnd);
                case Y -> set(run, depth, outer, runEnd, depth, outerEnd);
                case Z -> set(run, outer, depth, runEnd, outerEnd, depth);
            }
        }

        private void set(final int fx, final int fy, final int fz, final int tx, final int ty, final int tz) {
            from[0] = fx;
            from[1] = fy;
            from[2] = fz;
            to[0] = tx;
            to[1] = ty;
            to[2] = tz;
        }

        private BakedQuad quad(
                final ModelQuadLayer source, final int stateId, final Direction face, final ChunkSectionLayer layer) {
            final int packedTint = 0 <= source.tint && source.tint <= 0xff ? (stateId << 8) | source.tint : source.tint;
            faceUvs(face, source.uvs);

            final int[] vertMap = Tables.FACE_VERT_MAP[face.get3DDataValue()];
            for (int vertex = 0; vertex < 4; vertex++) {
                final float[] mapping = Tables.QUAD_MAPPING[face.ordinal()][vertex];
                positions[vertex] = grid(
                        to[0] * mapping[0] + from[0] * mapping[1],
                        to[1] * mapping[2] + from[1] * mapping[3],
                        to[2] * mapping[4] + from[2] * mapping[5]);

                final int uvIndex = vertMap[vertex] * 2;
                packedUvs[vertex] =
                        UVPair.pack(source.sprite.getU(uvs[uvIndex] / 16f), source.sprite.getV(uvs[uvIndex + 1] / 16f));
            }

            return new BakedQuad(
                    positions[0],
                    positions[1],
                    positions[2],
                    positions[3],
                    packedUvs[0],
                    packedUvs[1],
                    packedUvs[2],
                    packedUvs[3],
                    face,
                    source.material(layer, packedTint));
        }

        private static Vector3fc grid(final float x, final float y, final float z) {
            return GRID[(Math.round(x * 16) * 17 + Math.round(y * 16)) * 17 + Math.round(z * 16)];
        }

        private void faceUvs(final Direction face, final float[] quadsUV) {
            final float toU;
            final float toV;
            final float fromU;
            final float fromV;

            switch (face) {
                case UP, DOWN -> {
                    toU = to[0] / 16.0f;
                    toV = to[2] / 16.0f;
                    fromU = from[0] / 16.0f;
                    fromV = from[2] / 16.0f;
                }
                case SOUTH, NORTH -> {
                    toU = to[0] / 16.0f;
                    toV = to[1] / 16.0f;
                    fromU = from[0] / 16.0f;
                    fromV = from[1] / 16.0f;
                }
                default -> {
                    toU = to[1] / 16.0f;
                    toV = to[2] / 16.0f;
                    fromU = from[1] / 16.0f;
                    fromV = from[2] / 16.0f;
                }
            }

            uvs[0] = 16.0f * u(quadsUV, toU, toV);
            uvs[1] = 16.0f * v(quadsUV, toU, toV);

            uvs[2] = 16.0f * u(quadsUV, fromU, toV);
            uvs[3] = 16.0f * v(quadsUV, fromU, toV);

            uvs[4] = 16.0f * u(quadsUV, fromU, fromV);
            uvs[5] = 16.0f * v(quadsUV, fromU, fromV);

            uvs[6] = 16.0f * u(quadsUV, toU, fromV);
            uvs[7] = 16.0f * v(quadsUV, toU, fromV);
        }

        private static float u(final float[] src, final float inU, final float inV) {
            final float inv = 1.0f - inU;
            final float u1 = src[0] * inU + inv * src[2];
            final float u2 = src[4] * inU + inv * src[6];
            return u1 * inV + (1.0f - inV) * u2;
        }

        private static float v(final float[] src, final float inU, final float inV) {
            final float inv = 1.0f - inU;
            final float v1 = src[1] * inU + inv * src[3];
            final float v2 = src[5] * inU + inv * src[7];
            return v1 * inV + (1.0f - inV) * v2;
        }
    }

    /** Same filter as {@link ChiselRenderType#filter}, applied to one palette entry. */
    private static boolean matches(final ChiselRenderType type, final int flags) {
        if ((flags & 1 << type.layer.ordinal()) == 0) {
            return false;
        }
        return type.type == null || ((flags & ShapeMesher.FLAG_FLUID) != 0) == (type.type == VoxelType.FLUID);
    }

    /** Vertex order and corner mapping of vanilla's baked cube faces, so chiseled quads light like vanilla ones. */
    private static final class Tables {
        static final int[][] FACE_VERT_MAP = new int[6][4];
        static final float[][][] QUAD_MAPPING = new float[6][4][6];

        static {
            final Vector3f to = new Vector3f(0, 0, 0);
            final Vector3f from = new Vector3f(16, 16, 16);

            for (final Direction myFace : Direction.values()) {
                final TextureAtlasSprite texture = Minecraft.getInstance()
                        .getAtlasManager()
                        .getAtlasOrThrow(AtlasIds.BLOCKS)
                        .getSprite(Identifier.parse("missingno"));
                final ModelBaker.Interner interner = new ModelBaker.Interner() {
                    @Override
                    public Vector3fc vector(final Vector3fc value) {
                        return value;
                    }

                    @Override
                    public BakedQuad.MaterialInfo materialInfo(final BakedQuad.MaterialInfo value) {
                        return value;
                    }
                };
                final BakedQuad.MaterialInfo materialInfo = BakedQuad.MaterialInfo.of(
                        new Material.Baked(texture, false), texture.transparency(), 0, true, 0);
                final BakedQuad q = FaceBakery.bakeQuad(
                        interner,
                        to,
                        from,
                        new CuboidFace.UVs(0, 0, 1, 1),
                        Quadrant.R0,
                        materialInfo,
                        myFace,
                        BlockModelRotation.IDENTITY,
                        null);

                int a = 0;
                int b = 2;

                switch (myFace) {
                    case NORTH:
                    case SOUTH:
                        a = 0;
                        b = 1;
                        break;
                    case EAST:
                    case WEST:
                        a = 1;
                        b = 2;
                        break;
                    default:
                }

                for (int vertNum = 0; vertNum < 4; vertNum++) {
                    final Vector3fc vertex = q.position(vertNum);
                    final float A = vertex.get(a); // Current material
                    final float B = vertex.get(b); // Neighbor material

                    for (int o = 0; o < 3; o++) {
                        final float v = vertex.get(o);
                        final float scaler = 1.0f / 16.0f; // pos start in the 0-16
                        QUAD_MAPPING[myFace.ordinal()][vertNum][o * 2] = v * scaler;
                        QUAD_MAPPING[myFace.ordinal()][vertNum][o * 2 + 1] = (1.0f - v) * scaler;
                    }
                    if (ModelUtil.isZero(A) && ModelUtil.isZero(B)) {
                        FACE_VERT_MAP[myFace.get3DDataValue()][vertNum] = 0;
                    } else if (ModelUtil.isZero(A) && ModelUtil.isOne(B)) {
                        FACE_VERT_MAP[myFace.get3DDataValue()][vertNum] = 3;
                    } else if (ModelUtil.isOne(A) && ModelUtil.isZero(B)) {
                        FACE_VERT_MAP[myFace.get3DDataValue()][vertNum] = 1;
                    } else {
                        FACE_VERT_MAP[myFace.get3DDataValue()][vertNum] = 2;
                    }
                }
            }
        }
    }
}
