package mod.chiselsandbits.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.BlockPropertyResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @WrapOperation(
            method = "place",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;getSoundType()Lnet/minecraft/world/level/block/SoundType;"))
    private SoundType chiselsandbits$resolvePlacedBlockSoundType(
            final BlockState state,
            final Operation<SoundType> original,
            final @Local BlockPos pos,
            final @Local Level level,
            final @Local Player player) {
        return BlockPropertyResolver.resolveSoundType(state, level, pos, player);
    }

    @WrapOperation(
            method = "place",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/BlockItem;getPlaceSound(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/sounds/SoundEvent;"))
    private SoundEvent chiselsandbits$resolvePlacedBlockSoundEvent(
            final BlockItem item,
            final BlockState state,
            final Operation<SoundEvent> original,
            final @Local BlockPos pos,
            final @Local Level level,
            final @Local Player player) {
        if (state.getBlock() instanceof BlockChiseled) {
            return BlockPropertyResolver.resolveSoundType(state, level, pos, player)
                    .getPlaceSound();
        }

        return original.call(item, state);
    }
}
