package mod.chiselsandbits.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import mod.chiselsandbits.bitstorage.TileEntityBitStorage;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;

public final class PlatformFluidUtil {

    private PlatformFluidUtil() {}

    @ExpectPlatform
    public static int getColor(Fluid fluid) {
        throw new AssertionError("ExpectPlatform implementation missing");
    }

    @ExpectPlatform
    public static boolean interactWithFluidHandler(Player player, InteractionHand hand, Object handler) {
        throw new AssertionError("ExpectPlatform implementation missing");
    }

    @ExpectPlatform
    public static boolean interactWithBitStorage(Player player, InteractionHand hand, TileEntityBitStorage storage) {
        throw new AssertionError("ExpectPlatform implementation missing");
    }
}
