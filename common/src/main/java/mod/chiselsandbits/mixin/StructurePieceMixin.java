package mod.chiselsandbits.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mod.chiselsandbits.chiseledblock.LegacyBlockEntityProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StructurePiece.class)
public abstract class StructurePieceMixin {

    @Shadow
    private Mirror mirror;

    @Inject(
            method = "placeBlock",
            at =
                    @At(
                            value = "INVOKE_ASSIGN",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;mirror(Lnet/minecraft/world/level/block/Mirror;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void mod$invokeMirror(
            WorldGenLevel worldGenLevel,
            BlockState blockState,
            int i,
            int j,
            int k,
            BoundingBox boundingBox,
            CallbackInfo ci,
            @Local(ordinal = 0) BlockPos pos) {
        if (!(worldGenLevel.getBlockEntity(pos) instanceof LegacyBlockEntityProperties properties)) {
            return;
        }
        properties.mirror(worldGenLevel, pos, blockState, mirror);
    }
}
