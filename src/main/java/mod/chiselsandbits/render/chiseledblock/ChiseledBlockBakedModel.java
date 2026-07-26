package mod.chiselsandbits.render.chiseledblock;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob.VisibleFace;
import mod.chiselsandbits.client.culling.ICullTest;
import mod.chiselsandbits.client.model.baked.BaseBakedBlockModel;
import mod.chiselsandbits.client.model.data.IModelData;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.render.helpers.BakedQuadBuilder;
import mod.chiselsandbits.render.helpers.ModelQuadLayer;
import mod.chiselsandbits.render.helpers.ModelUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class ChiseledBlockBakedModel extends BaseBakedBlockModel {
    public static final float PIXELS_PER_BLOCK = 16.0f;
    private static final RandomSource RANDOM = RandomSource.create();
    private static final int[][] faceVertMap = new int[6][4];
    private static final float[][][] quadMapping = new float[6][4][6];

    private static final Direction[] X_Faces = new Direction[] {Direction.EAST, Direction.WEST};
    private static final Direction[] Y_Faces = new Direction[] {Direction.UP, Direction.DOWN};
    private static final Direction[] Z_Faces = new Direction[] {Direction.SOUTH, Direction.NORTH};

    // Analyze FaceBakery / makeBakedQuad and prepare static data for face gen.
    static {
        final Vector3f to = new Vector3f(0, 0, 0);
        final Vector3f from = new Vector3f(16, 16, 16);

        for (final Direction myFace : Direction.values()) {
            final FaceBakery faceBakery = new FaceBakery();

            final BlockElementRotation bpr = null;
            final BlockModelRotation mr = BlockModelRotation.X0_Y0;

            final float[] defUVs = new float[] {0, 0, 1, 1};
            final BlockFaceUV uv = new BlockFaceUV(defUVs, 0);
            final BlockElementFace bpf = new BlockElementFace(myFace, 0, "", uv);

            final TextureAtlasSprite texture = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(new ResourceLocation("missingno"));
            final BakedQuad q = faceBakery.bakeQuad(
                    to,
                    from,
                    bpf,
                    texture,
                    myFace,
                    mr,
                    bpr,
                    true,
                    new ResourceLocation(ChiselsAndBits.MODID, "chiseled_block"));

            final int[] vertData = q.getVertices();

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

            final int p = vertData.length / 4;
            for (int vertNum = 0; vertNum < 4; vertNum++) {
                final float A = Float.intBitsToFloat(vertData[vertNum * p + a]); // Current material
                final float B = Float.intBitsToFloat(vertData[vertNum * p + b]); // Neighbor material

                for (int o = 0; o < 3; o++) {
                    final float v = Float.intBitsToFloat(vertData[vertNum * p + o]);
                    final float scaler = 1.0f / 16.0f; // pos start in the 0-16
                    quadMapping[myFace.ordinal()][vertNum][o * 2] = v * scaler;
                    quadMapping[myFace.ordinal()][vertNum][o * 2 + 1] = (1.0f - v) * scaler;
                }
                if (ModelUtil.isZero(A) && ModelUtil.isZero(B)) {
                    faceVertMap[myFace.get3DDataValue()][vertNum] = 0;
                } else if (ModelUtil.isZero(A) && ModelUtil.isOne(B)) {
                    faceVertMap[myFace.get3DDataValue()][vertNum] = 3;
                } else if (ModelUtil.isOne(A) && ModelUtil.isZero(B)) {
                    faceVertMap[myFace.get3DDataValue()][vertNum] = 1;
                } else {
                    faceVertMap[myFace.get3DDataValue()][vertNum] = 2;
                }
            }
        }
    }

    private ChiselRenderType myLayer;
    private TextureAtlasSprite sprite;

    // keep memory requirements low by using arrays.
    private BakedQuad[] up;
    private BakedQuad[] down;
    private BakedQuad[] north;
    private BakedQuad[] south;
    private BakedQuad[] east;
    private BakedQuad[] west;
    private BakedQuad[] generic;

    private ChiseledBlockBakedModel() {}

    public ChiseledBlockBakedModel(
            final int blockReference,
            final ChiselRenderType layer,
            final VoxelBlob data,
            final VertexFormat format,
            boolean isItem) {
        this(blockReference, layer, data, format);
    }

    public ChiseledBlockBakedModel(
            final int blockReference, final ChiselRenderType layer, final VoxelBlob data, final VertexFormat format) {
        myLayer = layer;
        final BlockState state = ModUtil.getStateById(blockReference);

        BakedModel originalModel = null;

        if (state != null) {
            originalModel = Minecraft.getInstance()
                    .getBlockRenderer()
                    .getBlockModelShaper()
                    .getBlockModel(state);
        }

        if (originalModel != null && data != null) {
            final VoxelBlob filteredData = new VoxelBlob(data);
            if (layer.filter(filteredData)) {
                final ChiseledModelBuilder builder = new ChiseledModelBuilder();
                generateFaces(builder, filteredData, RANDOM);

                // convert from builder to final storage.
                up = builder.getSide(Direction.UP);
                down = builder.getSide(Direction.DOWN);
                east = builder.getSide(Direction.EAST);
                west = builder.getSide(Direction.WEST);
                north = builder.getSide(Direction.NORTH);
                south = builder.getSide(Direction.SOUTH);
                generic = builder.getSide(null);
            }
        }
    }

    public static ChiseledBlockBakedModel breakingParticleModel(
            final ChiselRenderType layer, final Integer blockStateID, final RandomSource random) {
        return ModelUtil.getBreakingModel(layer, blockStateID, random);
    }

    private static void offsetVec(
            final int[] result, final int toX, final int toY, final int toZ, final Direction f, final int d) {

        int leftX = 0;
        final int leftY = 0;
        int leftZ = 0;

        final int upX = 0;
        int upY = 0;
        int upZ = 0;

        switch (f) {
            case DOWN:
                leftX = 1;
                upZ = 1;
                break;
            case EAST:
                leftZ = 1;
                upY = 1;
                break;
            case NORTH:
                leftX = 1;
                upY = 1;
                break;
            case SOUTH:
                leftX = 1;
                upY = 1;
                break;
            case UP:
                leftX = 1;
                upZ = 1;
                break;
            case WEST:
                leftZ = 1;
                upY = 1;
                break;
            default:
                break;
        }

        result[0] = (toX + leftX * d + upX * d) / 2;
        result[1] = (toY + leftY * d + upY * d) / 2;
        result[2] = (toZ + leftZ * d + upZ * d) / 2;
    }

    public static ChiseledBlockBakedModel createFromTexture(TextureAtlasSprite findTexture, ChiselRenderType layer) {
        ChiseledBlockBakedModel out = new ChiseledBlockBakedModel();
        out.sprite = findTexture;
        out.myLayer = layer;
        return out;
    }

    public List<BakedQuad> getList(final Direction side) {
        if (side != null) {
            switch (side) {
                case DOWN:
                    return asList(down);
                case EAST:
                    return asList(east);
                case NORTH:
                    return asList(north);
                case SOUTH:
                    return asList(south);
                case UP:
                    return asList(up);
                case WEST:
                    return asList(west);
                default:
            }
        }

        return asList(generic);
    }

    private List<BakedQuad> asList(final BakedQuad[] array) {
        if (array == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(array);
    }

    public boolean isEmpty() {
        boolean trulyEmpty = getList(null).isEmpty();

        for (final Direction e : Direction.values()) {
            trulyEmpty = trulyEmpty && getList(e).isEmpty();
        }

        return trulyEmpty;
    }

    IFaceBuilder getBuilder(VertexFormat format) {
        return new BakedQuadBuilder(format);
    }

    private void generateFaces(final ChiseledModelBuilder builder, final VoxelBlob blob, final RandomSource weight) {
        final ArrayList<ArrayList<FaceRegion>> rset = new ArrayList<>();
        final VisibleFace visFace = new VisibleFace();

        processXFaces(blob, visFace, rset);
        processYFaces(blob, visFace, rset);
        processZFaces(blob, visFace, rset);

        // re-usable float[]'s to minimize garbage cleanup.
        final int[] to = new int[3];
        final int[] from = new int[3];
        final float[] uvs = new float[8];
        final float[] pos = new float[3];

        // single reusable face builder.
        final IFaceBuilder darkBuilder = getBuilder(DefaultVertexFormat.BLOCK);
        final IFaceBuilder litBuilder = darkBuilder;

        for (final ArrayList<FaceRegion> src : rset) {
            FaceRegion.merge(src);

            for (final FaceRegion region : src) {
                final Direction myFace = region.face;
                int stateId = region.blockStateID;
                // keep integers up until the last moment... ( note I tested
                // snapping the floats after this stage, it made no
                // difference. )
                offsetVec(to, region.getMaxX(), region.getMaxY(), region.getMaxZ(), myFace, 1);
                offsetVec(from, region.getMinX(), region.getMinY(), region.getMinZ(), myFace, -1);
                final ModelQuadLayer[] mpc = ModelUtil.getCachedFace(stateId, weight, myFace, myLayer.layer);
                final float maxLightmap = 32.0f / 0xffff;
                if (mpc != null) {
                    for (final ModelQuadLayer pc : mpc) {
                        final IFaceBuilder faceBuilder = pc.light > 0 ? litBuilder : darkBuilder;
                        VertexFormat builderFormat = faceBuilder.getFormat();

                        faceBuilder.begin();
                        faceBuilder.setFace(myFace, pc.tint);

                        getFaceUvs(uvs, myFace, from, to, pc.uvs);
                        // build it.
                        for (int vertNum = 0; vertNum < 4; vertNum++) {
                            for (int elementIndex = 0;
                                    elementIndex < builderFormat.getElements().size();
                                    elementIndex++) {
                                final VertexFormatElement element =
                                        builderFormat.getElements().get(elementIndex);
                                switch (element.getUsage()) {
                                    case POSITION:
                                        getVertexPos(pos, myFace, vertNum, to, from);
                                        faceBuilder.put(vertNum, elementIndex, pos[0], pos[1], pos[2]);
                                        break;
                                    case COLOR:
                                        int cb = pc.color;
                                        final float[] colorData = new float[4];
                                        colorData[0] = ((cb >> 16) & 0xFF) / 255.0F;
                                        colorData[1] = ((cb >> 8) & 0xFF) / 255.0F;
                                        colorData[2] = (cb & 0xFF) / 255.0F;
                                        colorData[3] = ((cb >> 24) & 0xFF) / 255.0F;
                                        faceBuilder.put(vertNum, elementIndex, colorData);
                                        break;

                                    case NORMAL:
                                        // this fixes a bug with Forge AO?? and
                                        // solid blocks.. I have no idea why...
                                        faceBuilder.put(
                                                vertNum,
                                                elementIndex,
                                                myFace.getStepX(),
                                                myFace.getStepY(),
                                                myFace.getStepZ());
                                        break;

                                    case UV:
                                        if (element.getIndex() == 2) {
                                            final float v = maxLightmap * Math.max(0, Math.min(15, pc.light));
                                            faceBuilder.put(vertNum, elementIndex, v, v);
                                        } else {
                                            int uIndex = faceVertMap[myFace.get3DDataValue()][vertNum] * 2;
                                            int vIndex = faceVertMap[myFace.get3DDataValue()][vertNum] * 2 + 1;
                                            float u = pc.sprite.getU(uvs[uIndex] / 16f);
                                            float v = pc.sprite.getV(uvs[vIndex] / 16f);
                                            faceBuilder.put(vertNum, elementIndex, u, v);
                                        }

                                        break;
                                    default:
                                        faceBuilder.put(vertNum, elementIndex);
                                        break;
                                }
                            }
                        }

                        if (region.isEdge) {
                            builder.getList(myFace).add(faceBuilder.create(pc.sprite));
                        } else {
                            builder.getList(null).add(faceBuilder.create(pc.sprite));
                        }
                    }
                }
            }
        }
    }

    private float NotZero(float byteToFloat) {
        if (byteToFloat < 0.00001f) {
            return 1;
        }

        return byteToFloat;
    }

    private float byteToFloat(final int i) {
        return (i & 0xff) / 255.0f;
    }

    private void processXFaces(
            final VoxelBlob blob, final VisibleFace visFace, final ArrayList<ArrayList<FaceRegion>> rset) {
        ArrayList<FaceRegion> regions = null;
        final ICullTest test = myLayer.getTest();

        for (final Direction myFace : X_Faces) {
            for (int x = 0; x < blob.detail; x++) {
                if (regions == null) {
                    regions = new ArrayList<FaceRegion>(16);
                }

                for (int z = 0; z < blob.detail; z++) {
                    FaceRegion currentFace = null;

                    for (int y = 0; y < blob.detail; y++) {
                        final FaceRegion region = getRegion(blob, myFace, x, y, z, visFace, test);

                        if (region == null) {
                            currentFace = null;
                            continue;
                        }

                        if (currentFace != null) {
                            if (currentFace.extend(region)) {
                                continue;
                            }
                        }

                        currentFace = region;
                        regions.add(region);
                    }
                }

                if (!regions.isEmpty()) {
                    rset.add(regions);
                    regions = null;
                }
            }
        }
    }

    private void processYFaces(
            final VoxelBlob blob, final VisibleFace visFace, final ArrayList<ArrayList<FaceRegion>> rset) {
        ArrayList<FaceRegion> regions = null;
        final ICullTest test = myLayer.getTest();

        for (final Direction myFace : Y_Faces) {
            for (int y = 0; y < blob.detail; y++) {
                if (regions == null) {
                    regions = new ArrayList<FaceRegion>(16);
                }

                for (int z = 0; z < blob.detail; z++) {
                    FaceRegion currentFace = null;

                    for (int x = 0; x < blob.detail; x++) {
                        final FaceRegion region = getRegion(blob, myFace, x, y, z, visFace, test);

                        if (region == null) {
                            currentFace = null;
                            continue;
                        }

                        if (currentFace != null) {
                            if (currentFace.extend(region)) {
                                continue;
                            }
                        }

                        currentFace = region;
                        regions.add(region);
                    }
                }

                if (!regions.isEmpty()) {
                    rset.add(regions);
                    regions = null;
                }
            }
        }
    }

    private void processZFaces(
            final VoxelBlob blob, final VisibleFace visFace, final ArrayList<ArrayList<FaceRegion>> rset) {
        ArrayList<FaceRegion> regions = null;
        final ICullTest test = myLayer.getTest();

        for (final Direction myFace : Z_Faces) {
            for (int z = 0; z < blob.detail; z++) {
                if (regions == null) {
                    regions = new ArrayList<FaceRegion>(16);
                }

                for (int y = 0; y < blob.detail; y++) {
                    FaceRegion currentFace = null;

                    for (int x = 0; x < blob.detail; x++) {
                        final FaceRegion region = getRegion(blob, myFace, x, y, z, visFace, test);

                        if (region == null) {
                            currentFace = null;
                            continue;
                        }

                        if (currentFace != null) {
                            if (currentFace.extend(region)) {
                                continue;
                            }
                        }

                        currentFace = region;
                        regions.add(region);
                    }
                }

                if (!regions.isEmpty()) {
                    rset.add(regions);
                    regions = null;
                }
            }
        }
    }

    private FaceRegion getRegion(
            final VoxelBlob blob,
            final Direction myFace,
            final int x,
            final int y,
            final int z,
            final VisibleFace visFace,
            final ICullTest test) {
        blob.visibleFace(myFace, x, y, z, visFace, test);

        if (visFace.visibleFace) {
            final Vec3i off = myFace.getNormal();

            return new FaceRegion(
                    myFace,
                    x * 2 + 1 + off.getX(),
                    y * 2 + 1 + off.getY(),
                    z * 2 + 1 + off.getZ(),
                    visFace.state,
                    visFace.isEdge);
        }

        return null;
    }

    // generate final pos from static data.
    private void getVertexPos(
            final float[] pos, final Direction side, final int vertNum, final int[] to, final int[] from) {
        final float[] interpos = quadMapping[side.ordinal()][vertNum];

        pos[0] = to[0] * interpos[0] + from[0] * interpos[1];
        pos[1] = to[1] * interpos[2] + from[1] * interpos[3];
        pos[2] = to[2] * interpos[4] + from[2] * interpos[5];
    }

    private void getFaceUvs(
            final float[] uvs, final Direction face, final int[] from, final int[] to, final float[] quadsUV) {
        float to_u = 0;
        float to_v = 0;
        float from_u = 0;
        float from_v = 0;

        switch (face) {
            case UP:
                to_u = to[0] / 16.0f;
                to_v = to[2] / 16.0f;
                from_u = from[0] / 16.0f;
                from_v = from[2] / 16.0f;
                break;
            case DOWN:
                to_u = to[0] / 16.0f;
                to_v = to[2] / 16.0f;
                from_u = from[0] / 16.0f;
                from_v = from[2] / 16.0f;
                break;
            case SOUTH:
                to_u = to[0] / 16.0f;
                to_v = to[1] / 16.0f;
                from_u = from[0] / 16.0f;
                from_v = from[1] / 16.0f;
                break;
            case NORTH:
                to_u = to[0] / 16.0f;
                to_v = to[1] / 16.0f;
                from_u = from[0] / 16.0f;
                from_v = from[1] / 16.0f;
                break;
            case EAST:
                to_u = to[1] / 16.0f;
                to_v = to[2] / 16.0f;
                from_u = from[1] / 16.0f;
                from_v = from[2] / 16.0f;
                break;
            case WEST:
                to_u = to[1] / 16.0f;
                to_v = to[2] / 16.0f;
                from_u = from[1] / 16.0f;
                from_v = from[2] / 16.0f;
                break;
            default:
        }

        uvs[0] = 16.0f * u(quadsUV, to_u, to_v); // 0
        uvs[1] = 16.0f * v(quadsUV, to_u, to_v); // 1

        uvs[2] = 16.0f * u(quadsUV, from_u, to_v); // 2
        uvs[3] = 16.0f * v(quadsUV, from_u, to_v); // 3

        uvs[4] = 16.0f * u(quadsUV, from_u, from_v); // 2
        uvs[5] = 16.0f * v(quadsUV, from_u, from_v); // 3

        uvs[6] = 16.0f * u(quadsUV, to_u, from_v); // 0
        uvs[7] = 16.0f * v(quadsUV, to_u, from_v); // 1
    }

    // Interpolate u
    float u(final float[] src, final float inU, final float inV) {
        final float inv = 1.0f - inU;
        final float u1 = src[0] * inU + inv * src[2];
        final float u2 = src[4] * inU + inv * src[6];
        return u1 * inV + (1.0f - inV) * u2;
    }

    // Interpolate v
    float v(final float[] src, final float inU, final float inV) {
        final float inv = 1.0f - inU;
        final float v1 = src[1] * inU + inv * src[3];
        final float v2 = src[5] * inU + inv * src[7];
        return v1 * inV + (1.0f - inV) * v2;
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState blockState, @Nullable Direction direction, RandomSource randomSource) {
        return getList(direction);
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return ClientSide.instance.getMissingIcon();
    }

    public int faceCount() {
        int count = getList(null).size();

        for (final Direction f : Direction.values()) {
            count += getList(f).size();
        }

        return count;
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            @NotNull RandomSource rand,
            @NotNull IModelData extraData,
            @NotNull ChiselRenderType renderType) {
        return getQuads(state, side, rand);
    }

    @Override
    public Set<ChiselRenderType> getRenderTypes(
            @NotNull BlockAndTintGetter world,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @NotNull IModelData modelData) {
        return Set.of(myLayer);
    }
}
