package mod.chiselsandbits.render.chiseledblock;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.data.VoxelBlobStateInstance;
import mod.chiselsandbits.chiseledblock.data.VoxelBlobStateReference;
import mod.chiselsandbits.client.model.baked.BaseSmartModel;
import mod.chiselsandbits.client.model.data.IModelData;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.interfaces.ICacheClearable;
import mod.chiselsandbits.render.ModelCombined;
import mod.chiselsandbits.utils.SimpleMaxSizedCache;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class ChiseledBlockSmartModel extends BaseSmartModel implements ICacheClearable {
    public static final BitSet FLUID_RENDER_TYPES =
            new BitSet(RenderType.chunkBufferLayers().size());
    private static final SimpleMaxSizedCache<ModelCacheKey, ChiseledBlockBakedModel> MODEL_CACHE =
            new SimpleMaxSizedCache<>(
                    ChiselsAndBits.getConfig().getClient().modelCacheSize.get());
    private static final Map<ItemStack, BakedModel> ITEM_TO_MODEL_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<VoxelBlobStateInstance, Integer> SIDE_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final RandomSource RANDOM_SOURCE = RandomSource.create();

    public static int getSides(final TileEntityBlockChiseled te) {
        final VoxelBlobStateReference ref = te.getBlobStateReference();
        Integer out;

        if (ref == null) {
            return 0;
        }

        synchronized (SIDE_CACHE) {
            out = SIDE_CACHE.get(ref.getInstance());
            if (out == null) {
                final VoxelBlob blob = ref.getVoxelBlob();

                // ignore non-solid, and fluids.
                blob.simulateFilter(RenderType.solid());
                blob.filterFluids(false);

                out = blob.getSideFlags(0, VoxelBlob.dim_minus_one, VoxelBlob.dim2);
                SIDE_CACHE.put(ref.getInstance(), out);
            }
        }

        return out;
    }

    public static ChiseledBlockBakedModel getCachedModel(
            final TileEntityBlockChiseled te, final ChiselRenderType layer) {
        final VoxelBlobStateReference data = te.getBlobStateReference();
        Integer blockP = te.getPrimaryBlockStateId();
        VoxelBlob vBlob = (data != null) ? data.getVoxelBlob() : null;
        return getCachedModel(blockP, vBlob, layer, getModelFormat(), Objects.requireNonNull(te.getLevel()).random);
    }

    public static ChiseledBlockBakedModel getCachedModel(final ItemStack stack, final ChiselRenderType layer) {
        Integer blockP = 0;
        return getCachedModel(
                blockP, ModUtil.getBlobFromStack(stack, null), layer, getModelFormat(), RandomSource.create());
    }

    private static VertexFormat getModelFormat() {
        return DefaultVertexFormat.BLOCK;
    }

    public static boolean ForgePipelineDisabled() {
        return ChiselsAndBits.getConfig().getClient().disableCustomVertexFormats.get();
    }

    public static ChiseledBlockBakedModel getCachedModel(
            final Integer blockP,
            final VoxelBlob data,
            final ChiselRenderType layer,
            final VertexFormat format,
            final RandomSource random) {
        if (data == null) {
            return new ChiseledBlockBakedModel(blockP, layer, null, format);
        }

        ChiseledBlockBakedModel out = null;

        if (format == getModelFormat()) {
            out = MODEL_CACHE.get(new ModelCacheKey(data, layer));
        }

        if (out == null) {
            out = new ChiseledBlockBakedModel(blockP, layer, data, format);

            if (out.isEmpty()) {
                out = ChiseledBlockBakedModel.breakingParticleModel(layer, blockP, random);
            }

            if (format == getModelFormat()) {
                MODEL_CACHE.put(new ModelCacheKey(data, layer), out);
            }
        } else {
            return out;
        }

        return out;
    }

    public static void onConfigurationReload(ModConfig modConfig) {
        MODEL_CACHE.changeMaxSize(
                ChiselsAndBits.getConfig().getClient().modelCacheSize.get());
    }

    @Override
    public BakedModel handleBlockState(
            BlockState state, RandomSource random, IModelData modelData, ChiselRenderType renderType) {
        if (state == null) {
            return super.handleBlockState(state, random, modelData, renderType);
        }
        Map<ChiselRenderType, BakedModel> pre;
        if (!modelData.getData(TileEntityBlockChiseled.MODEL_UPDATE)
                && (pre = modelData.getData(TileEntityBlockChiseled.MODEL_PROP)) != null) {
            return pre.get(renderType);
        }
        VoxelBlobStateReference data = modelData.getData(TileEntityBlockChiseled.MP_VBSR);
        int primaryStateId = modelData.getData(TileEntityBlockChiseled.MP_PBSI);
        final VoxelBlob blob = data == null ? null : data.getVoxelBlob();
        Map<ChiselRenderType, BakedModel> typedModels = new ConcurrentHashMap<>();

        Set<Integer> states = ModUtil.getAllStates(blob);

        for (int s : states) {
            Optional<Pair<ChiselRenderType, BakedModel>> opt = createModel(s, primaryStateId, blob, random);
            opt.ifPresent(model -> {
                typedModels.put(model.getKey(), model.getValue());
            });
        }

        modelData.setData(TileEntityBlockChiseled.MODEL_PROP, typedModels);

        return typedModels.get(renderType);
    }

    private Optional<ChiselRenderType> getRenderType(int stateId) {
        BlockState state = ModUtil.getStateById(stateId);
        if (state.isAir()) {
            return Optional.empty();
        }
        if (!state.getFluidState().isEmpty()) {
            return Optional.of(
                    ChiselRenderType.fromLayer(ItemBlockRenderTypes.getRenderLayer(state.getFluidState()), true));
        }
        return Optional.of(ChiselRenderType.fromLayer(ItemBlockRenderTypes.getChunkRenderType(state), false));
    }

    private Optional<Pair<ChiselRenderType, BakedModel>> createModel(
            int stateId, int blockP, VoxelBlob blob, RandomSource randomSource) {
        BlockState state = ModUtil.getStateById(stateId);
        if (state.isAir()) {
            return Optional.empty();
        }

        if (ModUtil.isFluid(state)) {
            RenderType renderType = ItemBlockRenderTypes.getRenderLayer(state.getFluidState());

            ChiselRenderType solidLayer, fluidLayer;
            ChiseledBlockBakedModel fluid = getCachedModel(
                    blockP,
                    blob,
                    fluidLayer = ChiselRenderType.fromLayer(renderType, true),
                    getModelFormat(),
                    randomSource);
            ChiseledBlockBakedModel solid = getCachedModel(
                    blockP,
                    blob,
                    solidLayer = ChiselRenderType.fromLayer(renderType, false),
                    getModelFormat(),
                    randomSource);
            BakedModel out;
            if (solid.isEmpty()) {
                out = fluid;
            } else if (fluid.isEmpty()) {
                out = solid;
            } else {
                out = new ModelCombined(Set.of(solidLayer, fluidLayer), solid, fluid);
            }

            return Optional.of(new ImmutablePair<>(fluidLayer, out));
        }

        ChiselRenderType renderType = ChiselRenderType.fromLayer(ItemBlockRenderTypes.getChunkRenderType(state), false);
        return Optional.of(new ImmutablePair<>(
                renderType, getCachedModel(blockP, blob, renderType, getModelFormat(), randomSource)));
    }

    @Override
    public BakedModel resolve(
            final BakedModel originalModel, final ItemStack stack, final Level world, final LivingEntity entity) {
        BakedModel mdl = ITEM_TO_MODEL_CACHE.get(stack);

        if (mdl != null) {
            return mdl;
        }

        CompoundTag c = stack.getTag();
        if (c == null) {
            return this;
        }

        c = c.getCompound(ModUtil.NBT_BLOCKENTITYTAG);

        final byte[] data = c.getByteArray(NBTBlobConverter.NBT_LEGACY_VOXEL);
        byte[] vdata = c.getByteArray(NBTBlobConverter.NBT_VERSIONED_VOXEL);
        final Integer blockP = c.getInt(NBTBlobConverter.NBT_PRIMARY_STATE);

        if (vdata.length == 0 && data.length > 0) {
            final VoxelBlob xx = new VoxelBlob();

            try {
                xx.fromLegacyByteArray(data);
            } catch (final IOException e) {
                // :_(
            }

            vdata = xx.blobToBytes(VoxelBlob.VERSION_COMPACT_PALLETED);
        }
        byte[] finalVdata = vdata;
        final BakedModel[] models =
                ModUtil.extractRenderTypes(new VoxelBlobStateReference(vdata, 0L).getVoxelBlob()).stream()
                        .flatMap(renderType -> {
                            BakedModel solidModel = getCachedModel(
                                    blockP,
                                    new VoxelBlobStateReference(finalVdata, 0L).getVoxelBlob(),
                                    ChiselRenderType.fromLayer(renderType, false),
                                    DefaultVertexFormat.BLOCK,
                                    RANDOM_SOURCE);
                            BakedModel fluidModel = getCachedModel(
                                    blockP,
                                    new VoxelBlobStateReference(finalVdata, 0L).getVoxelBlob(),
                                    ChiselRenderType.fromLayer(renderType, true),
                                    DefaultVertexFormat.BLOCK,
                                    RANDOM_SOURCE);
                            return Stream.of(solidModel, fluidModel);
                        })
                        .toArray(BakedModel[]::new);
        mdl = new ModelCombined(models);

        ITEM_TO_MODEL_CACHE.put(stack, mdl);

        return mdl;
    }

    @Override
    public void clearCache() {
        SIDE_CACHE.clear();
        MODEL_CACHE.clear();
        ITEM_TO_MODEL_CACHE.clear();

        FLUID_RENDER_TYPES.clear();
        final List<RenderType> blockRenderTypes = RenderType.chunkBufferLayers();
        for (int i = 0; i < blockRenderTypes.size(); i++) {
            final RenderType renderType = blockRenderTypes.get(i);
            for (final Fluid fluid : BuiltInRegistries.FLUID) {
                if (ItemBlockRenderTypes.getRenderLayer(fluid.defaultFluidState()) == renderType) {
                    FLUID_RENDER_TYPES.set(i);
                    break;
                }
            }
        }
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public void updateModelData(
            @NotNull BlockAndTintGetter world,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @NotNull IModelData modelData) {

        VoxelBlobStateReference data = modelData.getData(TileEntityBlockChiseled.MP_VBSR);
        final VoxelBlob blob = data == null ? null : data.getVoxelBlob();
        Map<ChiselRenderType, BakedModel> typedModels = new ConcurrentHashMap<>();
        int primaryStateId = modelData.getData(TileEntityBlockChiseled.MP_PBSI);

        Set<Integer> states = ModUtil.getAllStates(blob);

        for (int s : states) {
            Optional<Pair<ChiselRenderType, BakedModel>> opt = createModel(s, primaryStateId, blob, RANDOM_SOURCE);
            opt.ifPresent(model -> {
                typedModels.put(model.getKey(), model.getValue());
            });
        }

        modelData.setData(TileEntityBlockChiseled.MODEL_PROP, typedModels);
        modelData.setData(TileEntityBlockChiseled.MODEL_UPDATE, false);
    }

    private Set<ChiselRenderType> getRenderTypes(VoxelBlob blob) {
        Set<ChiselRenderType> result = new HashSet<>();
        for (int state : ModUtil.getAllStates(blob)) {
            getRenderType(state).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public Set<ChiselRenderType> getRenderTypes(
            @NotNull BlockAndTintGetter world,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @NotNull IModelData modelData) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityBlockChiseled te)) {
            return Set.of();
        }

        if (te.getBlob() == null) {
            return Set.of();
        }

        Map<ChiselRenderType, BakedModel> data;
        if ((data = modelData.getData(TileEntityBlockChiseled.MODEL_PROP)) == null) {
            VoxelBlobStateReference blobRef = modelData.getData(TileEntityBlockChiseled.MP_VBSR);
            final VoxelBlob blob = blobRef == null ? null : blobRef.getVoxelBlob();
            return getRenderTypes(blob);
        }

        return data.keySet();
    }

    private static final class ModelCacheKey {
        private final VoxelBlob blob;
        private final ChiselRenderType type;

        private ModelCacheKey(final VoxelBlob blob, final ChiselRenderType type) {
            this.blob = blob;
            this.type = type;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ModelCacheKey that)) {
                return false;
            }
            return Objects.equals(blob, that.blob) && Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(blob, type);
        }
    }
}
