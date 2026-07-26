package mod.chiselsandbits.mixin.compat.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.BlockPropertyResolver;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.ChiselToolType;
import mod.chiselsandbits.network.packets.PacketPickBlockHit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @WrapOperation(
            method = "continueDestroyBlock",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
    private SoundType chiselsandbits$resolveHitSound(
            final BlockState state, final Operation<SoundType> original, final @Local(argsOnly = true) BlockPos pos) {
        if (minecraft.level == null || minecraft.player == null) {
            return original.call(state);
        }

        return BlockPropertyResolver.resolveSoundType(state, minecraft.level, pos, minecraft.player);
    }

    @Inject(method = "handlePickItemFromBlock", at = @At("HEAD"))
    private void chiselsandbits$sendPrecisePickHit(
            final BlockPos pos, final boolean includeData, final CallbackInfo callbackInfo) {
        if (minecraft.level == null
                || minecraft.player == null
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || !hit.getBlockPos().equals(pos)
                || !(minecraft.level.getBlockState(pos).getBlock() instanceof BlockChiseled)
                || ChiselToolType.fromItemStack(minecraft.player.getMainHandItem()) == null) {
            return;
        }

        ChiselsAndBits.getNetworkChannel().sendToServer(new PacketPickBlockHit(hit));
    }
}
