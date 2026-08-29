package mod.chiselsandbits.bitstorage;

import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModTileEntityTypes;
import mod.chiselsandbits.utils.FluidUnits;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantItemStorage;
import net.minecraft.world.item.ItemStack;

public final class FabricBitStorageRegistration {

    private FabricBitStorageRegistration() {}

    public static void register() {
        FluidStorage.ITEM.registerForItems(
                (stack, context) -> new TankItemFluidStorage(context), ModBlocks.BIT_STORAGE_BLOCK_ITEM.get());

        ItemStorage.SIDED.registerForBlockEntity(
                (blockEntity, direction) -> FabricBitStorageStorage.itemStorage((TileEntityBitStorage) blockEntity),
                ModTileEntityTypes.BIT_STORAGE.get());
        FluidStorage.SIDED.registerForBlockEntity(
                (blockEntity, direction) -> FabricBitStorageStorage.fluidStorage((TileEntityBitStorage) blockEntity),
                ModTileEntityTypes.BIT_STORAGE.get());
    }

    private static final class TankItemFluidStorage extends SingleVariantItemStorage<FluidVariant> {

        private TankItemFluidStorage(final ContainerItemContext context) {
            super(context);
        }

        @Override
        protected FluidVariant getBlankResource() {
            return FluidVariant.blank();
        }

        @Override
        protected FluidVariant getResource(final ItemVariant currentVariant) {
            final ItemStack stack = currentVariant.toStack();
            final var fluid = ItemBlockBitStorage.getStoredFluid(stack);
            return fluid == null ? FluidVariant.blank() : FluidVariant.of(fluid);
        }

        @Override
        protected long getAmount(final ItemVariant currentVariant) {
            return ItemBlockBitStorage.getFluidAmount(currentVariant.toStack());
        }

        @Override
        protected long getCapacity(final FluidVariant variant) {
            return FluidUnits.BUCKET;
        }

        @Override
        protected ItemVariant getUpdatedVariant(
                final ItemVariant currentVariant, final FluidVariant newResource, final long newAmount) {
            final ItemStack updatedStack = currentVariant.toStack();
            ItemBlockBitStorage.setFluid(
                    updatedStack, newResource.isBlank() ? null : newResource.getFluid(), newAmount);
            return ItemVariant.of(updatedStack);
        }
    }
}
