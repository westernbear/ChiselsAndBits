package mod.chiselsandbits.render.helpers;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.DeprecationHelper;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.interfaces.ICacheClearable;
import mod.chiselsandbits.render.chiseledblock.ChiselRenderType;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockBakedModel;
import mod.chiselsandbits.render.helpers.ModelQuadLayer.ModelQuadLayerBuilder;
import mod.chiselsandbits.utils.FluidUtil;
import mod.chiselsandbits.utils.LightUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

@SuppressWarnings("unchecked")
public class ModelUtil implements ICacheClearable {
    private static final HashMap<Pair<ChunkSectionLayer, Direction>, HashMap<Integer, String>> blockToTexture =
            new HashMap<>();
    private static final HashMap<Triple<Integer, ChunkSectionLayer, Direction>, ModelQuadLayer[]> cache =
            new HashMap<>();
    private static final HashMap<Pair<ChunkSectionLayer, Integer>, ChiseledBlockBakedModel> breakCache =
            new HashMap<>();

    @SuppressWarnings("unused")
    private static final ModelUtil instance = new ModelUtil();

    public static RandomSource MODEL_RANDOM = RandomSource.create();

    private ModelUtil() {
        ChiselsAndBits.getInstance().addClearable(this);
    }

    public static ModelQuadLayer[] getCachedFace(
            final int stateID, final RandomSource weight, final Direction face, final ChunkSectionLayer layer) {
        if (layer == null) {
            return null;
        }
        final Triple<Integer, ChunkSectionLayer, Direction> cacheVal = Triple.of(stateID, layer, face);

        final ModelQuadLayer[] mpc = cache.get(cacheVal);
        if (mpc != null) {
            return mpc;
        }

        return getInnerCachedFace(cacheVal, stateID, weight, face, layer);
    }

    private static ModelQuadLayer[] getInnerCachedFace(
            final Triple<Integer, ChunkSectionLayer, Direction> cacheVal,
            final int stateID,
            final RandomSource weight,
            final Direction face,
            final ChunkSectionLayer layer) {
        final BlockState state = ModUtil.getStateById(stateID);
        final BlockStateModel model = Minecraft.getInstance()
                .getModelManager()
                .getBlockStateModelSet()
                .get(state);
        final int lv = ChiselsAndBits.getConfig().getClient().useGetLightValue.get()
                ? DeprecationHelper.getLightValue(state)
                : 0;

        final Fluid fluid = BlockBitInfo.getFluidFromBlock(state.getBlock());
        if (fluid != null) {
            for (final Direction xf : Direction.values()) {
                final ModelQuadLayer[] mp = new ModelQuadLayer[1];
                mp[0] = new ModelQuadLayer();
                mp[0].color = FluidUtil.getColor(fluid);
                mp[0].light = lv;
                final float V = 0.5f;
                final float Uf = 1.0f;
                final float U = 0.5f;
                final float Vf = 1.0f;

                if (xf.getAxis() == Axis.Y) {
                    mp[0].sprite = Minecraft.getInstance()
                            .getAtlasManager()
                            .getAtlasOrThrow(AtlasIds.BLOCKS)
                            .getSprite(
                                    FluidUtil.getStillTexture(fluid).contents().name());
                    mp[0].uvs = new float[] {Uf, Vf, 0, Vf, Uf, 0, 0, 0};
                } else if (xf.getAxis() == Axis.X) {
                    mp[0].sprite = Minecraft.getInstance()
                            .getAtlasManager()
                            .getAtlasOrThrow(AtlasIds.BLOCKS)
                            .getSprite(FluidUtil.getFlowingTexture(fluid)
                                    .contents()
                                    .name());
                    mp[0].uvs = new float[] {U, 0, U, V, 0, 0, 0, V};
                } else {
                    mp[0].sprite = Minecraft.getInstance()
                            .getAtlasManager()
                            .getAtlasOrThrow(AtlasIds.BLOCKS)
                            .getSprite(FluidUtil.getFlowingTexture(fluid)
                                    .contents()
                                    .name());
                    mp[0].uvs = new float[] {U, 0, 0, 0, U, V, 0, V};
                }

                mp[0].tint = 0;

                final Triple<Integer, ChunkSectionLayer, Direction> k = Triple.of(stateID, layer, xf);
                cache.put(k, mp);
            }

            return cache.get(cacheVal);
        }

        final HashMap<Direction, ArrayList<ModelQuadLayerBuilder>> tmp = new HashMap<>();
        final int color = BlockBitInfo.getColorFor(state, 0);
        for (final Direction f : Direction.values()) {
            tmp.put(f, new ArrayList<>());
        }

        if (model != null) {
            for (final Direction f : Direction.values()) {
                final List<BakedQuad> quads = ModelUtil.getModelQuads(model, layer, state, f, MODEL_RANDOM);
                processFaces(tmp, quads, state);
            }

            processFaces(tmp, ModelUtil.getModelQuads(model, layer, state, null, MODEL_RANDOM), state);
        }

        for (final Direction f : Direction.values()) {
            final Triple<Integer, ChunkSectionLayer, Direction> k = Triple.of(stateID, layer, f);
            final ArrayList<ModelQuadLayerBuilder> x = tmp.get(f);
            final ModelQuadLayer[] mp = new ModelQuadLayer[x.size()];

            for (int z = 0; z < x.size(); z++) {
                mp[z] = x.get(z).build(stateID, color, lv);
            }

            cache.put(k, mp);
        }

        return cache.get(cacheVal);
    }

    private static List<BakedQuad> getModelQuads(
            final BlockStateModel model,
            final ChunkSectionLayer renderType,
            final BlockState state,
            final Direction f,
            final RandomSource rand) {
        if (model == null) {
            return Collections.emptyList();
        }

        final List<BlockStateModelPart> parts = new ArrayList<>();
        rand.setSeed(42L);
        model.collectParts(rand, parts);
        final List<BakedQuad> result = new ArrayList<>();
        for (final BlockStateModelPart part : parts) {
            for (final BakedQuad quad : part.getQuads(f)) {
                if (quad.materialInfo().layer() == renderType) {
                    result.add(quad);
                }
            }
        }

        return result;
    }

    private static void processFaces(
            final HashMap<Direction, ArrayList<ModelQuadLayerBuilder>> tmp,
            final List<BakedQuad> quads,
            final BlockState state) {

        for (final BakedQuad q : quads) {
            final Direction face = q.direction();

            if (face == null) {
                continue;
            }

            try {
                final TextureAtlasSprite sprite = findQuadTexture(q, state);
                final ArrayList<ModelQuadLayerBuilder> l = tmp.get(face);

                ModelQuadLayerBuilder b = null;
                for (final ModelQuadLayerBuilder lx : l) {
                    if (lx.cache.sprite == sprite) {
                        b = lx;
                        break;
                    }
                }

                if (b == null) {
                    // top/bottom
                    int uCoord = 0;
                    int vCoord = 2;

                    switch (face) {
                        case NORTH:
                        case SOUTH:
                            uCoord = 0;
                            vCoord = 1;
                            break;
                        case EAST:
                        case WEST:
                            uCoord = 1;
                            vCoord = 2;
                            break;
                        default:
                    }

                    b = new ModelQuadLayerBuilder(sprite, uCoord, vCoord);
                    b.setFace(face);
                    b.setSourceQuad(q);
                    b.setTint(q.materialInfo().tintIndex());
                    l.add(b);
                    LightUtil.put(b.uvr, q);
                    LightUtil.put(b.lv, q);
                }
            } catch (final Exception ignored) {

            }
        }
    }

    public static TextureAtlasSprite findQuadTexture(final BakedQuad q, final BlockState state)
            throws IllegalArgumentException, NullPointerException {
        if (q.materialInfo().sprite() == null) {
            return Minecraft.getInstance()
                    .getAtlasManager()
                    .getAtlasOrThrow(AtlasIds.BLOCKS)
                    .getSprite(MissingTextureAtlasSprite.getLocation());
        }
        return q.materialInfo().sprite();
    }

    public static BlockStateModel solveModel(
            final BlockState state,
            final RandomSource weight,
            final BlockStateModel originalModel,
            final ChunkSectionLayer layer) {
        boolean hasFaces;

        try {
            hasFaces = hasFaces(originalModel, layer, state, null, weight);

            for (final Direction f : Direction.values()) {
                hasFaces = hasFaces || hasFaces(originalModel, layer, state, f, weight);
            }
        } catch (final Exception e) {
            // an exception was thrown.. use the item model and hope...
            hasFaces = false;
        }

        if (!hasFaces) {
            return originalModel;
        }

        return originalModel;
    }

    private static boolean hasFaces(
            final BlockStateModel model,
            final ChunkSectionLayer renderType,
            final BlockState state,
            final Direction f,
            final RandomSource weight) {
        final List<BakedQuad> l = getModelQuads(model, renderType, state, f, weight);
        if (l == null || l.isEmpty()) {
            return false;
        }

        TextureAtlasSprite texture = null;

        try {
            texture = findTexture(null, l, f);
        } catch (final Exception ignored) {
        }

        final ModelVertexRange mvr = new ModelVertexRange();

        for (final BakedQuad q : l) {
            LightUtil.put(mvr, q);
        }

        return mvr.getLargestRange() > 0 && !isMissing(texture);
    }

    private static boolean isMissing(final TextureAtlasSprite texture) {
        if (texture == null) {
            return true;
        }

        return texture.contents().name().equals(MissingTextureAtlasSprite.getLocation());
    }

    public static TextureAtlasSprite findTexture(
            final int BlockRef,
            final BlockStateModel model,
            final Direction myFace,
            final ChunkSectionLayer layer,
            final RandomSource random) {
        // didn't work? ok lets try scanning for the texture in the
        if (blockToTexture
                .getOrDefault(Pair.of(layer, myFace), Maps.newHashMap())
                .containsKey(BlockRef)) {
            final String textureName =
                    blockToTexture.get(Pair.of(layer, myFace)).get(BlockRef);
            return Minecraft.getInstance()
                    .getAtlasManager()
                    .getAtlasOrThrow(AtlasIds.BLOCKS)
                    .getSprite(Identifier.parse(textureName));
        }

        TextureAtlasSprite texture = null;
        final BlockState state = ModUtil.getStateById(BlockRef);

        if (model != null) {
            try {
                texture = findTexture(texture, getModelQuads(model, layer, state, myFace, random), myFace);

                if (texture == null) {
                    for (final Direction side : Direction.values()) {
                        texture = findTexture(texture, getModelQuads(model, layer, state, side, random), side);
                    }

                    texture = findTexture(texture, getModelQuads(model, layer, state, null, random), null);
                }
            } catch (final Exception ignored) {
            }
        }

        // who knows if that worked.. now lets try to get a texture...
        if (isMissing(texture)) {
            try {
                if (model != null) {
                    texture = model.particleMaterial().sprite();
                }
            } catch (final Exception ignored) {
            }
        }

        if (isMissing(texture)) {
            try {
                texture = Minecraft.getInstance()
                        .getModelManager()
                        .getBlockStateModelSet()
                        .getParticleMaterial(state)
                        .sprite();
            } catch (final Exception ignored) {
            }
        }

        if (texture == null) {
            texture = Minecraft.getInstance()
                    .getAtlasManager()
                    .getAtlasOrThrow(AtlasIds.BLOCKS)
                    .getSprite(Identifier.parse("missingno"));
        }

        blockToTexture.remove(Pair.of(layer, myFace), null);
        blockToTexture.putIfAbsent(Pair.of(layer, myFace), Maps.newHashMap());
        blockToTexture
                .get(Pair.of(layer, myFace))
                .put(BlockRef, texture.atlasLocation().toString());
        return texture;
    }

    private static TextureAtlasSprite findTexture(
            TextureAtlasSprite texture, final List<BakedQuad> faceQuads, final Direction myFace)
            throws IllegalArgumentException, NullPointerException {
        for (final BakedQuad q : faceQuads) {
            if (q.direction() == myFace) {
                texture = findQuadTexture(q, null);
            }
        }

        return texture;
    }

    public static boolean isOne(final float v) {
        return Math.abs(v) < 0.01;
    }

    public static boolean isZero(final float v) {
        return Math.abs(v - 1.0f) < 0.01;
    }

    public static Integer getItemStackColor(final ItemStack target, final int tint) {
        final Block block = Block.byItem(target.getItem());
        return block == null ? 0xffffff : getBlockStateColor(block.defaultBlockState(), tint);
    }

    public static int getBlockStateColor(final BlockState state, final int tint) {
        final BlockTintSource tintSource =
                Minecraft.getInstance().getBlockColors().getTintSource(state, tint);
        return tintSource == null ? 0xffffff : tintSource.color(state);
    }

    public static ChiseledBlockBakedModel getBreakingModel(
            ChiselRenderType layer, Integer blockStateID, RandomSource random) {
        Pair<ChunkSectionLayer, Integer> key = Pair.of(layer.layer, blockStateID);
        ChiseledBlockBakedModel out = breakCache.get(key);

        if (out == null) {

            final BlockState state = ModUtil.getStateById(blockStateID);
            final BlockStateModel model = ModelUtil.solveModel(
                    state,
                    random,
                    Minecraft.getInstance()
                            .getModelManager()
                            .getBlockStateModelSet()
                            .get(ModUtil.getStateById(blockStateID)),
                    layer.layer);

            if (model != null) {
                out = ChiseledBlockBakedModel.createFromTexture(
                        ModelUtil.findTexture(blockStateID, model, Direction.UP, layer.layer, random), layer);
            } else {
                out = ChiseledBlockBakedModel.createFromTexture(null, null);
            }

            breakCache.put(key, out);
        }

        return out;
    }

    @Override
    public void clearCache() {
        blockToTexture.clear();
        cache.clear();
        breakCache.clear();
    }
}
