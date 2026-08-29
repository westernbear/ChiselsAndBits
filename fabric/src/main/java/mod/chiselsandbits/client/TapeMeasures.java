package mod.chiselsandbits.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import mod.chiselsandbits.chiseledblock.data.BitLocation;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.DeprecationHelper;
import mod.chiselsandbits.modes.IToolMode;
import mod.chiselsandbits.modes.TapeMeasureModes;
import mod.chiselsandbits.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Stores tape measurements and splits their 26.2 rendering into extraction and
 * submission. World/player access remains in {@link #extractRenderState(float)};
 * the drawing phase consumes only immutable coordinates, colors and labels.
 */
public class TapeMeasures {
    private static final double BLOCK_SIZE = 1.0;
    private static final double BIT_SIZE = 1.0 / 16.0;
    private static final double HALF_BIT = BIT_SIZE / 2.0F;
    private static final double LETTER_SIZE = 5.0;
    private static final float LABEL_Z_SCALE = 0.001F;

    private final ArrayList<Measure> measures = new ArrayList<>();
    private Measure preview;

    private static double AABBDistnace(final Vec3 eyes, final AABB box) {
        // Snap the eye position into the box, then measure to that closest point.
        final double boxPointX = Math.min(box.maxX, Math.max(box.minX, eyes.x));
        final double boxPointY = Math.min(box.maxY, Math.max(box.minY, eyes.y));
        final double boxPointZ = Math.min(box.maxZ, Math.max(box.minZ, eyes.z));
        return Math.sqrt(eyes.distanceToSqr(boxPointX, boxPointY, boxPointZ));
    }

    private static double getLineDistance(final Vec3 v, final Vec3 w, final Player player, final float partialTicks) {
        final Vec3 p = player.getEyePosition(partialTicks);
        final double segmentLength = v.distanceToSqr(w);

        if (segmentLength == 0.0) {
            return p.distanceTo(v);
        }

        final double t = Math.max(0, Math.min(1, p.subtract(v).dot(w.subtract(v)) / segmentLength));
        final Vec3 projection = v.add(w.subtract(v).scale(t));
        return p.distanceTo(projection);
    }

    public void clear() {
        measures.clear();
    }

    public void setPreviewMeasure(
            final BitLocation a, final BitLocation b, final IToolMode chMode, final ItemStack itemStack) {
        final Player player = ClientSide.instance.getPlayer();

        if (a == null || b == null || player == null) {
            preview = null;
        } else {
            preview = new Measure(a, b, chMode, getDimension(player), getColor(itemStack));
        }
    }

    public void addMeasure(
            final BitLocation a, final BitLocation b, final IToolMode chMode, final ItemStack itemStack) {
        final Player player = ClientSide.instance.getPlayer();
        if (player == null) {
            return;
        }

        while (!measures.isEmpty()
                && measures.size()
                        >= ChiselsAndBits.getConfig()
                                .getClient()
                                .maxTapeMeasures
                                .get()) {
            measures.remove(0);
        }

        final Measure newMeasure = new Measure(a, b, chMode, getDimension(player), getColor(itemStack));

        if (ChiselsAndBits.getConfig().getClient().displayMeasuringTapeInChat.get()) {
            final AABB box = newMeasure.getBoundingBox();
            final double lenX = box.maxX - box.minX;
            final double lenY = box.maxY - box.minY;
            final double lenZ = box.maxZ - box.minZ;
            final double len = newMeasure.getVecA().distanceTo(newMeasure.getVecB());

            final String out = chMode == TapeMeasureModes.DISTANCE
                    ? getSize(len)
                    : DeprecationHelper.translateToLocal(
                            "mod.chiselsandbits.tapemeasure.chatmsg", getSize(lenX), getSize(lenY), getSize(lenZ));

            final MutableComponent chatMsg = Component.literal(out);
            // Retain the tape dye color used by the original chat notification.
            chatMsg.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(newMeasure.color.getTextColor())));
            player.sendSystemMessage(chatMsg);
        }

        measures.add(newMeasure);
    }

    private DyeColor getColor(final ItemStack itemStack) {
        return ModItems.ITEM_TAPE_MEASURE.get().getTapeColor(itemStack);
    }

    private Identifier getDimension(final Player player) {
        return player.level().dimension().registry();
    }

    /**
     * Extracts the same sorted, distance-faded geometry the old immediate
     * renderer produced. The returned state is safe to consume in the drawing
     * phase without touching the level.
     */
    public RenderState extractRenderState(final float partialTicks) {
        if (measures.isEmpty() && preview == null) {
            return RenderState.EMPTY;
        }

        final Player player = ClientSide.instance.getPlayer();
        if (player == null || !hasTapeMeasure(player.getInventory())) {
            return RenderState.EMPTY;
        }

        final ArrayList<Measure> sorted = new ArrayList<>(measures.size() + 1);
        if (preview != null) {
            preview.calcDistance(partialTicks);
            sorted.add(preview);
        }

        for (final Measure measure : measures) {
            measure.calcDistance(partialTicks);
            sorted.add(measure);
        }

        sorted.sort(Comparator.comparingDouble((Measure measure) -> measure.distance)
                .reversed());

        final List<MeasureRenderState> renderStates = new ArrayList<>(sorted.size());
        for (final Measure measure : sorted) {
            final MeasureRenderState state = extractMeasure(measure, measure.distance, player);
            if (state != null) {
                renderStates.add(state);
            }
        }

        return renderStates.isEmpty() ? RenderState.EMPTY : new RenderState(renderStates);
    }

    private boolean hasTapeMeasure(final Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == ModItems.ITEM_TAPE_MEASURE.get()) {
                return true;
            }
        }

        return false;
    }

    private MeasureRenderState extractMeasure(final Measure measure, final double distance, final Player player) {
        if (!measure.dimensionId.equals(getDimension(player))) {
            return null;
        }

        final int alpha = getAlphaFromRange(distance);
        if (alpha < 30) {
            return null;
        }

        final int value = measure.color.getTextColor();
        final int red = value >> 16 & 0xff;
        final int green = value >> 8 & 0xff;
        final int blue = value & 0xff;
        final List<LabelRenderState> labels = new ArrayList<>(3);

        if (measure.mode == TapeMeasureModes.DISTANCE) {
            final Vec3 a = measure.getVecA();
            final Vec3 b = measure.getVecB();
            final double len = a.distanceTo(b) + BIT_SIZE;
            labels.add(createLabel((a.x + b.x) * 0.5, (a.y + b.y) * 0.5, (a.z + b.z) * 0.5, len, red, green, blue));
            return new MeasureRenderState(true, a, b, null, red, green, blue, alpha, labels);
        }

        final AABB box = measure.getBoundingBox();
        final double lenX = box.maxX - box.minX;
        final double lenY = box.maxY - box.minY;
        final double lenZ = box.maxZ - box.minZ;

        // Retain the original label placement: Y, X and Z dimensions in that order.
        labels.add(createLabel(box.minX, (box.maxY + box.minY) * 0.5, box.minZ, lenY, red, green, blue));
        labels.add(createLabel((box.minX + box.maxX) * 0.5, box.minY, box.minZ, lenX, red, green, blue));
        labels.add(createLabel(box.minX, box.minY, (box.minZ + box.maxZ) * 0.5, lenZ, red, green, blue));

        return new MeasureRenderState(false, null, null, box, red, green, blue, alpha, labels);
    }

    private LabelRenderState createLabel(
            final double x,
            final double y,
            final double z,
            final double len,
            final int red,
            final int green,
            final int blue) {
        final String text = getSize(len);
        final float scale = getScale(len);
        final float centeredX = -Minecraft.getInstance().font.width(text) * 0.5F;
        return new LabelRenderState(
                x, y + scale * LETTER_SIZE, z, scale, centeredX, text, ARGB.color(255, red, green, blue));
    }

    /** Draws an immutable extracted state through vanilla/Fabric submit nodes. */
    public void submitRenderState(
            final RenderState state,
            final PoseStack matrixStack,
            final SubmitNodeCollector collector,
            final CameraRenderState camera) {
        if (state.measures().isEmpty() || camera.pos == null) {
            return;
        }

        matrixStack.pushPose();
        matrixStack.translate(-camera.pos.x, -camera.pos.y, -camera.pos.z);

        for (final MeasureRenderState measure : state.measures()) {
            if (measure.distanceMode()) {
                RenderHelper.drawLineWithColor(
                        matrixStack,
                        collector,
                        measure.lineStart(),
                        measure.lineEnd(),
                        BlockPos.ZERO,
                        false,
                        measure.red(),
                        measure.green(),
                        measure.blue(),
                        measure.alpha(),
                        (int) (measure.alpha() / 3.4));
            } else {
                RenderHelper.drawSelectionBoundingBoxIfExistsWithColor(
                        matrixStack,
                        collector,
                        measure.box().expandTowards(-0.001, -0.001, -0.001),
                        BlockPos.ZERO,
                        false,
                        measure.red(),
                        measure.green(),
                        measure.blue(),
                        measure.alpha(),
                        (int) (measure.alpha() / 3.4));
            }

            for (final LabelRenderState label : measure.labels()) {
                submitLabel(matrixStack, collector, camera, label);
            }
        }

        matrixStack.popPose();
    }

    private void submitLabel(
            final PoseStack matrixStack,
            final SubmitNodeCollector collector,
            final CameraRenderState camera,
            final LabelRenderState label) {
        matrixStack.pushPose();
        matrixStack.translate(label.x(), label.y(), label.z());
        matrixStack.mulPose(Axis.YP.rotationDegrees(180.0F - camera.yRot));
        matrixStack.mulPose(Axis.XP.rotationDegrees(-camera.xRot));
        matrixStack.scale(label.scale(), -label.scale(), LABEL_Z_SCALE);
        matrixStack.translate(label.centeredX(), 0.0F, 0.0F);

        RenderHelper.submitAlwaysOnTopText(
                collector,
                matrixStack,
                0.0F,
                0.0F,
                Component.literal(label.text()).getVisualOrderText(),
                true,
                Font.DisplayMode.NORMAL,
                LightCoordsUtil.FULL_BRIGHT,
                label.color(),
                0,
                0);

        matrixStack.popPose();
    }

    private int getAlphaFromRange(final double distance) {
        if (distance < 16) {
            return 102;
        }

        return (int) (102 - (distance - 16) * 6);
    }

    private float getScale(final double maxLen) {
        final double maxFontSize = 0.04;
        final double minFontSize = 0.004;
        final double delta = Math.min(1.0, maxLen / 4.0);
        double scale = maxFontSize * delta + minFontSize * (1.0 - delta);

        if (maxLen < 0.25) {
            scale = minFontSize;
        }

        return (float) Math.min(maxFontSize, scale);
    }

    private String getSize(final double value) {
        final double blocks = Math.floor(value);
        final double bits = value - blocks;
        final StringBuilder result = new StringBuilder();

        if (blocks > 0) {
            result.append((int) blocks).append("m");
        }

        if (bits * 16 > 0.9999) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            result.append((int) (bits * 16)).append("b");
        }

        return result.toString();
    }

    public record RenderState(List<MeasureRenderState> measures) {
        private static final RenderState EMPTY = new RenderState(List.of());

        public RenderState {
            measures = List.copyOf(measures);
        }
    }

    public record MeasureRenderState(
            boolean distanceMode,
            Vec3 lineStart,
            Vec3 lineEnd,
            AABB box,
            int red,
            int green,
            int blue,
            int alpha,
            List<LabelRenderState> labels) {
        public MeasureRenderState {
            labels = List.copyOf(labels);
        }
    }

    public record LabelRenderState(
            double x, double y, double z, float scale, float centeredX, String text, int color) {}

    private final class Measure {
        private final IToolMode mode;
        private final BitLocation a;
        private final BitLocation b;
        private final DyeColor color;
        private final Identifier dimensionId;
        private double distance = 1;

        private Measure(
                final BitLocation a,
                final BitLocation b,
                final IToolMode mode,
                final Identifier dimensionId,
                final DyeColor color) {
            this.a = a;
            this.b = b;
            this.mode = mode;
            this.dimensionId = dimensionId;
            this.color = color;
        }

        private AABB getBoundingBox() {
            if (mode == TapeMeasureModes.BLOCK) {
                final double ax = a.blockPos.getX();
                final double ay = a.blockPos.getY();
                final double az = a.blockPos.getZ();
                final double bx = b.blockPos.getX();
                final double by = b.blockPos.getY();
                final double bz = b.blockPos.getZ();

                return new AABB(
                        Math.min(ax, bx),
                        Math.min(ay, by),
                        Math.min(az, bz),
                        Math.max(ax, bx) + BLOCK_SIZE,
                        Math.max(ay, by) + BLOCK_SIZE,
                        Math.max(az, bz) + BLOCK_SIZE);
            }

            final double ax = a.blockPos.getX() + BIT_SIZE * a.bitX;
            final double ay = a.blockPos.getY() + BIT_SIZE * a.bitY;
            final double az = a.blockPos.getZ() + BIT_SIZE * a.bitZ;
            final double bx = b.blockPos.getX() + BIT_SIZE * b.bitX;
            final double by = b.blockPos.getY() + BIT_SIZE * b.bitY;
            final double bz = b.blockPos.getZ() + BIT_SIZE * b.bitZ;

            return new AABB(
                    Math.min(ax, bx),
                    Math.min(ay, by),
                    Math.min(az, bz),
                    Math.max(ax, bx) + BIT_SIZE,
                    Math.max(ay, by) + BIT_SIZE,
                    Math.max(az, bz) + BIT_SIZE);
        }

        private Vec3 getVecA() {
            return new Vec3(
                    a.blockPos.getX() + BIT_SIZE * a.bitX + HALF_BIT,
                    a.blockPos.getY() + BIT_SIZE * a.bitY + HALF_BIT,
                    a.blockPos.getZ() + BIT_SIZE * a.bitZ + HALF_BIT);
        }

        private Vec3 getVecB() {
            return new Vec3(
                    b.blockPos.getX() + BIT_SIZE * b.bitX + HALF_BIT,
                    b.blockPos.getY() + BIT_SIZE * b.bitY + HALF_BIT,
                    b.blockPos.getZ() + BIT_SIZE * b.bitZ + HALF_BIT);
        }

        private void calcDistance(final float partialTicks) {
            final Player player = ClientSide.instance.getPlayer();
            if (player == null) {
                distance = Double.POSITIVE_INFINITY;
                return;
            }

            if (mode == TapeMeasureModes.DISTANCE) {
                distance = getLineDistance(getVecA(), getVecB(), player, partialTicks);
            } else {
                final Vec3 eyes = player.getEyePosition(partialTicks);
                final AABB box = getBoundingBox();
                distance = box.contains(eyes) ? 0.0 : AABBDistnace(eyes, box);
            }
        }
    }
}
