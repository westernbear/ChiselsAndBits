package mod.chiselsandbits.mixin;

import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Map;
import mod.chiselsandbits.legacy.LegacyChiseledBlockFix;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.schemas.V99;
import net.minecraft.util.filefix.FileFixerUpper;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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

    @Mixin(FileFixerUpper.class)
    public static class FileFixerUpperMixin {

        @ModifyArg(
                method = "writeUpdatedLevelData(Ljava/nio/file/Path;I)V",
                at =
                        @At(
                                value = "INVOKE",
                                target =
                                        "Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/nio/file/Path;)V"),
                index = 0)
        private CompoundTag chiselsandbits$preserveForgeRegistry(final CompoundTag root) {
            LegacyChiseledBlockFix.preserveForgeRegistry(root);
            return root;
        }
    }

    @Mixin(SerializableChunkData.class)
    public static class SerializableChunkDataMixin {

        @Inject(method = "parse", at = @At("HEAD"))
        private static void chiselsandbits$sanitizeLegacyData(
                final LevelHeightAccessor levelHeight,
                final PalettedContainerFactory containerFactory,
                final CompoundTag chunkData,
                final CallbackInfoReturnable<SerializableChunkData> callback) {
            LegacyChiseledBlockFix.sanitizeLegacyData(chunkData);
        }
    }

    @Mixin(SimpleRegionStorage.class)
    public static class SimpleRegionStorageMixin {

        @Inject(
                method =
                        "upgradeChunkTag(Lnet/minecraft/nbt/CompoundTag;ILnet/minecraft/nbt/CompoundTag;I)Lnet/minecraft/nbt/CompoundTag;",
                at = @At("RETURN"))
        private void chiselsandbits$sanitizeLegacyData(
                final CompoundTag chunkTag,
                final int defaultVersion,
                final CompoundTag contextTag,
                final int targetVersion,
                final CallbackInfoReturnable<CompoundTag> callback) {
            LegacyChiseledBlockFix.sanitizeLegacyData(callback.getReturnValue());
        }
    }

    @Mixin(V99.class)
    public static class ItemStackNamesMixin {

        @Inject(
                method = "addNames(Lcom/mojang/serialization/Dynamic;Ljava/util/Map;Ljava/util/Map;)Ljava/lang/Object;",
                at = @At("HEAD"),
                cancellable = true)
        private static void chiselsandbits$upgradeLegacyItem(
                final Dynamic<?> itemStack,
                final Map<String, String> blockEntityNames,
                final Map<String, String> entityNames,
                final CallbackInfoReturnable<Object> callback) {
            final Dynamic<?> fixed = LegacyChiseledBlockFix.convertItemStack(itemStack);
            if (fixed != null) {
                callback.setReturnValue(fixed.getValue());
            }
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
            final Dynamic<?> blockState =
                    level.emptyMap().set("Name", level.createString(LegacyChiseledBlockFix.CURRENT_BLOCK));
            for (final Int2ObjectMap.Entry<Dynamic<?>> entry : blockEntities.int2ObjectEntrySet()) {
                final Dynamic<?> fixed = LegacyChiseledBlockFix.convertBlockEntity(entry.getValue());
                if (fixed == null) {
                    continue;
                }

                entry.setValue(fixed);
                chiselsandbits$setBlock(entry.getIntKey(), blockState);
            }
        }
    }
}
