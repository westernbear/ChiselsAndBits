package mod.chiselsandbits.gametest;

import com.mojang.serialization.Dynamic;
import java.nio.file.Path;
import java.util.Base64;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.legacy.LegacyChiseledBlockFix;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModDataComponents;
import mod.chiselsandbits.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LegacyCompatibilityGameTests {

    private static final int DATA_VERSION_1_12_2 = 1343;
    private static final byte[] LEGACY_1_12_BLOB =
            Base64.getDecoder().decode("eJxjYGFgXHyA+f9/RoYGDgYGBiaGUTAKaAYODLQDRgEO4ADEACSRBPU=");

    @GameTest(maxTicks = 200)
    public void upgradesLegacyWorldData(final GameTestHelper helper) {
        final CompoundTag levelData = legacyLevelData();
        final Path levelPath = Path.of("legacy-level.dat").toAbsolutePath();
        LegacyChiseledBlockFix.captureForgeRegistry(levelPath, levelData);
        final CompoundTag legacyFml = LegacyChiseledBlockFix.activateForgeRegistry(levelPath);
        final CompoundTag savedLevelData = new CompoundTag();
        LegacyChiseledBlockFix.preserveForgeRegistry(savedLevelData, legacyFml);
        helper.assertValueEqual(
                savedLevelData.getCompoundOrEmpty("FML"),
                levelData.getCompoundOrEmpty("FML"),
                "legacy Forge registry for lazy chunks");

        final CompoundTag fixed = DataFixTypes.CHUNK.updateToCurrentVersion(
                DataFixers.getDataFixer(), legacyChunk(), DATA_VERSION_1_12_2);
        helper.assertTrue(
                fixed.toString().contains("chiselsandbits:chiseled_block"), "legacy container block was not upgraded");
        final CompoundTag fixedBody = fixed.getCompound("Level").orElse(fixed);
        final long chiseledPaletteEntries = fixedBody
                .getListOrEmpty("sections")
                .compoundStream()
                .flatMap(section -> section.getCompoundOrEmpty("block_states")
                        .getListOrEmpty("palette")
                        .compoundStream())
                .filter(state -> state.getStringOr("Name", "").equals(LegacyChiseledBlockFix.CURRENT_BLOCK))
                .count();
        helper.assertValueEqual(chiseledPaletteEntries, 1L, "legacy container palette entries");

        final CompoundTag fixedBlockEntity = findChiseledBlockEntity(fixed);
        helper.assertValueEqual(fixedBlockEntity.getStringOr("id", ""), "chiselsandbits:chiseled", "block entity id");
        helper.assertFalse(fixedBlockEntity.contains("v"), "legacy voxel field must be removed");

        final BlockPos position = new BlockPos(1, 64, 1);
        final TileEntityBlockChiseled loaded = load(position, fixedBlockEntity, helper);
        assertVoxelData(loaded, helper);

        final CompoundTag saved = loaded.saveWithFullMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(
                saved.get(NBTBlobConverter.NBT_PRIMARY_STATE) instanceof StringTag, "primary state was not stable");
        final TileEntityBlockChiseled reloaded = load(position, saved, helper);
        assertVoxelData(reloaded, helper);

        final ItemStack item = new ItemStack(ModItems.ITEM_CHISELED_BLOCK.get());
        helper.assertTrue(new NBTBlobConverter(false, reloaded).writeToStack(item, true), "item data was not written");
        final NBTBlobConverter itemData = new NBTBlobConverter();
        helper.assertTrue(
                itemData.readFromStack(item, VoxelBlob.VERSION_COMPACT_PALLETED), "item data was not reloaded");
        try {
            helper.assertValueEqual(
                    itemData.getVoxelRef(VoxelBlob.VERSION_COMPACT_PALLETED, 0).getVoxelBlob(),
                    reloaded.getBlob(),
                    "chiseled item voxel data");
        } catch (final Exception error) {
            throw new AssertionError(error);
        }

        final Dynamic<?> convertedItem = LegacyChiseledBlockFix.convertItemStack(new Dynamic<>(
                NbtOps.INSTANCE,
                legacyPlayer()
                        .getListOrEmpty("Inventory")
                        .compoundStream()
                        .findFirst()
                        .orElseThrow()));
        helper.assertTrue(
                convertedItem != null && LegacyChiseledBlockFix.convertItemStack(convertedItem) != null,
                "legacy item conversion must be idempotent");

        final CompoundTag fixedPlayer = DataFixTypes.PLAYER.updateToCurrentVersion(
                DataFixers.getDataFixer(), legacyPlayer(), DATA_VERSION_1_12_2);
        final CompoundTag fixedLegacyItem = fixedPlayer
                .getListOrEmpty("Inventory")
                .compoundStream()
                .findFirst()
                .orElseThrow();
        helper.assertValueEqual(
                fixedLegacyItem.getStringOr("id", ""), LegacyChiseledBlockFix.CURRENT_BLOCK, "legacy item id");
        helper.assertValueEqual(
                fixedLegacyItem
                        .getCompoundOrEmpty("components")
                        .getCompoundOrEmpty("minecraft:block_entity_data")
                        .getStringOr("id", ""),
                "chiselsandbits:chiseled",
                "legacy item block entity id");

        final CompoundTag fixedArmorItem = fixedPlayer
                .getListOrEmpty("Inventory")
                .compoundStream()
                .filter(stack -> stack.getStringOr("id", "").equals("minecraft:diamond_chestplate"))
                .findFirst()
                .orElseThrow();
        helper.assertValueEqual(
                fixedArmorItem
                        .getCompoundOrEmpty("components")
                        .getCompoundOrEmpty("minecraft:custom_data")
                        .getCompoundOrEmpty("data")
                        .getStringOr("migration_marker", ""),
                "kept",
                "legacy armor custom data");
        final ItemStack recoveredArmor = ItemStack.CODEC
                .parse(RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess()), fixedArmorItem)
                .getOrThrow(AssertionError::new);
        helper.assertTrue(recoveredArmor.is(Items.DIAMOND_CHESTPLATE), "legacy armor item did not decode");

        final ItemStack legacyItem = ItemStack.CODEC
                .parse(RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess()), fixedLegacyItem)
                .getOrThrow(AssertionError::new);
        final NBTBlobConverter legacyItemData = new NBTBlobConverter();
        helper.assertTrue(
                legacyItemData.readFromStack(legacyItem, VoxelBlob.VERSION_COMPACT_PALLETED),
                "legacy item data was not reloaded");
        helper.assertValueEqual(
                legacyItemData.getBlob().get(0, 0, 0),
                ModUtil.getStateId(Blocks.STONE.defaultBlockState()),
                "legacy item stone voxel");
        helper.assertValueEqual(
                legacyItemData.getBlob().get(7, 8, 9),
                ModUtil.getStateId(Blocks.COBBLESTONE.defaultBlockState()),
                "legacy item bodypart template voxel");
        helper.assertValueEqual(
                legacyItemData.getBlob().get(15, 15, 15),
                ModUtil.getStateId(Blocks.WOOL.red().defaultBlockState()),
                "legacy item metadata voxel");
        helper.assertTrue(legacyItem.get(ModDataComponents.CHISELED_DATA) != null, "legacy item component");
        helper.assertTrue(legacyItem.get(DataComponents.BLOCK_ENTITY_DATA) == null, "legacy item data cleanup");

        LegacyChiseledBlockFix.captureForgeRegistry(new CompoundTag());
        final CompoundTag unmapped = new CompoundTag();
        unmapped.putString("id", "minecraft:mod.chiselsandbits.tileentitychiseled");
        helper.assertTrue(
                LegacyChiseledBlockFix.convertBlockEntity(new Dynamic<>(NbtOps.INSTANCE, unmapped)) == null,
                "legacy data without its Forge registry must remain unchanged");

        helper.succeed();
    }

    private static void assertVoxelData(final TileEntityBlockChiseled blockEntity, final GameTestHelper helper) {
        final VoxelBlob blob = blockEntity.getBlob();
        helper.assertValueEqual(blob.get(0, 0, 0), ModUtil.getStateId(Blocks.STONE.defaultBlockState()), "stone voxel");
        helper.assertValueEqual(
                blob.get(7, 8, 9),
                ModUtil.getStateId(Blocks.COBBLESTONE.defaultBlockState()),
                "bodypart template voxel fallback");
        helper.assertValueEqual(
                blob.get(15, 15, 15), ModUtil.getStateId(Blocks.WOOL.red().defaultBlockState()), "metadata voxel");
        helper.assertValueEqual(blob.filled(), 3, "filled voxel count");
        helper.assertValueEqual(
                blockEntity.getPrimaryBlockStateId(),
                ModUtil.getStateId(Blocks.WOOL.red().defaultBlockState()),
                "primary state");
        helper.assertValueEqual(blockEntity.getLightValue(), 0, "light value");
    }

    private static TileEntityBlockChiseled load(
            final BlockPos position, final CompoundTag tag, final GameTestHelper helper) {
        final BlockEntity loaded = BlockEntity.loadStatic(
                position,
                ModBlocks.getChiseledDefaultState(),
                tag,
                helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof TileEntityBlockChiseled, "converted block entity did not load");
        final TileEntityBlockChiseled chiseled = (TileEntityBlockChiseled) loaded;
        chiseled.setLevel(helper.getLevel());
        return chiseled;
    }

    private static CompoundTag findChiseledBlockEntity(final CompoundTag chunk) {
        final CompoundTag body = chunk.getCompound("Level").orElse(chunk);
        for (final String key : new String[] {"block_entities", "TileEntities"}) {
            for (final CompoundTag blockEntity :
                    body.getListOrEmpty(key).compoundStream().toList()) {
                if (blockEntity.getStringOr("id", "").equals("chiselsandbits:chiseled")) {
                    return blockEntity;
                }
            }
        }
        throw new AssertionError("converted block entity is missing");
    }

    private static CompoundTag legacyChunk() {
        final CompoundTag blockEntity = new CompoundTag();
        blockEntity.putString("id", "minecraft:mod.chiselsandbits.tileentitychiseled");
        blockEntity.putInt("x", 1);
        blockEntity.putInt("y", 64);
        blockEntity.putInt("z", 1);
        blockEntity.putInt("b", 14 << 12 | 35);
        blockEntity.putByteArray("X", LEGACY_1_12_BLOB);
        blockEntity.putInt("s", 0);
        blockEntity.putInt("lv", 15);
        blockEntity.putBoolean("nc", true);

        final ListTag blockEntities = new ListTag();
        blockEntities.add(blockEntity);
        final CompoundTag secondBlockEntity = blockEntity.copy();
        secondBlockEntity.putInt("x", 2);
        blockEntities.add(secondBlockEntity);

        final CompoundTag section = new CompoundTag();
        section.putByte("Y", (byte) 4);
        section.putByteArray("Blocks", new byte[4096]);
        section.putByteArray("Data", new byte[2048]);
        section.putByteArray("BlockLight", new byte[2048]);
        section.putByteArray("SkyLight", new byte[2048]);
        final ListTag sections = new ListTag();
        sections.add(section);

        final CompoundTag level = new CompoundTag();
        level.putInt("xPos", 0);
        level.putInt("zPos", 0);
        level.putLong("LastUpdate", 0);
        level.putLong("InhabitedTime", 0);
        level.putBoolean("TerrainPopulated", true);
        level.putBoolean("LightPopulated", true);
        level.put("Sections", sections);
        level.put("TileEntities", blockEntities);
        level.put("Entities", new ListTag());
        level.put("TileTicks", new ListTag());
        level.putByteArray("Biomes", new byte[256]);
        level.putIntArray("HeightMap", new int[256]);

        final CompoundTag chunk = new CompoundTag();
        chunk.putInt("DataVersion", DATA_VERSION_1_12_2);
        chunk.put("Level", level);
        return chunk;
    }

    private static CompoundTag legacyLevelData() {
        final ListTag ids = new ListTag();
        ids.add(registryEntry("chiselsandbits:chiseled_rock", 256));
        ids.add(registryEntry("extrabitmanipulation:bodypart_template", 4095));

        final CompoundTag blockRegistry = new CompoundTag();
        blockRegistry.put("ids", ids);
        final CompoundTag registries = new CompoundTag();
        registries.put("minecraft:blocks", blockRegistry);
        final CompoundTag fml = new CompoundTag();
        fml.put("Registries", registries);
        final CompoundTag root = new CompoundTag();
        root.put("FML", fml);
        return root;
    }

    private static CompoundTag legacyPlayer() {
        final CompoundTag blockEntity = new CompoundTag();
        blockEntity.putInt("b", 14 << 12 | 35);
        blockEntity.putByteArray("X", LEGACY_1_12_BLOB);
        blockEntity.putInt("s", 0);
        blockEntity.putInt("lv", 15);
        blockEntity.putBoolean("nc", true);

        final CompoundTag tag = new CompoundTag();
        tag.put("BlockEntityTag", blockEntity);
        tag.putByte("side", (byte) 3);

        final CompoundTag item = new CompoundTag();
        item.putByte("Slot", (byte) 0);
        item.putString("id", "chiselsandbits:chiseled_rock");
        item.putByte("Count", (byte) 1);
        item.putShort("Damage", (short) 0);
        item.put("tag", tag);

        final ListTag inventory = new ListTag();
        inventory.add(item);

        final CompoundTag armorData = new CompoundTag();
        armorData.putString("migration_marker", "kept");
        final CompoundTag armorTag = new CompoundTag();
        armorTag.put("data", armorData);
        final CompoundTag armor = new CompoundTag();
        armor.putByte("Slot", (byte) 1);
        armor.putString("id", "extrabitmanipulation:chiseled_chestplate");
        armor.putByte("Count", (byte) 1);
        armor.putShort("Damage", (short) 0);
        armor.put("tag", armorTag);
        inventory.add(armor);

        final CompoundTag player = new CompoundTag();
        player.put("Inventory", inventory);
        return player;
    }

    private static CompoundTag registryEntry(final String name, final int id) {
        final CompoundTag entry = new CompoundTag();
        entry.putString("K", name);
        entry.putInt("V", id);
        return entry;
    }
}
