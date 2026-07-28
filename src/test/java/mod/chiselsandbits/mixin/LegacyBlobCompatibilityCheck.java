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
        equipment.put("feet", item("missing:gone"));
        entity.put("equipment", equipment);
        final ListTag armorItems = new ListTag();
        armorItems.add(item("extrabitmanipulation:chiseled_leggings"));
        entity.put("ArmorItems", armorItems);
        final ListTag entities = new ListTag();
        entities.add(entity);
        chunk.put("entities", entities);

        final String[][] replacements = {
            {"extrabitmanipulation:chiseled_helmet", "minecraft:diamond_helmet"},
            {"extrabitmanipulation:chiseled_chestplate", "minecraft:diamond_chestplate"},
            {"extrabitmanipulation:chiseled_leggings", "minecraft:diamond_leggings"},
            {"extrabitmanipulation:chiseled_boots", "minecraft:diamond_boots"},
            {"extrabitmanipulation:chiseled_helmet_iron", "minecraft:iron_helmet"},
            {"extrabitmanipulation:chiseled_chestplate_iron", "minecraft:iron_chestplate"},
            {"extrabitmanipulation:chiseled_leggings_iron", "minecraft:iron_leggings"},
            {"extrabitmanipulation:chiseled_boots_iron", "minecraft:iron_boots"}
        };
        final ListTag items = new ListTag();
        for (int i = 0; i < replacements.length; i++) {
            final CompoundTag stack = item(replacements[i][0]);
            stack.putByte("Slot", (byte) i);
            items.add(stack);
        }
        final CompoundTag retained = item("minecraft:stone");
        retained.putByte("Slot", (byte) replacements.length);
        final CompoundTag components = new CompoundTag();
        final CompoundTag customData = new CompoundTag();
        customData.putString("marker", "kept");
        customData.putString("id", "extrabitmanipulation:chiseled_helmet");
        customData.putInt("count", 1);
        components.put("minecraft:custom_data", customData);
        retained.put("components", components);
        items.add(retained);
        final CompoundTag shulker = new CompoundTag();
        shulker.putString("id", "minecraft:shulker_box");
        shulker.put("Items", items);
        final ListTag blockEntities = new ListTag();
        blockEntities.add(shulker);
        chunk.put("block_entities", blockEntities);

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
        require(
                equipment.getCompoundOrEmpty("head").getStringOr("id", "").equals("minecraft:diamond_helmet"),
                "legacy armor equipment was not recovered");
        require(equipment.get("chest") != null, "installed equipment was removed");
        require(equipment.get("feet") == null, "unknown legacy equipment was not removed");
        require(
                armorItems.getCompoundOrEmpty(0).getStringOr("id", "").equals("minecraft:diamond_leggings"),
                "legacy armor stand item was not recovered");
        require(items.size() == replacements.length + 1, "legacy container items were removed");
        for (int i = 0; i < replacements.length; i++) {
            require(
                    items.getCompoundOrEmpty(i).getStringOr("id", "").equals(replacements[i][1]),
                    "legacy container armor was not recovered at index " + i);
        }
        require(
                items.getCompoundOrEmpty(replacements.length)
                        .getCompoundOrEmpty("components")
                        .getCompoundOrEmpty("minecraft:custom_data")
                        .getStringOr("marker", "")
                        .equals("kept"),
                "installed container item data was changed");
        require(
                items.getCompoundOrEmpty(replacements.length)
                        .getCompoundOrEmpty("components")
                        .getCompoundOrEmpty("minecraft:custom_data")
                        .getStringOr("id", "")
                        .equals("extrabitmanipulation:chiseled_helmet"),
                "non-item custom data id was changed");
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
