package mod.chiselsandbits.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;

public final class PlatformPickBlock {

    private PlatformPickBlock() {}

    @ExpectPlatform
    public static void register() {}

    @ExpectPlatform
    public static void rememberHit(ServerPlayer player, BlockHitResult hit) {}
}
