package mod.chiselsandbits.platform.fabric;

import mod.chiselsandbits.events.PickBlockHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("unused")
public final class PlatformPickBlockImpl {

    private PlatformPickBlockImpl() {}

    public static void register() {
        PickBlockHandler.register();
    }

    public static void rememberHit(ServerPlayer player, BlockHitResult hit) {
        PickBlockHandler.rememberHit(player, hit);
    }
}
