package mod.chiselsandbits.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mod.chiselsandbits.chiseledblock.BlockPropertyResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F"))
    private float chiselsandbits$resolveGroundFriction(final Block block, final Operation<Float> original) {
        final ExperienceOrb orb = (ExperienceOrb) (Object) this;
        final BlockPos pos = orb.getBlockPosBelowThatAffectsMyMovement();
        final BlockState state = orb.level().getBlockState(pos);
        if (state.getBlock() != block) {
            return original.call(block);
        }

        return BlockPropertyResolver.resolveFriction(state, orb.level(), pos, orb);
    }
}
