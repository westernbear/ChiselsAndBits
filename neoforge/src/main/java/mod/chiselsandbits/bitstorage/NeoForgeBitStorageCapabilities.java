package mod.chiselsandbits.bitstorage;

import java.util.Optional;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModTileEntityTypes;
import mod.chiselsandbits.utils.FluidUnits;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.Nullable;

/** NeoForge fluid/item capabilities for the bit storage block and its item form. */
@EventBusSubscriber(modid = ChiselsAndBits.MODID)
public final class NeoForgeBitStorageCapabilities {

    private NeoForgeBitStorageCapabilities() {}

    @SubscribeEvent
    public static void register(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModTileEntityTypes.BIT_STORAGE.get(),
                (storage, side) -> new BitStorageFluidHandler(storage));
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModTileEntityTypes.BIT_STORAGE.get(),
                (storage, side) -> new BitStorageItemHandler(storage));
        event.registerItem(
                Capabilities.Fluid.ITEM,
                (stack, access) -> new ItemTankFluidHandler(access),
                ModBlocks.BIT_STORAGE_BLOCK_ITEM.get());
    }

    /** Fluid tank: one index, one bucket, only while no solid bits are stored. */
    static final class BitStorageFluidHandler extends FluidStacksResourceHandler {

        private final TileEntityBitStorage storage;

        BitStorageFluidHandler(final TileEntityBitStorage storage) {
            super(NonNullList.of(FluidStack.EMPTY, storedFluid(storage)), (int) FluidUnits.BUCKET);
            this.storage = storage;
        }

        private static FluidStack storedFluid(final TileEntityBitStorage storage) {
            final net.minecraft.world.level.material.Fluid fluid = storage.getStoredFluid();
            return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, (int) storage.getFluidAmount());
        }

        @Override
        protected void onContentsChanged(final int index, final FluidStack previous) {
            final FluidStack current = stacks.get(index);
            storage.setFluid(current.isEmpty() ? null : current.getFluid(), current.getAmount());
        }

        @Override
        public boolean isValid(final int index, final FluidResource resource) {
            return storage.getState() == null;
        }
    }

    /** Item slot: one index holding up to {@link TileEntityBitStorage#MAX_CONTENTS} bits. */
    static final class BitStorageItemHandler extends ItemStacksResourceHandler {

        private final TileEntityBitStorage storage;

        BitStorageItemHandler(final TileEntityBitStorage storage) {
            super(NonNullList.of(ItemStack.EMPTY, storage.getStackInSlot(0)));
            this.storage = storage;
        }

        @Override
        protected int getCapacity(final int index, final ItemResource resource) {
            return TileEntityBitStorage.MAX_CONTENTS;
        }

        @Override
        protected void onContentsChanged(final int index, final ItemStack previous) {
            storage.setItemStorageStack(stacks.get(index));
        }
    }

    /** Tank carried by the item form, backed by its fluid data component. */
    static final class ItemTankFluidHandler extends ItemAccessResourceHandler<FluidResource> {

        ItemTankFluidHandler(final ItemAccess itemAccess) {
            super(itemAccess, 1);
        }

        @Override
        protected FluidResource getResourceFrom(final ItemResource accessResource, final int index) {
            final Optional<FluidResource> fluid = Optional.ofNullable(
                            ItemBlockBitStorage.getStoredFluid(accessResource.toStack(1)))
                    .map(FluidResource::of);
            return fluid.orElse(FluidResource.EMPTY);
        }

        @Override
        protected int getAmountFrom(final ItemResource accessResource, final int index) {
            return (int) ItemBlockBitStorage.getFluidAmount(accessResource.toStack(1));
        }

        @Override
        protected @Nullable ItemResource update(
                final ItemResource accessResource,
                final int index,
                final FluidResource newResource,
                final int newAmount) {
            final ItemStack stack = accessResource.toStack(1);
            ItemBlockBitStorage.setFluid(stack, newResource.isEmpty() ? null : newResource.getFluid(), newAmount);
            return ItemResource.of(stack);
        }

        @Override
        protected int getCapacity(final int index, final FluidResource resource) {
            return (int) FluidUnits.BUCKET;
        }
    }
}
