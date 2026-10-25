package mod.chiselsandbits.gametest;

import mod.chiselsandbits.api.APIExceptions.InvalidBitItem;
import mod.chiselsandbits.api.IChiselAndBitsAPI;
import mod.chiselsandbits.bitstorage.ItemBlockBitStorage;
import mod.chiselsandbits.bitstorage.TileEntityBitStorage;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.utils.FluidUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;

/** Edge cases for the bit-storage block entity and its item form. */
public final class BitStorageGameTestsBodies {

    private static final BlockPos STORAGE_POS = new BlockPos(1, 1, 1);

    public static void clampsBitsToCapacityAndRejectsOverflow(final GameTestHelper helper) {
        final TileEntityBitStorage storage = placeStorage(helper);

        final ItemStack overflow = storage.insertItem(0, stoneBits(TileEntityBitStorage.MAX_CONTENTS + 904), false);
        helper.assertValueEqual(storage.getBits(), TileEntityBitStorage.MAX_CONTENTS, "stored bits clamped");
        helper.assertValueEqual(overflow.getCount(), 904, "overflow returned to caller");

        helper.assertValueEqual(
                storage.insertItem(0, stoneBits(1), false).getCount(), 1, "full storage accepts nothing");

        helper.succeed();
    }

    public static void convertsFullBlockIntoStorageEntry(final GameTestHelper helper) {
        final TileEntityBitStorage storage = placeStorage(helper);

        final ItemStack remainder = storage.insertItem(0, new ItemStack(Blocks.STONE), false);
        helper.assertTrue(remainder.isEmpty(), "full block fully consumed");
        helper.assertValueEqual(storage.getBits(), TileEntityBitStorage.MAX_CONTENTS, "full block fills storage");

        helper.succeed();
    }

    public static void rejectsMixedBitMaterials(final GameTestHelper helper) {
        final TileEntityBitStorage storage = placeStorage(helper);

        storage.insertItem(0, stoneBits(16), false);
        final ItemStack dirtBits = bitItem(Blocks.DIRT);
        dirtBits.setCount(16);
        helper.assertValueEqual(storage.insertItem(0, dirtBits, false).getCount(), 16, "second material rejected");
        helper.assertValueEqual(storage.getBits(), 16, "rejected insert left state untouched");

        helper.succeed();
    }

    public static void clampsFluidAndClearsOnNonPositiveAmount(final GameTestHelper helper) {
        final TileEntityBitStorage storage = placeStorage(helper);

        storage.setFluid(Fluids.WATER, FluidUnits.BUCKET * 3);
        helper.assertValueEqual(storage.getFluidAmount(), FluidUnits.BUCKET, "fluid clamped to bucket");

        storage.setFluid(Fluids.WATER, -10);
        helper.assertTrue(storage.getStoredFluid() == null, "negative amount clears fluid");
        helper.assertValueEqual(storage.getFluidAmount(), 0L, "amount zeroed");

        helper.succeed();
    }

    public static void itemFluidComponentClampsAndClears(final GameTestHelper helper) {
        final ItemStack tank = new ItemStack(ModBlocks.BIT_STORAGE_BLOCK_ITEM.get());

        ItemBlockBitStorage.setFluid(tank, Fluids.WATER, FluidUnits.BUCKET * 2);
        helper.assertValueEqual(ItemBlockBitStorage.getFluidAmount(tank), FluidUnits.BUCKET, "component clamped");
        helper.assertTrue(ItemBlockBitStorage.getStoredFluid(tank) == Fluids.WATER, "component keeps fluid");

        ItemBlockBitStorage.setFluid(tank, Fluids.EMPTY, 100);
        helper.assertTrue(ItemBlockBitStorage.getStoredFluid(tank) == null, "empty fluid clears component");
        helper.assertValueEqual(ItemBlockBitStorage.getFluidAmount(tank), 0L, "cleared amount");

        helper.succeed();
    }

    public static void dropletConversionIsLosslessAtBoundaries(final GameTestHelper helper) {
        helper.assertValueEqual(TileEntityBitStorage.bitsToDroplets(0), 0L, "zero bits");
        helper.assertValueEqual(
                TileEntityBitStorage.bitsToDroplets(TileEntityBitStorage.MAX_CONTENTS),
                FluidUnits.BUCKET,
                "full bits is one bucket");
        helper.assertValueEqual(
                TileEntityBitStorage.dropletsToBits(FluidUnits.BUCKET),
                TileEntityBitStorage.MAX_CONTENTS,
                "one bucket is full bits");
        helper.assertValueEqual(TileEntityBitStorage.dropletsToBits(0), 0, "zero droplets");
        helper.assertValueEqual(TileEntityBitStorage.bitsToDroplets(1), 20L, "single bit rounds up");

        helper.succeed();
    }

    public static TileEntityBitStorage placeStorage(final GameTestHelper helper) {
        helper.setBlock(STORAGE_POS, ModBlocks.BIT_STORAGE_BLOCK.get());
        final BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(STORAGE_POS));
        if (!(entity instanceof TileEntityBitStorage storage)) {
            throw new AssertionError("bit storage block entity missing");
        }
        return storage;
    }

    public static ItemStack stoneBits(final int count) {
        final ItemStack bits = bitItem(Blocks.STONE);
        bits.setCount(count);
        return bits;
    }

    private static ItemStack bitItem(final net.minecraft.world.level.block.Block block) {
        try {
            return IChiselAndBitsAPI.getInstance().getBitItem(block.defaultBlockState());
        } catch (final InvalidBitItem error) {
            throw new AssertionError(error);
        }
    }
}
