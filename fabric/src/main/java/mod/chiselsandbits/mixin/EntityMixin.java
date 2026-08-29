package mod.chiselsandbits.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mod.chiselsandbits.chiseledblock.BlockPropertyResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @WrapOperation(
            method = "playStepSound",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
    private SoundType chiselsandbits$resolveStepSound(
            final BlockState state, final Operation<SoundType> original, final @Local(argsOnly = true) BlockPos pos) {
        final Entity entity = (Entity) (Object) this;
        return BlockPropertyResolver.resolveSoundType(state, entity.level(), pos, entity);
    }

    @WrapOperation(
            method = {"playCombinationStepSounds", "playMuffledStepSound"},
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
    private SoundType chiselsandbits$resolveMovementSound(final BlockState state, final Operation<SoundType> original) {
        final Entity entity = (Entity) (Object) this;
        final BlockPos pos = entity.getBlockPosBelowThatAffectsMyMovement();
        return BlockPropertyResolver.resolveSoundType(state, entity.level(), pos, entity);
    }
}
