package mod.chiselsandbits.platform.neoforge;

import mod.chiselsandbits.bitstorage.TileEntityBitStorage;
import mod.chiselsandbits.client.ClientFluidColors;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.fluids.FluidUtil;

@SuppressWarnings("unused")
public final class PlatformFluidUtilImpl {

    private PlatformFluidUtilImpl() {}

    public static int getColor(final Fluid fluid) {
        return FMLEnvironment.getDist().isClient() ? ClientFluidColors.color(fluid) : -1;
    }

    // FluidUtil is marked for removal in favour of the transfer API; the new API has no
    // equivalent player-in-hand helper yet, so keep the working call until it lands.
    @SuppressWarnings("removal")
    public static boolean interactWithBitStorage(
            final Player player, final InteractionHand hand, final TileEntityBitStorage storage) {
        final Level level = storage.getLevel();
        return level != null && FluidUtil.interactWithFluidHandler(player, hand, level, storage.getBlockPos(), null);
    }
}
