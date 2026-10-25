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
import mod.chiselsandbits.utils.FluidUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TileEntityBitStorage extends BlockEntity {

    public static final int MAX_CONTENTS = 4096;

    private Contents contents = Contents.EMPTY;
    private int oldLV = -1;

    public TileEntityBitStorage(final BlockPos pos, final BlockState state) {
        super(ModTileEntityTypes.BIT_STORAGE.get(), pos, state);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return saveWithFullMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);

        final int storedBits = clampBits(input.getIntOr("bits", 0));
        Contents loaded = Contents.EMPTY;

        final String fluidId = input.getStringOr("fluid", "");
        if (!fluidId.isEmpty()) {
            final Fluid fluid = sourceFluid(BuiltInRegistries.FLUID.getValue(Identifier.parse(fluidId)));
            if (fluid != Fluids.EMPTY) {
                final long droplets =
                        Math.min(FluidUnits.BUCKET, input.getLongOr("fluid_amount", bitsToDroplets(storedBits)));
                loaded = Contents.ofFluid(fluid, dropletsToBits(droplets), droplets);
            }
        }

        if (loaded.fluid() == null) {
            final int rawState = input.getIntOr("blockstate", -1);
            loaded = Contents.ofBlock(rawState == -1 ? null : ModUtil.getStateById(rawState), storedBits);
        }

        contents = loaded;
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        final Identifier fluidId = contents.fluid() == null ? null : BuiltInRegistries.FLUID.getKey(contents.fluid());
        output.putString("fluid", fluidId == null ? "" : fluidId.toString());
        output.putInt(
                "blockstate",
                contents.fluid() != null || contents.state() == null ? -1 : ModUtil.getStateId(contents.state()));
        output.putInt("bits", contents.bits());
        output.putLong("fluid_amount", contents.droplets());
    }

    public ItemStack getStackInSlot(final int slot) {
        if (contents.bits() > 0 && slot == 0) {
            return contents.fluid() == null
                    ? getBlockBitStack(contents.state(), contents.bits())
                    : getFluidBitStack(contents.fluid(), contents.bits());
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

        if (BlockBitInfo.canChisel(stack) && contents.isEmpty()) {
            final BlockState stackState = ModUtil.getStateFromItem(stack);
            if (stackState.getBlock() != Blocks.AIR) {
                if (!simulate) {
                    contents = Contents.ofBlock(stackState, MAX_CONTENTS);
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
        if (contents.state() != null || contents.fluid() != null && sourceFluid(contents.fluid()) != insertedFluid) {
            return stack;
        }

        final int inserted = Math.min(stack.getCount(), contents.remainingCapacity());
        if (inserted <= 0) {
            return stack;
        }

        if (!simulate) {
            contents = contents.withAddedFluid(insertedFluid, inserted);
            saveAndUpdate();
        }
        return remainder(stack, inserted);
    }

    private ItemStack attemptSolidBitStackInsertion(
            final ItemStack stack, final boolean simulate, final BlockState insertedState) {
        if (contents.fluid() != null
                || contents.state() != null && !contents.state().equals(insertedState)) {
            return stack;
        }

        final int inserted = Math.min(stack.getCount(), contents.remainingCapacity());
        if (inserted <= 0) {
            return stack;
        }

        if (!simulate) {
            contents = contents.withAddedBlockBits(insertedState, inserted);
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

    void setItemStorageStack(final ItemStack stack) {
        if (stack.isEmpty()) {
            contents = Contents.EMPTY;
            return;
        }
        if (!(stack.getItem() instanceof ItemChiseledBit)) {
            return;
        }

        final BlockState blockState = ModUtil.getStateById(ItemChiseledBit.getStackState(stack));
        final Fluid fluid = fluidFrom(blockState);
        final int bits = Math.min(MAX_CONTENTS, stack.getCount());
        contents = fluid == null
                ? Contents.ofBlock(blockState, bits)
                : Contents.ofFluid(fluid, bits, bitsToDroplets(bits));
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

    public void setFluid(@Nullable final Fluid fluid, final long amount) {
        if (fluid == null || fluid == Fluids.EMPTY || amount <= 0) {
            contents = Contents.EMPTY;
        } else {
            final long droplets = Math.min(FluidUnits.BUCKET, amount);
            contents = Contents.ofFluid(sourceFluid(fluid), dropletsToBits(droplets), droplets);
        }
        saveAndUpdate();
    }

    @Nullable
    public Fluid getStoredFluid() {
        return contents.fluid();
    }

    public long getFluidAmount() {
        return contents.droplets();
    }

    public static long bitsToDroplets(final int bits) {
        final int clamped = clampBits(bits);
        return ((long) clamped * FluidUnits.BUCKET + MAX_CONTENTS - 1) / MAX_CONTENTS;
    }

    public static int dropletsToBits(final long amount) {
        final long clamped = Math.max(0, Math.min(FluidUnits.BUCKET, amount));
        return (int) (clamped * MAX_CONTENTS / FluidUnits.BUCKET);
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
            this.contents = this.contents.withoutBits(contents.getCount());
            saveAndUpdate();
        }
        return contents;
    }

    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        return extractBits(slot, Math.min(amount, ModItems.ITEM_BLOCK_BIT.get().getDefaultMaxStackSize()), simulate);
    }

    public int getLightValue() {
        final BlockState workingState = contents.fluid() == null
                ? contents.state()
                : contents.fluid().defaultFluidState().createLegacyBlock();
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
        for (int slot = 0; slot < playerIn.getInventory().getContainerSize(); slot++) {
            final ItemStack stack = playerIn.getInventory().getItem(slot);
            if (ChiselsAndBits.getApi().getItemType(stack) == ItemType.CHISELED_BIT) {
                playerIn.getInventory().setItem(slot, insertItem(0, stack, false));
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
            playerIn.getInventory().setChanged();
        }
        return changed;
    }

    boolean addHeldBits(final @NotNull ItemStack current, final Player playerIn) {
        if ((playerIn.isShiftKeyDown() || contents.bits() == 0)
                && (ChiselsAndBits.getApi().getItemType(current) == ItemType.CHISELED_BIT
                        || BlockBitInfo.canChisel(current))) {
            final ItemStack resultStack = insertItem(0, current, false);
            if (!playerIn.isCreative()) {
                playerIn.getInventory().setItem(playerIn.getInventory().getSelectedSlot(), resultStack);
                playerIn.getInventory().setChanged();
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
        return contents.state();
    }

    public Fluid getMyFluid() {
        return contents.fluid();
    }

    public int getBits() {
        return contents.bits();
    }

    /**
     * What the storage holds: block bits or a single fluid, never both. {@code bits} is the
     * item-count view; {@code droplets} keeps the exact fluid amount, since bits round.
     */
    private record Contents(
            @Nullable BlockState state, @Nullable Fluid fluid, int bits, long droplets) {
        static final Contents EMPTY = new Contents(null, null, 0, 0L);

        static Contents ofBlock(@Nullable final BlockState state, final int bits) {
            return new Contents(state, null, clampBits(bits), 0L);
        }

        static Contents ofFluid(final Fluid fluid, final int bits, final long droplets) {
            return droplets <= 0 ? EMPTY : new Contents(null, fluid, clampBits(bits), droplets);
        }

        boolean isEmpty() {
            return state == null && fluid == null && bits == 0;
        }

        int remainingCapacity() {
            return MAX_CONTENTS - bits;
        }

        Contents withAddedFluid(final Fluid addedFluid, final int addedBits) {
            final int total = bits + addedBits;
            return ofFluid(addedFluid, total, bitsToDroplets(total));
        }

        Contents withAddedBlockBits(final BlockState addedState, final int addedBits) {
            return ofBlock(addedState, bits + addedBits);
        }

        Contents withoutBits(final int removed) {
            final int remaining = bits - removed;
            if (remaining <= 0) {
                return EMPTY;
            }
            return fluid == null ? ofBlock(state, remaining) : ofFluid(fluid, remaining, bitsToDroplets(remaining));
        }
    }

    private static int clampBits(final int bits) {
        return Math.max(0, Math.min(MAX_CONTENTS, bits));
    }
}
