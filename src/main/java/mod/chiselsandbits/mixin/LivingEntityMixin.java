package mod.chiselsandbits.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mod.chiselsandbits.chiseledblock.BlockPropertyResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @WrapOperation(
            method = "travelInAir",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F"))
    private float chiselsandbits$resolveTravelFriction(
            final Block block, final Operation<Float> original, final @Local(ordinal = 0) BlockPos movementPos) {
        final LivingEntity entity = (LivingEntity) (Object) this;
        final BlockState state = entity.level().getBlockState(movementPos);
        if (state.getBlock() != block) {
            return original.call(block);
        }

        return BlockPropertyResolver.resolveFriction(state, entity.level(), movementPos, entity);
    }

    @WrapOperation(
            method = "playBlockFallSound",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
    private SoundType chiselsandbits$resolveFallSound(final BlockState state, final Operation<SoundType> original) {
        final LivingEntity entity = (LivingEntity) (Object) this;
        final BlockPos pos = BlockPos.containing(entity.getX(), entity.getY() - 0.20000000298023224D, entity.getZ());
        return BlockPropertyResolver.resolveSoundType(state, entity.level(), pos, entity);
    }
}
