package mod.chiselsandbits.mixin;

import java.util.Base64;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.legacy.LegacyChiseledBlockFix;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.SimpleBitStorage;

public final class LegacyBlobCompatibilityCheck {

    private static final byte[] LEGACY_1_12_BLOB =
            Base64.getDecoder().decode("eJxjYGFgXHyA+f9/RoYGDgYGBiaGUTAKaAYODLQDRgEO4ADEACSRBPU=");

    private LegacyBlobCompatibilityCheck() {}

    public static void main(final String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        final VoxelBlob blob = LegacyChiseledBlockFix.decodeLegacyCompact(LEGACY_1_12_BLOB, state -> state + 10);
        require(blob.get(0, 0, 0) == 11, "stone fixture voxel was not decoded");
        require(blob.get(7, 8, 9) == 32777, "modded fixture voxel was not decoded");
        require(blob.get(15, 15, 15) == 57389, "metadata fixture voxel was not decoded");
        require(blob.get(1, 1, 1) == 10, "air fixture voxel was not remapped");
        verifyLegacyChunkSanitizing();
    }

    private static void verifyLegacyChunkSanitizing() {
        final CompoundTag chunk = new CompoundTag();
        final CompoundTag entity = new CompoundTag();
        final ListTag attributes = new ListTag();
        attributes.add(attribute("minecraft:max_health"));
        attributes.add(attribute("forge.swimSpeed"));
        entity.put("attributes", attributes);

        final CompoundTag equipment = new CompoundTag();
        equipment.put("head", item("extrabitmanipulation:chiseled_helmet"));
        equipment.put("chest", item("minecraft:stone"));
        entity.put("equipment", equipment);
        final ListTag entities = new ListTag();
        entities.add(entity);
        chunk.put("entities", entities);

        final CompoundTag section = new CompoundTag();
        section.putByte("Y", (byte) 4);
        final CompoundTag blockStates = new CompoundTag();
        final ListTag palette = new ListTag();
        palette.add(blockState("minecraft:air"));
        for (int i = 1; i < 2049; i++) {
            palette.add(blockState("missing:block_" + i));
        }
        palette.add(blockState("minecraft:stone"));
        blockStates.put("palette", palette);
        final SimpleBitStorage oldStorage = new SimpleBitStorage(12, VoxelBlob.full_size);
        oldStorage.set(0, 2049);
        blockStates.putLongArray("data", oldStorage.getRaw());
        section.put("block_states", blockStates);
        final ListTag sections = new ListTag();
        sections.add(section);
        chunk.put("sections", sections);

        LegacyChiseledBlockFix.sanitizeLegacyData(chunk);

        require(attributes.size() == 1, "invalid legacy attribute was not removed");
        require(equipment.get("head") == null, "missing legacy equipment was not removed");
        require(equipment.get("chest") != null, "installed equipment was removed");
        require(blockStates.getListOrEmpty("palette").size() == 2, "legacy block palette was not compacted");
        final SimpleBitStorage fixedStorage = new SimpleBitStorage(
                4, VoxelBlob.full_size, blockStates.getLongArray("data").orElseThrow());
        require(fixedStorage.get(0) == 1, "legacy block palette index was not remapped");
    }

    private static CompoundTag attribute(final String id) {
        final CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        return tag;
    }

    private static CompoundTag item(final String id) {
        final CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putInt("count", 1);
        return tag;
    }

    private static CompoundTag blockState(final String id) {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Name", id);
        return tag;
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
