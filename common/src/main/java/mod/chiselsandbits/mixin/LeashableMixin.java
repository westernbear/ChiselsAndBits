package mod.chiselsandbits.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mod.chiselsandbits.chiseledblock.BlockPropertyResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Leashable.class)
public interface LeashableMixin {

    @WrapOperation(
            method = "angularFriction",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F"))
    private static float chiselsandbits$resolveAngularFriction(
            final Block block, final Operation<Float> original, final @Local(argsOnly = true) Entity entity) {
        final BlockPos pos = entity.getBlockPosBelowThatAffectsMyMovement();
        final BlockState state = entity.level().getBlockState(pos);
        if (state.getBlock() != block) {
            return original.call(block);
        }

        return BlockPropertyResolver.resolveFriction(state, entity.level(), pos, entity);
    }
}
