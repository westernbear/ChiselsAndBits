package mod.chiselsandbits.gametest;

import mod.chiselsandbits.bitstorage.TileEntityBitStorage;
import mod.chiselsandbits.utils.FluidUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** NeoForge-side coverage for the capability-backed tank. */
public final class NeoForgeBitStorageBodies {

    private static final BlockPos STORAGE = new BlockPos(1, 1, 1);

    private NeoForgeBitStorageBodies() {}

    public static void rejectsFluidWhileSolidBitsAreStored(final GameTestHelper helper) {
        final TileEntityBitStorage storage = BitStorageGameTestsBodies.placeStorage(helper);
        storage.insertItem(0, BitStorageGameTestsBodies.stoneBits(64), false);

        final ResourceHandler<FluidResource> handler = Capabilities.Fluid.BLOCK.getCapability(
                helper.getLevel(), helper.absolutePos(STORAGE), null, storage, null);
        if (handler == null) {
            throw new AssertionError("bit storage fluid capability missing");
        }

        try (Transaction transaction = Transaction.openRoot()) {
            final int inserted = handler.insert(FluidResource.of(Fluids.WATER), (int) FluidUnits.BUCKET, transaction);
            transaction.commit();
            helper.assertValueEqual(inserted, 0, "fluid rejected while solid bits present");
        }
        helper.succeed();
    }
}
