package mod.chiselsandbits.render.chiseledblock;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.model.baked.BaseBakedBlockModel;
import mod.chiselsandbits.client.model.data.IModelData;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.render.helpers.ModelUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One render type of a chiseled shape as a legacy quad model, for items, placement previews and bit storage. Placed
 * blocks skip this and emit their {@link ShapeMesh} straight into the chunk mesh.
 */
public class ChiseledBlockBakedModel extends BaseBakedBlockModel {
    public static final float PIXELS_PER_BLOCK = 16.0f;

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

        BlockStateModel originalModel = null;

        if (state != null) {
            originalModel = Minecraft.getInstance()
                    .getModelManager()
                    .getBlockStateModelSet()
                    .get(state);
        }

        if (originalModel != null && data != null) {
            build(ShapeMeshes.of(data));
        }
    }

    /** Builds the {@code layer} model of an already meshed shape. */
    public ChiseledBlockBakedModel(final ShapeMesh mesh, final ChiselRenderType layer) {
        myLayer = layer;
        build(mesh);
    }

    private void build(final ShapeMesh mesh) {
        final ChiseledModelBuilder builder = new ChiseledModelBuilder();
        ChiseledEmitter.emitType(mesh, myLayer, (quad, cullFace, chunkLayer) -> builder.getList(cullFace)
                .add(quad));

        up = builder.getSide(Direction.UP);
        down = builder.getSide(Direction.DOWN);
        east = builder.getSide(Direction.EAST);
        west = builder.getSide(Direction.WEST);
        north = builder.getSide(Direction.NORTH);
        south = builder.getSide(Direction.SOUTH);
        generic = builder.getSide(null);
    }

    public static ChiseledBlockBakedModel breakingParticleModel(
            final ChiselRenderType layer, final Integer blockStateID, final RandomSource random) {
        return ModelUtil.getBreakingModel(layer, blockStateID, random);
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
        return ClientSide.getInstance().getMissingIcon();
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
