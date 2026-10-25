package mod.chiselsandbits.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mod.chiselsandbits.chiseledblock.BlockPropertyResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin {

    @WrapOperation(
            method = "getGroundFriction",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F"))
    private float chiselsandbits$resolveGroundFriction(
            final Block block,
            final Operation<Float> original,
            final @Local BlockState state,
            final @Local BlockPos.MutableBlockPos pos) {
        final AbstractBoat boat = (AbstractBoat) (Object) this;
        if (state.getBlock() != block) {
            return original.call(block);
        }

        return BlockPropertyResolver.resolveFriction(state, boat.level(), pos, boat);
    }
}
