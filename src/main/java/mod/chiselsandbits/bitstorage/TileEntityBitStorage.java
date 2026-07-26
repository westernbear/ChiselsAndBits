package mod.chiselsandbits.bitstorage;

import mod.chiselsandbits.api.IBitBag;
import mod.chiselsandbits.api.ItemType;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.DeprecationHelper;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.items.ItemChiseledBit;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.registry.ModTileEntityTypes;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TileEntityBitStorage extends BlockEntity implements SidedStorageBlockEntity {

    public static final int MAX_CONTENTS = 4096;

    private BlockState state;
    private Fluid myFluid;
    private int bits;
    private long fluidAmount;
    private int oldLV = -1;

    private final SingleStackStorage itemStorage = new SingleStackStorage() {
        @Override
        protected ItemStack getStack() {
            return getStackInSlot(0);
        }

        @Override
        protected void setStack(final ItemStack stack) {
            setItemStorageStack(stack);
        }

        @Override
        protected boolean canInsert(final ItemVariant itemVariant) {
            return itemVariant.getItem() instanceof ItemChiseledBit;
        }

        @Override
        protected int getCapacity(final ItemVariant itemVariant) {
            return MAX_CONTENTS;
        }

        @Override
        public void updateSnapshots(final TransactionContext transaction) {
            storageSnapshot.updateSnapshots(transaction);
        }
    };

    private final SingleFluidStorage fluidStorage = new SingleFluidStorage() {
        @Override
        protected long getCapacity(final FluidVariant variant) {
            return FluidConstants.BUCKET;
        }

        @Override
        protected boolean canInsert(final FluidVariant variant) {
            return state == null;
        }

        @Override
        public void updateSnapshots(final TransactionContext transaction) {
            storageSnapshot.updateSnapshots(transaction);
        }

        @Override
        public long insert(final FluidVariant variant, final long maxAmount, final TransactionContext transaction) {
            final long inserted = super.insert(variant, maxAmount, transaction);
            if (inserted > 0) {
                applyFluidStorage();
            }
            return inserted;
        }

        @Override
        public long extract(final FluidVariant variant, final long maxAmount, final TransactionContext transaction) {
            final long extracted = super.extract(variant, maxAmount, transaction);
            if (extracted > 0) {
                applyFluidStorage();
            }
            return extracted;
        }
    };

    private final SnapshotParticipant<StorageSnapshot> storageSnapshot = new SnapshotParticipant<>() {
        @Override
        protected StorageSnapshot createSnapshot() {
            return new StorageSnapshot(state, myFluid, bits, fluidAmount, fluidStorage.variant, fluidStorage.amount);
        }

        @Override
        protected void readSnapshot(final StorageSnapshot snapshot) {
            state = snapshot.state();
            myFluid = snapshot.fluid();
            bits = snapshot.bits();
            fluidAmount = snapshot.fluidAmount();
            fluidStorage.variant = snapshot.variant();
            fluidStorage.amount = snapshot.storedFluidAmount();
        }

        @Override
        protected void onFinalCommit() {
            saveAndUpdate();
        }
    };

    public TileEntityBitStorage(final BlockPos pos, final BlockState state) {
        super(ModTileEntityTypes.BIT_STORAGE.get(), pos, state);
    }

    @Override
    public Storage<ItemVariant> getItemStorage(@Nullable final Direction side) {
        return itemStorage;
    }

    @Override
    public Storage<FluidVariant> getFluidStorage(@Nullable final Direction side) {
        return fluidStorage;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithFullMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return new ClientboundBlockEntityDataPacket(
                getBlockPos(), ModTileEntityTypes.BIT_STORAGE.get(), saveWithFullMetadata());
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        state = null;
        myFluid = null;
        bits = Math.max(0, Math.min(MAX_CONTENTS, nbt.getInt("bits")));
        fluidAmount = 0;
        fluidStorage.variant = FluidVariant.blank();
        fluidStorage.amount = 0;

        if (nbt.contains("fluid_storage", Tag.TAG_COMPOUND)) {
            fluidStorage.readNbt(nbt.getCompound("fluid_storage"));
        } else {
            final String fluidId = nbt.getString("fluid");
            if (!fluidId.isEmpty()) {
                final Fluid fluid = sourceFluid(BuiltInRegistries.FLUID.get(new ResourceLocation(fluidId)));
                if (fluid != Fluids.EMPTY) {
                    fluidStorage.variant = FluidVariant.of(fluid);
                    fluidStorage.amount = nbt.contains("fluid_amount", Tag.TAG_ANY_NUMERIC)
                            ? nbt.getLong("fluid_amount")
                            : bitsToDroplets(bits);
                }
            }
        }

        if (!fluidStorage.variant.isBlank() && fluidStorage.amount > 0) {
            fluidStorage.amount = Math.min(FluidConstants.BUCKET, fluidStorage.amount);
            myFluid = sourceFluid(fluidStorage.variant.getFluid());
            fluidAmount = fluidStorage.amount;
            bits = dropletsToBits(fluidAmount);
        } else {
            fluidStorage.variant = FluidVariant.blank();
            fluidStorage.amount = 0;
            final int rawState = nbt.getInt("blockstate");
            state = rawState == -1 ? null : ModUtil.getStateById(rawState);
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag nbt) {
        super.saveAdditional(nbt);
        final ResourceLocation fluidId = myFluid == null ? null : BuiltInRegistries.FLUID.getKey(myFluid);
        nbt.putString("fluid", fluidId == null ? "" : fluidId.toString());
        nbt.putInt("blockstate", myFluid != null || state == null ? -1 : ModUtil.getStateId(state));
        nbt.putInt("bits", bits);
        nbt.putLong("fluid_amount", fluidAmount);

        final CompoundTag fluidNbt = new CompoundTag();
        fluidStorage.writeNbt(fluidNbt);
        nbt.put("fluid_storage", fluidNbt);
    }

    public ItemStack getStackInSlot(final int slot) {
        if (bits > 0 && slot == 0) {
            return myFluid == null ? getBlockBitStack(state, bits) : getFluidBitStack(myFluid, bits);
        }
        return ItemStack.EMPTY;
    }

    public @NotNull ItemStack getFluidBitStack(final Fluid liquid, final int amount) {
        if (liquid == null) {
            return ItemStack.EMPTY;
        }
        return ItemChiseledBit.createStack(
                ModUtil.getStateId(sourceFluid(liquid).defaultFluidState().createLegacyBlock()), amount, false);
    }

    public @NotNull ItemStack getBlockBitStack(final BlockState blockState, final int amount) {
        return blockState == null
                ? ItemStack.EMPTY
                : ItemChiseledBit.createStack(ModUtil.getStateId(blockState), amount, false);
    }

    public @NotNull ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        if (slot != 0 || stack.isEmpty()) {
            return stack;
        }

        if (stack.getItem() instanceof ItemChiseledBit) {
            final BlockState blockState = ModUtil.getStateById(ItemChiseledBit.getStackState(stack));
            if (blockState == null) {
                return stack;
            }

            final Fluid fluid = fluidFrom(blockState);
            return fluid == null
                    ? attemptSolidBitStackInsertion(stack, simulate, blockState)
                    : attemptFluidBitStackInsertion(stack, simulate, fluid);
        }

        if (BlockBitInfo.canChisel(stack) && bits == 0 && state == null && myFluid == null) {
            final BlockState stackState = ModUtil.getStateFromItem(stack);
            if (stackState.getBlock() != Blocks.AIR) {
                if (!simulate) {
                    state = stackState;
                    bits = MAX_CONTENTS;
                    clearFluidStorage();
                    saveAndUpdate();
                }

                final ItemStack remainder = stack.copy();
                remainder.shrink(1);
                return remainder;
            }
        }

        return stack;
    }

    private ItemStack attemptFluidBitStackInsertion(
            final ItemStack stack, final boolean simulate, final Fluid insertedFluid) {
        if (state != null || myFluid != null && sourceFluid(myFluid) != insertedFluid) {
            return stack;
        }

        final int inserted = Math.min(stack.getCount(), MAX_CONTENTS - bits);
        if (inserted <= 0) {
            return stack;
        }

        if (!simulate) {
            myFluid = insertedFluid;
            state = null;
            bits += inserted;
            syncFluidStorageFromBits();
            saveAndUpdate();
        }
        return remainder(stack, inserted);
    }

    private ItemStack attemptSolidBitStackInsertion(
            final ItemStack stack, final boolean simulate, final BlockState insertedState) {
        if (myFluid != null || state != null && !state.equals(insertedState)) {
            return stack;
        }

        final int inserted = Math.min(stack.getCount(), MAX_CONTENTS - bits);
        if (inserted <= 0) {
            return stack;
        }

        if (!simulate) {
            myFluid = null;
            state = insertedState;
            bits += inserted;
            clearFluidStorage();
            saveAndUpdate();
        }
        return remainder(stack, inserted);
    }

    private static ItemStack remainder(final ItemStack stack, final int inserted) {
        if (inserted == stack.getCount()) {
            return ItemStack.EMPTY;
        }
        final ItemStack remainder = stack.copy();
        remainder.shrink(inserted);
        return remainder;
    }

    private void setItemStorageStack(final ItemStack stack) {
        if (stack.isEmpty()) {
            state = null;
            myFluid = null;
            bits = 0;
            clearFluidStorage();
            return;
        }
        if (!(stack.getItem() instanceof ItemChiseledBit)) {
            return;
        }

        final BlockState blockState = ModUtil.getStateById(ItemChiseledBit.getStackState(stack));
        final Fluid fluid = fluidFrom(blockState);
        bits = Math.min(MAX_CONTENTS, stack.getCount());
        if (fluid == null) {
            state = blockState;
            myFluid = null;
            clearFluidStorage();
        } else {
            state = null;
            myFluid = fluid;
            syncFluidStorageFromBits();
        }
    }

    private static Fluid fluidFrom(@Nullable final BlockState blockState) {
        if (blockState == null || blockState.getFluidState().isEmpty()) {
            return null;
        }
        final Fluid fluid = sourceFluid(blockState.getFluidState().getType());
        return fluid != Fluids.EMPTY
                        && fluid.defaultFluidState().createLegacyBlock().getBlock() == blockState.getBlock()
                ? fluid
                : null;
    }

    private static Fluid sourceFluid(final Fluid fluid) {
        return fluid instanceof FlowingFluid flowingFluid ? flowingFluid.getSource() : fluid;
    }

    private void syncFluidStorageFromBits() {
        if (myFluid == null || bits <= 0) {
            clearFluidStorage();
            return;
        }
        fluidAmount = bitsToDroplets(bits);
        fluidStorage.variant = FluidVariant.of(sourceFluid(myFluid));
        fluidStorage.amount = fluidAmount;
    }

    private void clearFluidStorage() {
        fluidAmount = 0;
        fluidStorage.variant = FluidVariant.blank();
        fluidStorage.amount = 0;
    }

    private void applyFluidStorage() {
        fluidAmount = Math.max(0, Math.min(FluidConstants.BUCKET, fluidStorage.amount));
        fluidStorage.amount = fluidAmount;
        if (fluidStorage.variant.isBlank() || fluidAmount == 0) {
            state = null;
            myFluid = null;
            bits = 0;
            clearFluidStorage();
        } else {
            state = null;
            myFluid = sourceFluid(fluidStorage.variant.getFluid());
            bits = dropletsToBits(fluidAmount);
        }
    }

    public void setFluid(final FluidVariant variant, final long amount) {
        fluidStorage.variant = amount <= 0 ? FluidVariant.blank() : variant;
        fluidStorage.amount = Math.max(0, Math.min(FluidConstants.BUCKET, amount));
        applyFluidStorage();
        saveAndUpdate();
    }

    public FluidVariant getFluidVariant() {
        return fluidStorage.variant;
    }

    public long getFluidAmount() {
        return fluidAmount;
    }

    public static long bitsToDroplets(final int bits) {
        final int clamped = Math.max(0, Math.min(MAX_CONTENTS, bits));
        return ((long) clamped * FluidConstants.BUCKET + MAX_CONTENTS - 1) / MAX_CONTENTS;
    }

    public static int dropletsToBits(final long amount) {
        final long clamped = Math.max(0, Math.min(FluidConstants.BUCKET, amount));
        return (int) (clamped * MAX_CONTENTS / FluidConstants.BUCKET);
    }

    private void saveAndUpdate() {
        setChanged();
        if (level == null) {
            return;
        }

        ModUtil.sendUpdate(level, getBlockPos());
        final int lightValue = getLightValue();
        if (oldLV != lightValue) {
            level.getLightEngine().checkBlock(getBlockPos());
            oldLV = lightValue;
        }
    }

    public @NotNull ItemStack extractBits(final int slot, final int amount, final boolean simulate) {
        final ItemStack contents = getStackInSlot(slot);
        if (contents.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }

        contents.setCount(Math.min(amount, contents.getCount()));
        if (!simulate) {
            bits -= contents.getCount();
            if (bits <= 0) {
                bits = 0;
                state = null;
                myFluid = null;
                clearFluidStorage();
            } else if (myFluid != null) {
                syncFluidStorageFromBits();
            }
            saveAndUpdate();
        }
        return contents;
    }

    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        return extractBits(slot, Math.min(amount, ModItems.ITEM_BLOCK_BIT.get().getMaxStackSize()), simulate);
    }

    public int getLightValue() {
        final BlockState workingState =
                myFluid == null ? state : myFluid.defaultFluidState().createLegacyBlock();
        return workingState == null ? 0 : DeprecationHelper.getLightValue(workingState);
    }

    boolean extractBits(
            final Player playerIn, final double hitX, final double hitY, final double hitZ, final BlockPos pos) {
        if (!playerIn.isShiftKeyDown()) {
            final ItemStack extracted = extractItem(0, 64, false);
            if (!extracted.isEmpty()) {
                ChiselsAndBits.getApi()
                        .giveBitToPlayer(
                                playerIn, extracted, new Vec3(hitX + pos.getX(), hitY + pos.getY(), hitZ + pos.getZ()));
            }
            return true;
        }
        return false;
    }

    boolean addAllPossibleBits(final Player playerIn) {
        if (!playerIn.isShiftKeyDown()) {
            return false;
        }

        boolean changed = false;
        for (int slot = 0; slot < playerIn.inventory.getContainerSize(); slot++) {
            final ItemStack stack = playerIn.inventory.getItem(slot);
            if (ChiselsAndBits.getApi().getItemType(stack) == ItemType.CHISELED_BIT) {
                playerIn.inventory.setItem(slot, insertItem(0, stack, false));
                changed = true;
            } else if (ChiselsAndBits.getApi().getItemType(stack) == ItemType.BIT_BAG) {
                final IBitBag bag = ChiselsAndBits.getApi().getBitbag(stack);
                if (bag == null) {
                    continue;
                }
                for (int bagSlot = 0; bagSlot < bag.getSlots(); bagSlot++) {
                    final ItemStack extracted = bag.extractItem(bagSlot, bag.getSlotLimit(bagSlot), false);
                    bag.insertItem(bagSlot, insertItem(0, extracted, false), false);
                    changed = true;
                }
            }
        }

        if (changed) {
            playerIn.inventory.setChanged();
        }
        return changed;
    }

    boolean addHeldBits(final @NotNull ItemStack current, final Player playerIn) {
        if ((playerIn.isShiftKeyDown() || bits == 0)
                && (ChiselsAndBits.getApi().getItemType(current) == ItemType.CHISELED_BIT
                        || BlockBitInfo.canChisel(current))) {
            final ItemStack resultStack = insertItem(0, current, false);
            if (!playerIn.isCreative()) {
                playerIn.inventory.setItem(playerIn.inventory.selected, resultStack);
                playerIn.inventory.setChanged();
            }
            return true;
        }
        return false;
    }

    public int getSlotLimit(final int slot) {
        return MAX_CONTENTS;
    }

    public boolean isItemValid(final int slot, final ItemStack stack) {
        return slot == 0
                && !stack.isEmpty()
                && (stack.getItem() instanceof ItemChiseledBit || BlockBitInfo.canChisel(stack));
    }

    public BlockState getState() {
        return state;
    }

    public Fluid getMyFluid() {
        return myFluid;
    }

    public int getBits() {
        return bits;
    }

    private record StorageSnapshot(
            BlockState state, Fluid fluid, int bits, long fluidAmount, FluidVariant variant, long storedFluidAmount) {}
}
