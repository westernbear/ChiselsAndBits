package mod.chiselsandbits.platform.neoforge;

import mod.chiselsandbits.bitstorage.TileEntityBitStorage;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;

@SuppressWarnings("unused")
public final class PlatformFluidUtilImpl {

    private PlatformFluidUtilImpl() {}

    public static int getColor(Fluid fluid) {
        return -1;
    }

    public static boolean interactWithFluidHandler(Player player, InteractionHand hand, Object handler) {
        return false;
    }

    public static boolean interactWithBitStorage(Player player, InteractionHand hand, TileEntityBitStorage storage) {
        return false;
    }
}
