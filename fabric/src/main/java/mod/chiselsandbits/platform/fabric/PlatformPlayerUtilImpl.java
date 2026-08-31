package mod.chiselsandbits.platform.fabric;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("unused")
public final class PlatformPlayerUtilImpl {

    private PlatformPlayerUtilImpl() {}

    public static boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer;
    }
}
