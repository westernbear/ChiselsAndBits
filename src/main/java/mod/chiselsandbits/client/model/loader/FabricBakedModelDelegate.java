package mod.chiselsandbits.client.model.loader;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.model.baked.DataAwareBakedModel;
import mod.chiselsandbits.client.model.data.IModelData;
import mod.chiselsandbits.interfaces.ICacheClearable;
import mod.chiselsandbits.render.chiseledblock.ChiselRenderType;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public class FabricBakedModelDelegate extends ForwardingBakedModel implements ICacheClearable {
    private boolean cached = false;

    public FabricBakedModelDelegate(final BakedModel delegate) {
        this.wrapped = delegate;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(
            final BlockAndTintGetter blockAndTintGetter,
            final BlockState blockState,
            final BlockPos blockPos,
            final Supplier<RandomSource> supplier,
            final RenderContext renderContext) {
        if (!(wrapped instanceof final DataAwareBakedModel dataAwareBakedModel)) {
            wrapped.emitBlockQuads(blockAndTintGetter, blockState, blockPos, supplier, renderContext);
            return;
        }

        Object attachmentData = blockAndTintGetter.getBlockEntityRenderData(blockPos);

        if (attachmentData instanceof IModelData modelData) {
            synchronized (dataAwareBakedModel) {
                dataAwareBakedModel.updateModelData(blockAndTintGetter, blockPos, blockState, modelData);
                emitBlockQuads(
                        dataAwareBakedModel,
                        modelData,
                        blockAndTintGetter,
                        blockState,
                        blockPos,
                        supplier,
                        renderContext);
            }
        }
    }

    public void emitBlockQuads(
            final DataAwareBakedModel dataAwareBakedModel,
            final IModelData blockModelData,
            final BlockAndTintGetter world,
            final BlockState blockState,
            final BlockPos blockPos,
            final Supplier<RandomSource> supplier,
            final RenderContext renderContext) {
        if (!cached) {
            BlockBitInfo.recalculate();
            VoxelBlob.clearCache();
            cached = true;
        }

        Set<ChiselRenderType> renderTypes =
                dataAwareBakedModel.getRenderTypes(world, blockPos, blockState, blockModelData);

        for (Direction direction : Direction.values()) {
            renderTypes.forEach(renderType -> emitBlockQuads(
                    dataAwareBakedModel,
                    blockModelData,
                    blockState,
                    blockPos,
                    direction,
                    supplier,
                    renderContext,
                    renderType,
                    RendererAccess.INSTANCE
                            .getRenderer()
                            .materialFinder()
                            .blendMode(BlendMode.fromRenderLayer(renderType.layer))
                            .find()));
        }

        renderTypes.forEach(renderType -> emitBlockQuads(
                dataAwareBakedModel,
                blockModelData,
                blockState,
                blockPos,
                null,
                supplier,
                renderContext,
                renderType,
                RendererAccess.INSTANCE
                        .getRenderer()
                        .materialFinder()
                        .blendMode(BlendMode.fromRenderLayer(renderType.layer))
                        .find()));
    }

    public void emitBlockQuads(
            final DataAwareBakedModel dataAwareBakedModel,
            final IModelData blockModelData,
            final BlockState blockState,
            final BlockPos blockPos,
            final Direction direction,
            final Supplier<RandomSource> supplier,
            final RenderContext renderContext,
            ChiselRenderType renderType,
            RenderMaterial renderMaterial) {
        final List<BakedQuad> quads =
                dataAwareBakedModel.getQuads(blockState, direction, supplier.get(), blockModelData, renderType);

        final RenderMaterial material = renderMaterial;

        quads.forEach(quad -> {
            final MeshBuilder meshBuilder =
                    RendererAccess.INSTANCE.getRenderer().meshBuilder();
            final QuadEmitter emitter = meshBuilder.getEmitter();
            emitter.fromVanilla(quad, material, direction);
            emitter.emit();
            renderContext.meshConsumer().accept(meshBuilder.build());
        });
    }

    @Override
    public void emitItemQuads(
            final ItemStack itemStack, final Supplier<RandomSource> supplier, final RenderContext renderContext) {
        if (!cached) {
            BlockBitInfo.recalculate();
            VoxelBlob.clearCache();
            cached = true;
        }
        final BakedModel itemModel = wrapped.getOverrides()
                .resolve(
                        wrapped,
                        itemStack,
                        Minecraft.getInstance().level,
                        Minecraft.getInstance().player,
                        supplier.get().nextInt());

        renderContext.pushTransform(quad -> true);
        if (itemModel != null) {
            itemModel.emitItemQuads(itemStack, supplier, renderContext);
        }

        renderContext.popTransform();
    }

    @Override
    public void clearCache() {
        if (wrapped instanceof ICacheClearable cache) {
            cache.clearCache();
        }
    }
}
