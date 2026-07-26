package mod.chiselsandbits.bitstorage;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.render.chiseledblock.ChiselRenderType;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockBakedModel;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockSmartModel;
import mod.chiselsandbits.utils.FluidUtil;
import mod.chiselsandbits.utils.SimpleMaxSizedCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

public class TileEntitySpecialRenderBitStorage
        implements BlockEntityRenderer<TileEntityBitStorage, TileEntitySpecialRenderBitStorage.RenderState> {

    private static final SimpleMaxSizedCache<CacheKey, VoxelBlob> STORAGE_CONTENTS_BLOB_CACHE =
            new SimpleMaxSizedCache<>(ChiselsAndBits.getConfig()
                    .getClient()
                    .bitStorageContentCacheSize
                    .get());

    public TileEntitySpecialRenderBitStorage(final BlockEntityRendererProvider.Context context) {}

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(
            final TileEntityBitStorage blockEntity,
            final RenderState renderState,
            final float partialTick,
            final Vec3 cameraPosition,
            final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity, renderState, partialTick, cameraPosition, crumblingOverlay);
        renderState.layers.clear();

        final int bits = blockEntity.getBits();
        final Fluid fluid = blockEntity.getMyFluid();
        final BlockState contentState = fluid == null
                ? blockEntity.getState()
                : fluid.defaultFluidState().createLegacyBlock();
        if (bits <= 0 || contentState == null || !(blockEntity.getLevel() instanceof BlockAndTintGetter tintLevel)) {
            return;
        }

        final int stateId = ModUtil.getStateId(contentState);
        VoxelBlob blob = STORAGE_CONTENTS_BLOB_CACHE.get(new CacheKey(stateId, bits));
        if (blob == null) {
            blob = new VoxelBlob();
            blob.fillAmountFromBottom(stateId, bits);
            STORAGE_CONTENTS_BLOB_CACHE.put(new CacheKey(stateId, bits), blob);
        }

        final RandomSource random =
                RandomSource.create(blockEntity.getBlockPos().asLong());
        final int fluidColor = fluid == null ? 0xffffffff : ARGB.opaque(FluidUtil.getColor(fluid));
        for (final ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            final ChiseledBlockBakedModel model = ChiseledBlockSmartModel.getCachedModel(
                    stateId, blob, ChiselRenderType.fromLayer(layer, fluid != null), DefaultVertexFormat.BLOCK, random);
            if (model.isEmpty()) {
                continue;
            }

            final List<ColoredQuad> quads =
                    collectQuads(model, blockEntity.getBlockPos(), tintLevel, fluidColor, random);
            if (!quads.isEmpty()) {
                renderState.layers.add(new SubmittedLayer(movingRenderType(layer), List.copyOf(quads)));
            }
        }
    }

    @Override
    public void submit(
            final RenderState renderState,
            final PoseStack poseStack,
            final SubmitNodeCollector collector,
            final CameraRenderState cameraState) {
        if (renderState.layers.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(2 / 16f, 2 / 16f, 2 / 16f);
        poseStack.scale(12 / 16f, 12 / 16f, 12 / 16f);

        for (final SubmittedLayer layer : renderState.layers) {
            collector.submitCustomGeometry(poseStack, layer.renderType(), (pose, vertexConsumer) -> {
                final QuadInstance instance = new QuadInstance();
                instance.setLightCoords(renderState.lightCoords);
                instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

                for (final ColoredQuad coloredQuad : layer.quads()) {
                    instance.setColor(coloredQuad.color());
                    vertexConsumer.putBakedQuad(pose, coloredQuad.quad(), instance);
                }
            });
        }

        poseStack.popPose();
    }

    private static List<ColoredQuad> collectQuads(
            final ChiseledBlockBakedModel model,
            final BlockPos pos,
            final BlockAndTintGetter level,
            final int baseColor,
            final RandomSource random) {
        final List<ColoredQuad> result = new ArrayList<>();
        for (final Direction direction : Direction.values()) {
            appendQuads(result, model.getQuads(null, direction, random), pos, level, baseColor);
        }
        appendQuads(result, model.getQuads(null, null, random), pos, level, baseColor);
        return result;
    }

    private static void appendQuads(
            final List<ColoredQuad> output,
            final List<BakedQuad> quads,
            final BlockPos pos,
            final BlockAndTintGetter level,
            final int baseColor) {
        for (final BakedQuad quad : quads) {
            int color = 0xffffffff;
            final int packedTint = quad.materialInfo().tintIndex();
            if (packedTint >= 0) {
                final BlockState containedState = ModUtil.getStateById(packedTint >>> 8);
                final BlockTintSource tintSource =
                        Minecraft.getInstance().getBlockColors().getTintSource(containedState, packedTint & 0xff);
                color = ARGB.opaque(tintSource.colorInWorld(containedState, level, pos));
            }
            output.add(new ColoredQuad(quad, ARGB.multiply(color, baseColor)));
        }
    }

    private static RenderType movingRenderType(final ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }

    public static final class RenderState extends BlockEntityRenderState {
        private final List<SubmittedLayer> layers = new ArrayList<>();
    }

    private record SubmittedLayer(RenderType renderType, List<ColoredQuad> quads) {}

    private record ColoredQuad(BakedQuad quad, int color) {}

    private static final class CacheKey {
        private final int blockStateId;
        private final int bitCount;

        private CacheKey(final int blockStateId, final int bitCount) {
            this.blockStateId = blockStateId;
            this.bitCount = bitCount;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CacheKey cacheKey)) {
                return false;
            }
            return blockStateId == cacheKey.blockStateId && bitCount == cacheKey.bitCount;
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockStateId, bitCount);
        }
    }
}
