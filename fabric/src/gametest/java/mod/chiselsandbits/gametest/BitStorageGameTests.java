package mod.chiselsandbits.gametest;

import mod.chiselsandbits.bitstorage.FabricBitStorageStorage;
import mod.chiselsandbits.bitstorage.TileEntityBitStorage;
import mod.chiselsandbits.utils.FluidUnits;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;

public class BitStorageGameTests {

    @GameTest(maxTicks = 100)
    public void clampsBitsToCapacityAndRejectsOverflow(final GameTestHelper helper) {
        BitStorageGameTestsBodies.clampsBitsToCapacityAndRejectsOverflow(helper);
    }

    @GameTest(maxTicks = 100)
    public void convertsFullBlockIntoStorageEntry(final GameTestHelper helper) {
        BitStorageGameTestsBodies.convertsFullBlockIntoStorageEntry(helper);
    }

    @GameTest(maxTicks = 100)
    public void rejectsMixedBitMaterials(final GameTestHelper helper) {
        BitStorageGameTestsBodies.rejectsMixedBitMaterials(helper);
    }

    @GameTest(maxTicks = 100)
    public void clampsFluidAndClearsOnNonPositiveAmount(final GameTestHelper helper) {
        BitStorageGameTestsBodies.clampsFluidAndClearsOnNonPositiveAmount(helper);
    }

    @GameTest(maxTicks = 100)
    public void itemFluidComponentClampsAndClears(final GameTestHelper helper) {
        BitStorageGameTestsBodies.itemFluidComponentClampsAndClears(helper);
    }

    @GameTest(maxTicks = 100)
    public void dropletConversionIsLosslessAtBoundaries(final GameTestHelper helper) {
        BitStorageGameTestsBodies.dropletConversionIsLosslessAtBoundaries(helper);
    }

    @GameTest(maxTicks = 100)
    public void rejectsFluidWhileSolidBitsAreStored(final GameTestHelper helper) {
        final TileEntityBitStorage storage = BitStorageGameTestsBodies.placeStorage(helper);
        storage.insertItem(0, BitStorageGameTestsBodies.stoneBits(64), false);

        try (Transaction transaction = Transaction.openOuter()) {
            final long inserted = FabricBitStorageStorage.fluidStorage(storage)
                    .insert(FluidVariant.of(Fluids.WATER), FluidUnits.BUCKET, transaction);
            transaction.commit();
            helper.assertValueEqual(inserted, 0L, "fluid rejected while solid bits present");
        }

        helper.succeed();
    }
}
