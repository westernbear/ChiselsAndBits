package mod.chiselsandbits.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mod.chiselsandbits.chiseledblock.LegacyBlockEntityProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {

    @Inject(
            method = "placeInWorld",
            at =
                    @At(
                            value = "INVOKE_ASSIGN",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;mirror(Lnet/minecraft/world/level/block/Mirror;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void mod$invokeMirror(
            ServerLevelAccessor serverLevelAccessor,
            BlockPos blockPos,
            BlockPos blockPos2,
            StructurePlaceSettings structurePlaceSettings,
            RandomSource randomSource,
            int i,
            CallbackInfoReturnable<Boolean> cir,
            @Local(ordinal = 2) BlockPos pos,
            @Local(ordinal = 0) StructureTemplate.StructureBlockInfo info) {
        if (!(serverLevelAccessor.getBlockEntity(pos) instanceof LegacyBlockEntityProperties properties)) {
            return;
        }

        properties.mirror(serverLevelAccessor, pos, info.state(), structurePlaceSettings.getMirror());
    }
}
