package mod.chiselsandbits.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;

public final class PlatformPlayerUtil {

    private PlatformPlayerUtil() {}

    @ExpectPlatform
    public static boolean isFakePlayer(Player player) {
        throw new AssertionError("ExpectPlatform implementation missing");
    }
}
