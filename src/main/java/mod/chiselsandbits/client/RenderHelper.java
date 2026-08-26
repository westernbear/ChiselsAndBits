package mod.chiselsandbits.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.helpers.ModUtil;
import net.fabricmc.fabric.api.client.rendering.v1.FabricOrderedSubmitNodeCollector;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ShapeOutlineFeatureRenderer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Rendering compatibility boundary for the pre-26.2 C&B rendering helpers.
 *
 * <p>Minecraft 26.2 no longer permits the old immediate {@code Tesselator} /
 * {@code MultiBufferSource} path. Geometry is now submitted during the level
 * drawing phase after all world-dependent data has been extracted. These
 * helpers retain every old visual pass while translating it into vanilla
 * submit nodes, which also makes the rendering backend independent (OpenGL or
 * Vulkan).
 */
public final class RenderHelper {

    private static final float LINE_WIDTH = 1.1F;
    private static final RenderType CHISEL_PREVIEW = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    private static final RandomSource RENDER_RANDOM = RandomSource.create();

    /**
     * Legacy public API calls do not receive the 26.2 submit collector. Calls
     * made outside our collect callback are therefore prepared immediately and
     * queued, rather than silently discarded, for the next level submission.
     */
    private static final ThreadLocal<SubmitNodeCollector> ACTIVE_COLLECTOR = new ThreadLocal<>();

    private static final Queue<Consumer<SubmitNodeCollector>> PENDING_SUBMISSIONS = new ConcurrentLinkedQueue<>();

    private RenderHelper() {}

    public static void withSubmitCollector(final SubmitNodeCollector collector, final Runnable submissions) {
        final SubmitNodeCollector previous = ACTIVE_COLLECTOR.get();
        ACTIVE_COLLECTOR.set(collector);

        try {
            Consumer<SubmitNodeCollector> pending;
            while ((pending = PENDING_SUBMISSIONS.poll()) != null) {
                pending.accept(collector);
            }

            submissions.run();
        } finally {
            if (previous == null) {
                ACTIVE_COLLECTOR.remove();
            } else {
                ACTIVE_COLLECTOR.set(previous);
            }
        }
    }

    public static void drawSelectionBoundingBoxIfExists(
            final PoseStack matrixStack,
            final SubmitNodeCollector collector,
            final AABB bb,
            final BlockPos blockPos,
            final boolean normalBoundingBox) {
        drawSelectionBoundingBoxIfExistsWithColor(
                matrixStack, collector, bb, blockPos, normalBoundingBox, 0, 0, 0, 102, 32);
    }

    public static void drawSelectionBoundingBoxIfExists(
            final PoseStack matrixStack,
            final SubmitNodeCollector collector,
            final AABB bb,
            final BlockPos blockPos,
            final Player player,
            final float partialTicks,
            final boolean normalBoundingBox) {
        drawSelectionBoundingBoxIfExistsWithColor(
                matrixStack, collector, bb, blockPos, player, partialTicks, normalBoundingBox, 0, 0, 0, 102, 32);
    }

    public static void drawSelectionBoundingBoxIfExistsWithColor(
            final PoseStack matrixStack,
            final SubmitNodeCollector collector,
            final AABB bb,
            final BlockPos blockPos,
            final boolean normalBoundingBox,
            final int red,
            final int green,
            final int blue,
            final int alpha,
            final int seeThroughAlpha) {
        drawSelectionBoundingBoxIfExistsWithColor(
                matrixStack,
                collector,
                bb,
                blockPos,
                null,
                0.0F,
                normalBoundingBox,
                red,
                green,
                blue,
                alpha,
                seeThroughAlpha);
    }

    public static void drawSelectionBoundingBoxIfExistsWithColor(
            final PoseStack matrixStack,
            final SubmitNodeCollector collector,
            final AABB bb,
            final BlockPos blockPos,
            final Player player,
            final float partialTicks,
            final boolean normalBoundingBox,
            final int red,
            final int green,
            final int blue,
            final int alpha,
            final int seeThroughAlpha) {
        if (bb == null) {
            return;
        }

        final AABB worldBox =
                bb.expandTowards(0.002D, 0.002D, 0.002D).move(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        // The old helper intentionally rendered two passes: a stronger,
        // depth-tested outline and a dim through-wall outline. Preserve both.
        if (!normalBoundingBox) {
            submitBoundingBox(matrixStack, collector, worldBox, red, green, blue, alpha, false);
        }

        submitBoundingBox(matrixStack, collector, worldBox, red, green, blue, seeThroughAlpha, true);
    }

    public static void drawLineWithColor(
            final PoseStack matrixStack,
            final SubmitNodeCollector collector,
            final Vec3 a,
            final Vec3 b,
            final BlockPos blockPos,
            final boolean normalBoundingBox,
            final int red,
            final int green,
            final int blue,
            final int alpha,
            final int seeThroughAlpha) {
        if (a == null || b == null) {
            return;
        }

        final Vec3 a2 = a.add(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        final Vec3 b2 = b.add(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        if (!normalBoundingBox) {
            submitLine(matrixStack, collector, a2, b2, red, green, blue, alpha, false);
        }

        submitLine(matrixStack, collector, a2, b2, red, green, blue, seeThroughAlpha, true);
    }

    public static void submitAlwaysOnTopText(
            final SubmitNodeCollector collector,
            final PoseStack matrixStack,
            final float x,
            final float y,
            final FormattedCharSequence text,
            final boolean dropShadow,
            final Font.DisplayMode displayMode,
            final int lightCoords,
            final int color,
            final int backgroundColor,
            final int outlineColor) {
        final TextFeatureRenderer.Submit submit = new TextFeatureRenderer.Submit(
                new Matrix4f(matrixStack.last().pose()),
                x,
                y,
                text,
                dropShadow,
                displayMode,
                lightCoords,
                color,
                backgroundColor,
                outlineColor);
        fabricCollector(collector).submitCustom(SubmitRenderPhases.ALWAYS_ON_TOP, submit);
    }

    public static void renderBoundingBox(
            final PoseStack matrixStack,
            final SubmitNodeCollector collector,
            final AABB boundingBox,
            final int red,
            final int green,
            final int blue,
            final int alpha) {
        submitBoundingBox(matrixStack, collector, boundingBox, red, green, blue, alpha, false);
    }

    public static void renderLine(
            final PoseStack matrixStack,
            final SubmitNodeCollector collector,
            final Vec3 a,
            final Vec3 b,
            final int red,
            final int green,
            final int blue,
            final int alpha) {
        submitLine(matrixStack, collector, a, b, red, green, blue, alpha, false);
    }

    private static void submitBoundingBox(
            final PoseStack matrixStack,
            final SubmitNodeCollector collector,
            final AABB boundingBox,
            final int red,
            final int green,
            final int blue,
            final int alpha,
            final boolean alwaysOnTop) {
        final ShapeOutlineFeatureRenderer.Submit submit = new ShapeOutlineFeatureRenderer.Submit(
                matrixStack.last().copy(),
                Shapes.create(boundingBox),
                RenderTypes.lines(),
                ARGB.color(alpha, red, green, blue),
                LINE_WIDTH);

        final FabricOrderedSubmitNodeCollector ordered = fabricCollector(collector);
        if (alwaysOnTop) {
            ordered.submitCustom(SubmitRenderPhases.ALWAYS_ON_TOP, submit);
        } else {
            ordered.submitCustom(SubmitRenderPhases.SHAPE_OUTLINES, submit);
        }
    }

    private static void submitLine(
            final PoseStack matrixStack,
            final SubmitNodeCollector collector,
            final Vec3 a,
            final Vec3 b,
            final int red,
            final int green,
            final int blue,
            final int alpha,
            final boolean alwaysOnTop) {
        final Vec3 delta = b.subtract(a);
        if (delta.lengthSqr() == 0.0D) {
            return;
        }

        final int color = ARGB.color(alpha, red, green, blue);
        final Vector3f normal = new Vector3f((float) delta.x, (float) delta.y, (float) delta.z).normalize();
        final CustomFeatureRenderer.Submit submit =
                new CustomFeatureRenderer.Submit(matrixStack.last().copy(), RenderTypes.lines(), (pose, vertices) -> {
                    vertices.addVertex(pose, (float) a.x, (float) a.y, (float) a.z)
                            .setColor(color)
                            .setNormal(pose, normal)
                            .setLineWidth(LINE_WIDTH);
                    vertices.addVertex(pose, (float) b.x, (float) b.y, (float) b.z)
                            .setColor(color)
                            .setNormal(pose, normal)
                            .setLineWidth(LINE_WIDTH);
                });

        final FabricOrderedSubmitNodeCollector ordered = fabricCollector(collector);
        if (alwaysOnTop) {
            ordered.submitCustom(SubmitRenderPhases.ALWAYS_ON_TOP, submit);
        } else {
            ordered.submitCustom(SubmitRenderPhases.TRANSLUCENT_CUSTOM_GEOMETRY, submit);
        }
    }

    /**
     * Resolves a packed C&amp;B tint ({@code stateId << 8 | localTint}) the same way item/block
     * smart models do. Looking up the packed index against the chiseled default state returned null
     * for tinted materials (FCB, stained glass) and crashed ghost previews.
     */
    public static int getTint(final int alpha, final int packedTint, final Level world, final BlockPos blockPos) {
        final BlockState containedState = ModUtil.getStateById(packedTint >>> 8);
        final int tintIndex = packedTint & 0xff;
        final BlockTintSource tintSource =
                Minecraft.getInstance().getBlockColors().getTintSource(containedState, tintIndex);
        if (tintSource == null) {
            return ARGB.color(alpha >>> 24, 255, 255, 255);
        }

        final int tint = world instanceof BlockAndTintGetter tintWorld
                ? tintSource.colorInWorld(containedState, tintWorld, blockPos)
                : tintSource.color(containedState);
        return ARGB.color(alpha >>> 24, ARGB.red(tint), ARGB.green(tint), ARGB.blue(tint));
    }

    public static PreparedModel prepareModel(
            final LegacyBakedModel model, final Level world, final BlockPos blockPos, final int alpha) {
        final List<PreparedQuad> quads = new ArrayList<>();
        for (final Direction direction : Direction.values()) {
            prepareQuads(quads, model.getQuads(null, direction, RENDER_RANDOM), world, blockPos, alpha);
        }
        prepareQuads(quads, model.getQuads(null, null, RENDER_RANDOM), world, blockPos, alpha);
        return new PreparedModel(quads);
    }

    private static void prepareQuads(
            final List<PreparedQuad> output,
            final List<BakedQuad> quads,
            final Level world,
            final BlockPos blockPos,
            final int alpha) {
        for (final BakedQuad quad : quads) {
            final int tintIndex = quad.materialInfo().tintIndex();
            final int color = tintIndex == -1
                    ? ARGB.color(alpha >>> 24, 255, 255, 255)
                    : getTint(alpha, tintIndex, world, blockPos);
            output.add(new PreparedQuad(quad, color));
        }
    }

    /**
     * Retains the legacy API signature. The geometry is prepared immediately;
     * only the backend submission is deferred when no collector is active.
     */
    public static void renderModel(
            final PoseStack matrixStack,
            final LegacyBakedModel model,
            final Level world,
            final BlockPos blockPos,
            final int alpha,
            final int combinedLight,
            final int combinedOverlay) {
        final PreparedModel prepared = prepareModel(model, world, blockPos, alpha);
        final PoseStack.Pose pose = matrixStack.last().copy();
        queueOrSubmit(
                collector -> submitPreparedModel(collector, pose, prepared, combinedLight, combinedOverlay, false));
    }

    public static void renderModel(
            final PoseStack matrixStack,
            final LegacyBakedModel model,
            final Level world,
            final BlockPos blockPos,
            final int alpha,
            final int combinedLight,
            final int combinedOverlay,
            final SubmitNodeCollector collector,
            final boolean alwaysOnTop) {
        submitPreparedModel(
                collector,
                matrixStack.last().copy(),
                prepareModel(model, world, blockPos, alpha),
                combinedLight,
                combinedOverlay,
                alwaysOnTop);
    }

    public static void renderGhostModel(
            final PoseStack matrixStack,
            final LegacyBakedModel model,
            final Level world,
            final BlockPos blockPos,
            final boolean isUnplaceable,
            final int combinedLight,
            final int combinedOverlay) {
        final int alpha = isUnplaceable ? 0x22000000 : 0xaa000000;
        renderModel(matrixStack, model, world, blockPos, alpha, combinedLight, combinedOverlay);
    }

    public static void renderGhostModel(
            final PoseStack matrixStack,
            final LegacyBakedModel model,
            final Level world,
            final BlockPos blockPos,
            final boolean isUnplaceable,
            final int combinedLight,
            final int combinedOverlay,
            final SubmitNodeCollector collector,
            final boolean alwaysOnTop) {
        final int alpha = isUnplaceable ? 0x22000000 : 0xaa000000;
        renderModel(matrixStack, model, world, blockPos, alpha, combinedLight, combinedOverlay, collector, alwaysOnTop);
    }

    public static void submitPreparedModel(
            final SubmitNodeCollector collector,
            final PoseStack.Pose pose,
            final PreparedModel model,
            final int combinedLight,
            final int combinedOverlay,
            final boolean alwaysOnTop) {
        final CustomFeatureRenderer.Submit submit = new CustomFeatureRenderer.Submit(
                pose,
                CHISEL_PREVIEW,
                (renderPose, vertices) ->
                        renderPreparedQuads(renderPose, vertices, model, combinedLight, combinedOverlay));

        final FabricOrderedSubmitNodeCollector ordered = fabricCollector(collector);
        if (alwaysOnTop) {
            ordered.submitCustom(SubmitRenderPhases.ALWAYS_ON_TOP, submit);
        } else {
            ordered.submitCustom(SubmitRenderPhases.TRANSLUCENT_CUSTOM_GEOMETRY, submit);
        }
    }

    private static void renderPreparedQuads(
            final PoseStack.Pose pose,
            final VertexConsumer vertices,
            final PreparedModel model,
            final int combinedLight,
            final int combinedOverlay) {
        final QuadInstance quadInstance = new QuadInstance();
        quadInstance.setLightCoords(combinedLight);
        quadInstance.setOverlayCoords(combinedOverlay);

        for (final PreparedQuad preparedQuad : model.quads()) {
            quadInstance.setColor(preparedQuad.color());
            vertices.putBakedQuad(pose, preparedQuad.quad(), quadInstance);
        }
    }

    private static void queueOrSubmit(final Consumer<SubmitNodeCollector> submission) {
        final SubmitNodeCollector collector = ACTIVE_COLLECTOR.get();
        if (collector == null) {
            PENDING_SUBMISSIONS.add(submission);
        } else {
            submission.accept(collector);
        }
    }

    private static FabricOrderedSubmitNodeCollector fabricCollector(final SubmitNodeCollector collector) {
        final OrderedSubmitNodeCollector ordered = collector.order(0);
        return (FabricOrderedSubmitNodeCollector) ordered;
    }

    public record PreparedQuad(BakedQuad quad, int color) {}

    public record PreparedModel(List<PreparedQuad> quads) {
        public PreparedModel {
            quads = List.copyOf(quads);
        }
    }
}
