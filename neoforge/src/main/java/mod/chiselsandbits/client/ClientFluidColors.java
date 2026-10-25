package mod.chiselsandbits.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/** Client-only fluid tint, resolved from the fluid's legacy block tint source. */
public final class ClientFluidColors {

    private ClientFluidColors() {}

    public static int color(final Fluid fluid) {
        final BlockState state = fluid.defaultFluidState().createLegacyBlock();
        if (state.isAir()) {
            return -1;
        }

        final BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(state, 0);
        if (source == null) {
            return -1;
        }

        final int color = source.color(state);
        return (color & 0xff000000) == 0 ? color | 0xff000000 : color;
    }
}
