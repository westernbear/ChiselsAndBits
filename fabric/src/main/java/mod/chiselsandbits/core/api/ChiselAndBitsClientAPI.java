package mod.chiselsandbits.core.api;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.chiselsandbits.api.IChiselAndBitsClientAPI;
import mod.chiselsandbits.api.ModKeyBinding;
import mod.chiselsandbits.client.RenderHelper;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.modes.ChiselMode;
import mod.chiselsandbits.modes.PositivePatternMode;
import mod.chiselsandbits.modes.TapeMeasureModes;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ChiselAndBitsClientAPI implements IChiselAndBitsClientAPI {

    @Override
    public @NotNull KeyMapping getKeyBinding(@NotNull ModKeyBinding modKeyBinding) {
        if (ClientSide.instance == null) {
            throw new IllegalStateException("ClientSide is not initialized");
        }
        switch (modKeyBinding) {
            case SINGLE:
                return (KeyMapping) ChiselMode.SINGLE.binding;
            case SNAP2:
                return (KeyMapping) ChiselMode.SNAP2.binding;
            case SNAP4:
                return (KeyMapping) ChiselMode.SNAP4.binding;
            case SNAP8:
                return (KeyMapping) ChiselMode.SNAP8.binding;
            case LINE:
                return (KeyMapping) ChiselMode.LINE.binding;
            case PLANE:
                return (KeyMapping) ChiselMode.PLANE.binding;
            case CONNECTED_PLANE:
                return (KeyMapping) ChiselMode.CONNECTED_PLANE.binding;
            case CUBE_SMALL:
                return (KeyMapping) ChiselMode.CUBE_SMALL.binding;
            case CUBE_MEDIUM:
                return (KeyMapping) ChiselMode.CUBE_MEDIUM.binding;
            case CUBE_LARGE:
                return (KeyMapping) ChiselMode.CUBE_LARGE.binding;
            case SAME_MATERIAL:
                return (KeyMapping) ChiselMode.SAME_MATERIAL.binding;
            case DRAWN_REGION:
                return (KeyMapping) ChiselMode.DRAWN_REGION.binding;
            case CONNECTED_MATERIAL:
                return (KeyMapping) ChiselMode.CONNECTED_MATERIAL.binding;
            case REPLACE:
                return (KeyMapping) PositivePatternMode.REPLACE.binding;
            case ADDITIVE:
                return (KeyMapping) PositivePatternMode.ADDITIVE.binding;
            case PLACEMENT:
                return (KeyMapping) PositivePatternMode.PLACEMENT.binding;
            case IMPOSE:
                return (KeyMapping) PositivePatternMode.IMPOSE.binding;
            case BIT:
                return (KeyMapping) TapeMeasureModes.BIT.binding;
            case BLOCK:
                return (KeyMapping) TapeMeasureModes.BLOCK.binding;
            case DISTANCE:
                return (KeyMapping) TapeMeasureModes.DISTANCE.binding;
            default:
                return ClientSide.instance.getKeyBinding(modKeyBinding);
        }
    }

    @Override
    public void renderModel(
            final PoseStack stack,
            final LegacyBakedModel model,
            final Level world,
            final BlockPos pos,
            final int alpha,
            final int combinedLight,
            final int combinedOverlay) {
        final int clampedAlpha = Math.max(0, Math.min(alpha, 255));
        RenderHelper.renderModel(stack, model, world, pos, clampedAlpha << 24, combinedLight, combinedOverlay);
    }

    @Override
    public void renderGhostModel(
            final PoseStack stack,
            final LegacyBakedModel model,
            final Level world,
            final BlockPos pos,
            final boolean isUnplaceable,
            final int combinedLight,
            final int combinedOverlay) {
        RenderHelper.renderGhostModel(stack, model, world, pos, isUnplaceable, combinedLight, combinedOverlay);
    }
}
