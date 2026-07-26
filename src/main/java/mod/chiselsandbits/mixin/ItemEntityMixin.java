package mod.chiselsandbits.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mod.chiselsandbits.chiseledblock.BlockPropertyResolver;
import mod.chiselsandbits.events.extra.EntityItemPickupEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F"))
    private float chiselsandbits$resolveGroundFriction(final Block block, final Operation<Float> original) {
        final ItemEntity entity = (ItemEntity) (Object) this;
        final BlockPos pos = entity.getBlockPosBelowThatAffectsMyMovement();
        final BlockState state = entity.level().getBlockState(pos);
        if (state.getBlock() != block) {
            return original.call(block);
        }

        return BlockPropertyResolver.resolveFriction(state, entity.level(), pos, entity);
    }

    @Inject(
            method = "playerTouch",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I"),
            cancellable = true)
    private void mod$onItemPickup(Player player, CallbackInfo ci) {
        boolean result = EntityItemPickupEvent.EVENT.invoker().handle((ItemEntity) (Object) this, player);

        if (result) {
            ci.cancel();
        }
    }
}
