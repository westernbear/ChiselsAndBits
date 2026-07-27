package mod.chiselsandbits.mixin;

import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import mod.chiselsandbits.legacy.LegacyChiseledBlockFix;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class LegacyChiseledBlockFixMixins {

    private LegacyChiseledBlockFixMixins() {}

    @Mixin(LevelStorageSource.class)
    public static class LevelDataMixin {

        @Inject(method = "readLevelDataTagRaw(Ljava/nio/file/Path;)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
        private static void chiselsandbits$captureForgeRegistry(
                final java.nio.file.Path path, final CallbackInfoReturnable<CompoundTag> callback) {
            LegacyChiseledBlockFix.captureForgeRegistry(path, callback.getReturnValue());
        }
    }

    @Mixin(LevelStorageSource.LevelStorageAccess.class)
    public static class LevelDataSaveMixin {

        @Shadow
        @Final
        private LevelStorageSource.LevelDirectory levelDirectory;

        @Unique
        private CompoundTag chiselsandbits$legacyFml;

        @Inject(method = "getUnfixedDataTag(Z)Lcom/mojang/serialization/Dynamic;", at = @At("RETURN"))
        private void chiselsandbits$activateForgeRegistry(
                final boolean oldData, final CallbackInfoReturnable<Dynamic<?>> callback) {
            final java.nio.file.Path path = oldData ? levelDirectory.oldDataFile() : levelDirectory.dataFile();
            chiselsandbits$legacyFml = LegacyChiseledBlockFix.activateForgeRegistry(path);
        }

        @Inject(method = "saveLevelData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("HEAD"))
        private void chiselsandbits$preserveForgeRegistry(final CompoundTag root, final CallbackInfo callback) {
            LegacyChiseledBlockFix.preserveForgeRegistry(root, chiselsandbits$legacyFml);
        }
    }

    @Mixin(targets = "net.minecraft.util.datafix.fixes.ChunkPalettedStorageFix$UpgradeChunk")
    public abstract static class UpgradeChunkMixin {

        @Shadow
        @Final
        private Int2ObjectMap<Dynamic<?>> blockEntities;

        @Invoker("setBlock")
        protected abstract void chiselsandbits$setBlock(int position, Dynamic<?> state);

        @Inject(method = "<init>(Lcom/mojang/serialization/Dynamic;)V", at = @At("TAIL"))
        private void chiselsandbits$upgradeLegacyBlocks(final Dynamic<?> level, final CallbackInfo callback) {
            for (final Int2ObjectMap.Entry<Dynamic<?>> entry : blockEntities.int2ObjectEntrySet()) {
                final Dynamic<?> fixed = LegacyChiseledBlockFix.convertBlockEntity(entry.getValue());
                if (fixed == null) {
                    continue;
                }

                entry.setValue(fixed);
                final Dynamic<?> blockState =
                        fixed.emptyMap().set("Name", fixed.createString(LegacyChiseledBlockFix.CURRENT_BLOCK));
                chiselsandbits$setBlock(entry.getIntKey(), blockState);
            }
        }
    }
}
