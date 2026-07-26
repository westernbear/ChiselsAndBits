package mod.chiselsandbits.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mod.chiselsandbits.chiseledblock.BlockPropertyResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {

    @WrapOperation(
            method = "playStepSound",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;",
                            ordinal = 0))
    private SoundType chiselsandbits$resolveStepSound(
            final BlockState state, final Operation<SoundType> original, final @Local(argsOnly = true) BlockPos pos) {
        final AbstractHorse horse = (AbstractHorse) (Object) this;
        return BlockPropertyResolver.resolveSoundType(state, horse.level(), pos, horse);
    }
}
