package mod.chiselsandbits.bitstorage;

import java.util.WeakHashMap;
import mod.chiselsandbits.items.ItemChiseledBit;
import mod.chiselsandbits.utils.FluidUnits;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

public final class FabricBitStorageStorage {

    private static final WeakHashMap<TileEntityBitStorage, Holder> HOLDERS = new WeakHashMap<>();

    private FabricBitStorageStorage() {}

    public static Storage<ItemVariant> itemStorage(final TileEntityBitStorage storage) {
        return holder(storage).itemStorage;
    }

    public static Storage<FluidVariant> fluidStorage(final TileEntityBitStorage storage) {
        return holder(storage).fluidStorage;
    }

    private static Holder holder(final TileEntityBitStorage storage) {
        synchronized (HOLDERS) {
            return HOLDERS.computeIfAbsent(storage, Holder::new);
        }
    }

    private static final class Holder {
        private final TileEntityBitStorage storage;
        private final SingleStackStorage itemStorage;
        private final SingleFluidStorage fluidStorage;
        private final SnapshotParticipant<StorageSnapshot> storageSnapshot;

        private Holder(final TileEntityBitStorage storage) {
            this.storage = storage;
            this.storageSnapshot = new SnapshotParticipant<>() {
                @Override
                protected StorageSnapshot createSnapshot() {
                    return new StorageSnapshot(
                            storage.getState(),
                            storage.getMyFluid(),
                            storage.getBits(),
                            storage.getFluidAmount(),
                            fluidStorage.variant,
                            fluidStorage.amount);
                }

                @Override
                protected void readSnapshot(final StorageSnapshot snapshot) {
                    if (snapshot.fluid() != null && snapshot.fluidAmount() > 0) {
                        storage.setFluid(snapshot.fluid(), snapshot.fluidAmount());
                    } else if (snapshot.state() != null && snapshot.bits() > 0) {
                        storage.setFluid(null, 0);
                        storage.setItemStorageStack(storage.getBlockBitStack(snapshot.state(), snapshot.bits()));
                    } else {
                        storage.setFluid(null, 0);
                    }
                    fluidStorage.variant = snapshot.variant();
                    fluidStorage.amount = snapshot.storedFluidAmount();
                }

                @Override
                protected void onFinalCommit() {
                    // setFluid / setItemStorageStack already persist changes.
                }
            };

            this.itemStorage = new SingleStackStorage() {
                @Override
                protected ItemStack getStack() {
                    return storage.getStackInSlot(0);
                }

                @Override
                protected void setStack(final ItemStack stack) {
                    storage.setItemStorageStack(stack);
                }

                @Override
                protected boolean canInsert(final ItemVariant itemVariant) {
                    return itemVariant.getItem() instanceof ItemChiseledBit;
                }

                @Override
                protected int getCapacity(final ItemVariant itemVariant) {
                    return TileEntityBitStorage.MAX_CONTENTS;
                }

                @Override
                public void updateSnapshots(final TransactionContext transaction) {
                    storageSnapshot.updateSnapshots(transaction);
                }
            };

            this.fluidStorage = new SingleFluidStorage() {
                @Override
                protected long getCapacity(final FluidVariant variant) {
                    return FluidUnits.BUCKET;
                }

                @Override
                protected boolean canInsert(final FluidVariant variant) {
                    return storage.getState() == null;
                }

                @Override
                public void updateSnapshots(final TransactionContext transaction) {
                    storageSnapshot.updateSnapshots(transaction);
                }

                @Override
                public long insert(
                        final FluidVariant variant, final long maxAmount, final TransactionContext transaction) {
                    final long inserted = super.insert(variant, maxAmount, transaction);
                    if (inserted > 0) {
                        applyFluidStorage();
                    }
                    return inserted;
                }

                @Override
                public long extract(
                        final FluidVariant variant, final long maxAmount, final TransactionContext transaction) {
                    final long extracted = super.extract(variant, maxAmount, transaction);
                    if (extracted > 0) {
                        applyFluidStorage();
                    }
                    return extracted;
                }

                private void applyFluidStorage() {
                    final long clamped = Math.max(0, Math.min(FluidUnits.BUCKET, amount));
                    amount = clamped;
                    if (variant.isBlank() || clamped == 0) {
                        storage.setFluid(null, 0);
                    } else {
                        storage.setFluid(variant.getFluid(), clamped);
                    }
                }
            };

            syncFluidFromTile();
        }

        private void syncFluidFromTile() {
            final Fluid fluid = storage.getStoredFluid();
            final long fluidAmount = storage.getFluidAmount();
            if (fluid == null || fluidAmount <= 0) {
                fluidStorage.variant = FluidVariant.blank();
                fluidStorage.amount = 0;
            } else {
                fluidStorage.variant = FluidVariant.of(fluid);
                fluidStorage.amount = fluidAmount;
            }
        }
    }

    private record StorageSnapshot(
            @Nullable BlockState state,
            @Nullable Fluid fluid,
            int bits,
            long fluidAmount,
            FluidVariant variant,
            long storedFluidAmount) {}
}
