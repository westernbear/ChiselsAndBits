package mod.chiselsandbits.legacy;

import com.mojang.serialization.Dynamic;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntUnaryOperator;
import java.util.zip.InflaterInputStream;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.serialization.StringStates;
import mod.chiselsandbits.helpers.ModUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LegacyChiseledBlockFix {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyChiseledBlockFix.class);
    private static final String CURRENT_BLOCK_ENTITY = "chiselsandbits:chiseled";
    public static final String CURRENT_BLOCK = "chiselsandbits:chiseled_block";
    private static final int MAX_INFLATED_BLOB_BYTES = 64 * 1024;
    private static final Snapshot EMPTY = new Snapshot(Map.of(), null);
    private static final Set<String> MISSING_BLOCKS = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<PendingSnapshot> PENDING_SNAPSHOT = new ThreadLocal<>();
    private static volatile Snapshot snapshot = EMPTY;

    private LegacyChiseledBlockFix() {}

    public static void captureForgeRegistry(final CompoundTag root) {
        snapshot = readForgeRegistry(root);
    }

    public static void captureForgeRegistry(final Path path, final CompoundTag root) {
        PENDING_SNAPSHOT.set(new PendingSnapshot(path.toAbsolutePath().normalize(), readForgeRegistry(root)));
    }

    public static CompoundTag activateForgeRegistry(final Path path) {
        final PendingSnapshot pending = PENDING_SNAPSHOT.get();
        PENDING_SNAPSHOT.remove();
        snapshot =
                pending != null && pending.path().equals(path.toAbsolutePath().normalize())
                        ? pending.snapshot()
                        : EMPTY;
        final CompoundTag fml = snapshot.fml();
        return fml == null ? null : fml.copy();
    }

    private static Snapshot readForgeRegistry(final CompoundTag root) {
        final CompoundTag fml = root.getCompound("FML").orElse(null);
        if (fml == null) {
            return EMPTY;
        }

        final ListTag ids = fml.getCompoundOrEmpty("Registries")
                .getCompoundOrEmpty("minecraft:blocks")
                .getListOrEmpty("ids");
        final Map<Integer, String> blocks = new HashMap<>();
        boolean hasChiselsAndBits = false;

        for (final Tag value : ids) {
            if (!(value instanceof CompoundTag entry)) {
                continue;
            }

            final int id = entry.getIntOr("V", -1);
            final String name = entry.getStringOr("K", "");
            if (id < 0 || id > 4095 || name.isEmpty()) {
                continue;
            }

            blocks.put(id, name);
            hasChiselsAndBits |= name.toLowerCase(Locale.ROOT).startsWith("chiselsandbits:");
        }

        return hasChiselsAndBits ? new Snapshot(Map.copyOf(blocks), fml.copy()) : EMPTY;
    }

    public static void preserveForgeRegistry(final CompoundTag root) {
        preserveForgeRegistry(root, snapshot.fml());
    }

    public static void preserveForgeRegistry(final CompoundTag root, final CompoundTag fml) {
        if (fml != null) {
            root.put("FML", fml.copy());
        }
    }

    public static Dynamic<?> convertBlockEntity(final Dynamic<?> blockEntity) {
        final String id = blockEntity.get("id").asString("");
        if (!isLegacyBlockEntity(id)) {
            return null;
        }
        if (snapshot.blocks().isEmpty()) {
            LOGGER.warn("No legacy Forge block registry is active; leaving Chisels & Bits block entity unchanged");
            return null;
        }

        final Tag value = blockEntity.convert(NbtOps.INSTANCE).getValue();
        if (!(value instanceof CompoundTag source)) {
            return null;
        }

        try {
            final CompoundTag fixed = source.copy();
            final VoxelBlob blob = readAndRemapBlob(fixed);
            int primaryState = resolveLegacyState(fixed.getIntOr(NBTBlobConverter.NBT_PRIMARY_STATE, 0));
            if (primaryState == 0) {
                primaryState = mostCommonState(blob);
            }
            if (primaryState == 0) {
                primaryState = ModUtil.getStateId(Blocks.COBBLESTONE.defaultBlockState());
            }

            fixed.putString("id", CURRENT_BLOCK_ENTITY);
            fixed.putByteArray(
                    NBTBlobConverter.NBT_VERSIONED_VOXEL, blob.blobToBytes(VoxelBlob.VERSION_COMPACT_PALLETED));
            fixed.remove(NBTBlobConverter.NBT_LEGACY_VOXEL);

            final NBTBlobConverter converter = new NBTBlobConverter();
            converter.setBlob(blob);
            fixed.putInt(NBTBlobConverter.NBT_SIDE_FLAGS, converter.getSideState());
            fixed.putInt(NBTBlobConverter.NBT_LIGHTVALUE, converter.getLightValue());
            fixed.putBoolean(NBTBlobConverter.NBT_NORMALCUBE_FLAG, converter.isNormalCube());
            fixed.putString(NBTBlobConverter.NBT_PRIMARY_STATE, StringStates.getNameFromStateID(primaryState));
            return convertToOriginalOps(fixed, blockEntity);
        } catch (final Exception error) {
            LOGGER.warn(
                    "Could not upgrade legacy Chisels & Bits block entity at {}, {}, {}; leaving it unchanged",
                    source.getIntOr("x", 0),
                    source.getIntOr("y", 0),
                    source.getIntOr("z", 0),
                    error);
            return null;
        }
    }

    public static <T> Dynamic<T> convertItemStack(final Dynamic<T> itemStack) {
        final String id = itemStack.get("id").asString("").toLowerCase(Locale.ROOT);
        if (!id.startsWith("chiselsandbits:chiseled_")) {
            return null;
        }

        final Dynamic<T> tag = itemStack.get("tag").orElseEmptyMap();
        Dynamic<T> blockEntity = tag.get(ModUtil.NBT_BLOCKENTITYTAG).orElseEmptyMap();
        if (blockEntity.get(NBTBlobConverter.NBT_VERSIONED_VOXEL).result().isEmpty()
                && blockEntity.get(NBTBlobConverter.NBT_LEGACY_VOXEL).result().isEmpty()) {
            return null;
        }
        if (id.equals(CURRENT_BLOCK)) {
            return blockEntity.get("id").asString("").equals(CURRENT_BLOCK_ENTITY) ? itemStack : null;
        }
        if (blockEntity.get("id").asString("").isEmpty()) {
            blockEntity = blockEntity.set("id", blockEntity.createString("mod.chiselsandbits.tileentitychiseled"));
        }

        final Dynamic<?> fixed = convertBlockEntity(blockEntity);
        return fixed == null
                ? null
                : itemStack
                        .set("id", itemStack.createString(CURRENT_BLOCK))
                        .set("tag", tag.set(ModUtil.NBT_BLOCKENTITYTAG, fixed));
    }

    private static boolean isLegacyBlockEntity(final String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        }
        return normalized.equals("mod.chiselsandbits.tileentitychiseled")
                || normalized.equals("mod.chiselsandbits.tileentitychiseled.tesr");
    }

    private static VoxelBlob readAndRemapBlob(final CompoundTag source) throws IOException {
        final byte[] compact =
                source.getByteArray(NBTBlobConverter.NBT_VERSIONED_VOXEL).orElseGet(() -> new byte[0]);
        if (compact.length > 0) {
            return decodeLegacyCompact(compact, LegacyChiseledBlockFix::resolveLegacyState);
        }

        final byte[] legacy =
                source.getByteArray(NBTBlobConverter.NBT_LEGACY_VOXEL).orElseGet(() -> new byte[0]);
        if (legacy.length == 0) {
            throw new IOException("Legacy chiseled block has no voxel payload");
        }

        final VoxelBlob oldBlob = new VoxelBlob();
        oldBlob.fromLegacyByteArray(legacy);
        return remap(oldBlob, LegacyChiseledBlockFix::resolveLegacyState);
    }

    public static VoxelBlob decodeLegacyCompact(final byte[] compressed, final IntUnaryOperator stateMapper)
            throws IOException {
        final byte[] inflated;
        try (var input = new InflaterInputStream(new ByteArrayInputStream(compressed))) {
            inflated = input.readNBytes(MAX_INFLATED_BLOB_BYTES + 1);
        }
        if (inflated.length > MAX_INFLATED_BLOB_BYTES) {
            throw new IOException("Legacy chiseled blob is too large");
        }

        final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(inflated));
        try {
            if (buffer.readVarInt() != 0) {
                throw new IOException("Unsupported legacy chiseled blob version");
            }

            final int paletteSize = buffer.readVarInt();
            if (paletteSize < 1 || paletteSize > VoxelBlob.full_size) {
                throw new IOException("Invalid legacy chiseled palette size: " + paletteSize);
            }

            final int[] palette = new int[paletteSize];
            for (int i = 0; i < palette.length; i++) {
                final int legacyState = buffer.readVarInt();
                if (legacyState < 0 || legacyState > 0xffff) {
                    throw new IOException("Invalid legacy block state: " + legacyState);
                }
                palette[i] = stateMapper.applyAsInt(legacyState);
            }

            final int bitsPerValue = Math.max(Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1), 1);
            final int byteOffset = buffer.readVarInt();
            final int bytesOfInterest = buffer.readVarInt();
            final int payloadOffset = buffer.readerIndex();
            final int maxPayloadBytes = (VoxelBlob.full_size * bitsPerValue + 31) / 32 * Integer.BYTES;
            if (byteOffset < 0
                    || (byteOffset & 3) != 0
                    || bytesOfInterest < 0
                    || (bytesOfInterest & 3) != 0
                    || bytesOfInterest > inflated.length - payloadOffset
                    || byteOffset > maxPayloadBytes
                    || bytesOfInterest > maxPayloadBytes - byteOffset) {
                throw new IOException("Invalid legacy chiseled bit stream");
            }

            final VoxelBlob result = new VoxelBlob();
            int logicalBit = 0;
            for (int index = 0; index < VoxelBlob.full_size; index++) {
                int paletteIndex = 0;
                for (int bit = bitsPerValue - 1; bit >= 0; bit--) {
                    if (readLegacyBit(inflated, payloadOffset, bytesOfInterest, byteOffset, logicalBit++)) {
                        paletteIndex |= 1 << bit;
                    }
                }
                if (paletteIndex >= palette.length) {
                    throw new IOException("Invalid legacy chiseled palette index: " + paletteIndex);
                }
                result.set(index & 15, index >> 4 & 15, index >> 8 & 15, palette[paletteIndex]);
            }
            return result;
        } catch (final RuntimeException error) {
            throw new IOException("Malformed legacy chiseled blob", error);
        } finally {
            buffer.release();
        }
    }

    private static boolean readLegacyBit(
            final byte[] data,
            final int payloadOffset,
            final int payloadLength,
            final int byteOffset,
            final int logicalBit) {
        final int storedBit = logicalBit - byteOffset * Byte.SIZE;
        if (storedBit < 0 || storedBit >= payloadLength * Byte.SIZE) {
            return false;
        }

        final int wordOffset = payloadOffset + (storedBit >>> 5) * Integer.BYTES;
        final int word = ByteBuffer.wrap(data, wordOffset, Integer.BYTES).getInt();
        return (word & 1 << (storedBit & 31)) != 0;
    }

    private static VoxelBlob remap(final VoxelBlob source, final IntUnaryOperator stateMapper) {
        final VoxelBlob result = new VoxelBlob();
        for (int z = 0; z < VoxelBlob.dim; z++) {
            for (int y = 0; y < VoxelBlob.dim; y++) {
                for (int x = 0; x < VoxelBlob.dim; x++) {
                    result.set(x, y, z, stateMapper.applyAsInt(source.get(x, y, z)));
                }
            }
        }
        return result;
    }

    private static int resolveLegacyState(final int legacyState) {
        if (legacyState == 0) {
            return 0;
        }

        final int blockId = legacyState & 4095;
        final int metadata = legacyState >>> 12 & 15;
        final String oldName = snapshot.blocks().get(blockId);
        if (oldName == null && blockId > 255) {
            throw new IllegalArgumentException("No legacy Forge registry name is available for block id " + blockId);
        }
        if (oldName == null || oldName.toLowerCase(Locale.ROOT).startsWith("minecraft:")) {
            final Tag stateTag = BlockStateData.getTag(blockId << 4 | metadata)
                    .convert(NbtOps.INSTANCE)
                    .getValue();
            return stateTag instanceof CompoundTag compound
                    ? ModUtil.getStateId(NbtUtils.readBlockState(BuiltInRegistries.BLOCK, compound))
                    : 0;
        }

        final Identifier currentId = Identifier.tryParse(oldName.toLowerCase(Locale.ROOT));
        final Block currentBlock = currentId == null
                ? null
                : BuiltInRegistries.BLOCK.getOptional(currentId).orElse(null);
        if (currentBlock != null) {
            return ModUtil.getStateId(currentBlock.defaultBlockState());
        }

        if (MISSING_BLOCKS.add(oldName)) {
            LOGGER.warn("Legacy block {} is not installed; affected bits will become air", oldName);
        }
        return 0;
    }

    private static int mostCommonState(final VoxelBlob blob) {
        int state = 0;
        int count = 0;
        for (final Map.Entry<Integer, Integer> entry : blob.getBlockSums().entrySet()) {
            if (entry.getKey() != 0 && entry.getValue() > count) {
                state = entry.getKey();
                count = entry.getValue();
            }
        }
        return state;
    }

    private static <T> Dynamic<T> convertToOriginalOps(final CompoundTag tag, final Dynamic<T> original) {
        return new Dynamic<>(NbtOps.INSTANCE, tag).convert(original.getOps());
    }

    private record Snapshot(Map<Integer, String> blocks, CompoundTag fml) {}

    private record PendingSnapshot(Path path, Snapshot snapshot) {}
}
