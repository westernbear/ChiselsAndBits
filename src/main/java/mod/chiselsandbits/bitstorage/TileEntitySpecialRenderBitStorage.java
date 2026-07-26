package mod.chiselsandbits.bitstorage;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntitySpecialRenderBitStorage implements BlockEntityRenderer<TileEntityBitStorage> {

    private static final SimpleMaxSizedCache<CacheKey, VoxelBlob> STORAGE_CONTENTS_BLOB_CACHE =
            new SimpleMaxSizedCache<>(ChiselsAndBits.getConfig()
                    .getClient()
                    .bitStorageContentCacheSize
                    .get());

    public TileEntitySpecialRenderBitStorage(BlockEntityRenderDispatcher dispatcher) {}

    @Override
    public void render(
            final TileEntityBitStorage te,
            final float partialTicks,
            final PoseStack matrixStackIn,
            final MultiBufferSource buffer,
            final int combinedLightIn,
            final int combinedOverlayIn) {
        final int bits = te.getBits();
        final BlockState state = te.getMyFluid() == null
                ? te.getState()
                : te.getMyFluid().defaultFluidState().createLegacyBlock();
        if (bits <= 0 || state == null) {
            return;
        }

        VoxelBlob innerModelBlob = STORAGE_CONTENTS_BLOB_CACHE.get(new CacheKey(ModUtil.getStateId(state), bits));
        if (innerModelBlob == null) {
            innerModelBlob = new VoxelBlob();
            innerModelBlob.fillAmountFromBottom(ModUtil.getStateId(state), bits);
            STORAGE_CONTENTS_BLOB_CACHE.put(new CacheKey(ModUtil.getStateId(state), bits), innerModelBlob);
        }

        matrixStackIn.pushPose();
        matrixStackIn.translate(2 / 16f, 2 / 16f, 2 / 16f);
        matrixStackIn.scale(12 / 16f, 12 / 16f, 12 / 16f);
        final VoxelBlob finalInnerModelBlob = innerModelBlob;
        final RandomSource random =
                te.getLevel() == null ? RandomSource.create(0L) : te.getLevel().getRandom();
        RenderType.chunkBufferLayers().forEach(renderType -> {
            final ChiseledBlockBakedModel innerModel = ChiseledBlockSmartModel.getCachedModel(
                    ModUtil.getStateId(state),
                    finalInnerModelBlob,
                    ChiselRenderType.fromLayer(renderType, te.getMyFluid() != null),
                    DefaultVertexFormat.BLOCK,
                    random);

            if (!innerModel.isEmpty()) {
                final float r =
                        te.getMyFluid() == null ? 1f : ((FluidUtil.getColor(te.getMyFluid()) >> 16) & 0xff) / 255F;
                final float g =
                        te.getMyFluid() == null ? 1f : ((FluidUtil.getColor(te.getMyFluid()) >> 8) & 0xff) / 255f;
                final float b = te.getMyFluid() == null ? 1f : ((FluidUtil.getColor(te.getMyFluid())) & 0xff) / 255f;
                Minecraft.getInstance()
                        .getBlockRenderer()
                        .getModelRenderer()
                        .renderModel(
                                matrixStackIn.last(),
                                buffer.getBuffer(renderType),
                                state,
                                innerModel,
                                r,
                                g,
                                b,
                                combinedLightIn,
                                combinedOverlayIn);
            }
        });
        matrixStackIn.popPose();
    }

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
