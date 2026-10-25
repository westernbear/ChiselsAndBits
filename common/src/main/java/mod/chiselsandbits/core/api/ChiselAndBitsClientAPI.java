package mod.chiselsandbits.core.api;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mod.chiselsandbits.api.IChiselAndBitsClientAPI;
import mod.chiselsandbits.api.ModKeyBinding;
import mod.chiselsandbits.client.RenderHelper;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.modes.ChiselMode;
import mod.chiselsandbits.modes.IToolMode;
import mod.chiselsandbits.modes.PositivePatternMode;
import mod.chiselsandbits.modes.TapeMeasureModes;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ChiselAndBitsClientAPI implements IChiselAndBitsClientAPI {

    private static final Map<ModKeyBinding, IToolMode> MODE_BINDINGS = buildModeBindings();

    private static Map<ModKeyBinding, IToolMode> buildModeBindings() {
        final List<IToolMode> modes = new ArrayList<>();
        Collections.addAll(modes, ChiselMode.values());
        Collections.addAll(modes, PositivePatternMode.values());
        Collections.addAll(modes, TapeMeasureModes.values());

        final Map<ModKeyBinding, IToolMode> bindings = new EnumMap<>(ModKeyBinding.class);
        for (final IToolMode mode : modes) {
            bindings.put(ModKeyBinding.valueOf(mode.name()), mode);
        }
        return bindings;
    }

    @Override
    public @NotNull KeyMapping getKeyBinding(@NotNull ModKeyBinding modKeyBinding) {
        final IToolMode mode = MODE_BINDINGS.get(modKeyBinding);
        if (mode != null) {
            return (KeyMapping) mode.binding();
        }

        if (ClientSide.getInstance() == null) {
            throw new IllegalStateException("ClientSide is not initialized");
        }
        return ClientSide.getInstance().getKeyBinding(modKeyBinding);
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
