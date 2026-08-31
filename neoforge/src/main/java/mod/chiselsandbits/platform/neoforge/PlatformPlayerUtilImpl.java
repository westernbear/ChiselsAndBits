package mod.chiselsandbits.platform.neoforge;

import net.minecraft.world.entity.player.Player;

@SuppressWarnings("unused")
public final class PlatformPlayerUtilImpl {

    private PlatformPlayerUtilImpl() {}

    public static boolean isFakePlayer(Player player) {
        return player.getClass().getName().contains("FakePlayer");
    }
}
