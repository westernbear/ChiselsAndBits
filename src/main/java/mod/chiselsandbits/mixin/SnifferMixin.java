package mod.chiselsandbits.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mod.chiselsandbits.chiseledblock.BlockPropertyResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Sniffer.class)
public abstract class SnifferMixin {

    @WrapOperation(
            method = "emitDiggingParticles",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
    private SoundType chiselsandbits$resolveDiggingSound(
            final BlockState state, final Operation<SoundType> original, final @Local(ordinal = 0) BlockPos headBlock) {
        final Sniffer sniffer = (Sniffer) (Object) this;
        return BlockPropertyResolver.resolveSoundType(state, sniffer.level(), headBlock.below(), sniffer);
    }
}
