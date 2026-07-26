package mod.chiselsandbits.mixin;

import mod.chiselsandbits.chiseledblock.BlockChiseled;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow
    protected ServerLevel level;

    @Redirect(
            method = "destroyBlock",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerPlayer;hasCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean mod$useChiseledPrimaryStateForHarvest(
            final ServerPlayer player, final BlockState state, final BlockPos pos) {
        if (state.getBlock() instanceof BlockChiseled chiseledBlock) {
            return chiseledBlock.canHarvestBlock(state, level, pos, player);
        }

        return player.hasCorrectToolForDrops(state);
    }
}
