package mod.chiselsandbits.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.client.model.data.IModelData;
import mod.chiselsandbits.render.SmartModelManager;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockGeometry;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;

/** Renders the dynamic chiseled-block geometry, replacing the (empty) block model on NeoForge. */
public final class ChiseledBlockRenderer
        implements BlockEntityRenderer<TileEntityBlockChiseled, ChiseledBlockRenderer.State> {

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            final TileEntityBlockChiseled blockEntity,
            final State state,
            final float partialTick,
            final Vec3 cameraPos,
            final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);

        state.groups = List.of();
        final Level level = blockEntity.getLevel();
        final Object renderData = blockEntity.getRenderData();
        final SmartModelManager.Installed installed =
                SmartModelManager.getInstance().installed();
        if (!(level instanceof BlockAndTintGetter tintLevel)
                || !(renderData instanceof IModelData modelData)
                || installed == null) {
            return;
        }

        ChiseledBlockGeometry.initializeCaches();
        final List<ChiseledBlockGeometry.RenderedQuad> quads = ChiseledBlockGeometry.collect(
                installed.chiseledModel(),
                tintLevel,
                blockEntity.getBlockPos(),
                blockEntity.getBlockState(),
                RandomSource.create(42L),
                direction -> true,
                modelData);

        state.groups = Group.byLayer(quads);
        state.light = LightCoordsUtil.getLightCoords(level, blockEntity.getBlockPos());
    }

    @Override
    public void submit(
            final State state,
            final PoseStack poseStack,
            final SubmitNodeCollector collector,
            final CameraRenderState cameraRenderState) {
        for (final Group group : state.groups) {
            collector.submitCustomGeometry(poseStack, group.renderType(), (pose, buffer) -> {
                final QuadInstance instance = new QuadInstance();
                instance.setLightCoords(state.light);
                instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
                for (final ChiseledBlockGeometry.RenderedQuad quad : group.quads()) {
                    instance.setColor(quad.tintColor() == -1 ? -1 : quad.tintColor());
                    buffer.putBakedQuad(pose, quad.quad(), instance);
                }
            });
        }
    }

    public static final class State extends BlockEntityRenderState {
        private List<Group> groups = List.of();
        private int light;
    }

    private record Group(RenderType renderType, List<ChiseledBlockGeometry.RenderedQuad> quads) {

        private static List<Group> byLayer(final List<ChiseledBlockGeometry.RenderedQuad> quads) {
            final Map<ChunkSectionLayer, List<ChiseledBlockGeometry.RenderedQuad>> byLayer = new LinkedHashMap<>();
            for (final ChiseledBlockGeometry.RenderedQuad quad : quads) {
                byLayer.computeIfAbsent(quad.layer(), ignored -> new ArrayList<>())
                        .add(quad);
            }

            final List<Group> groups = new ArrayList<>(byLayer.size());
            byLayer.forEach((layer, layerQuads) -> groups.add(new Group(renderType(layer), List.copyOf(layerQuads))));
            return List.copyOf(groups);
        }

        private static RenderType renderType(final ChunkSectionLayer layer) {
            return switch (layer) {
                case SOLID -> NeoForgeRenderTypes.SOLID_BLOCK_SHEET;
                case CUTOUT -> Sheets.cutoutBlockItemSheet();
                case TRANSLUCENT -> Sheets.translucentBlockItemSheet();
            };
        }
    }
}
