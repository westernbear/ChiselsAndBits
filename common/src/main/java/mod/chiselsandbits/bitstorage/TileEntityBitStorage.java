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

    private BlockState state;
    private Fluid myFluid;
    private int bits;
    private long fluidAmount;
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
        state = null;
        myFluid = null;
        bits = Math.max(0, Math.min(MAX_CONTENTS, input.getIntOr("bits", 0)));
        fluidAmount = 0;

        final String fluidId = input.getStringOr("fluid", "");
        if (!fluidId.isEmpty()) {
            final Fluid fluid = sourceFluid(BuiltInRegistries.FLUID.getValue(Identifier.parse(fluidId)));
            if (fluid != Fluids.EMPTY) {
                myFluid = fluid;
                fluidAmount = input.getLongOr("fluid_amount", bitsToDroplets(bits));
                fluidAmount = Math.min(FluidUnits.BUCKET, fluidAmount);
                bits = dropletsToBits(fluidAmount);
            }
        }

        if (myFluid == null || fluidAmount <= 0) {
            final int rawState = input.getIntOr("blockstate", -1);
            state = rawState == -1 ? null : ModUtil.getStateById(rawState);
            if (state != null) {
                bits = Math.max(0, Math.min(MAX_CONTENTS, bits));
            }
        }
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        final Identifier fluidId = myFluid == null ? null : BuiltInRegistries.FLUID.getKey(myFluid);
        output.putString("fluid", fluidId == null ? "" : fluidId.toString());
        output.putInt("blockstate", myFluid != null || state == null ? -1 : ModUtil.getStateId(state));
        output.putInt("bits", bits);
        output.putLong("fluid_amount", fluidAmount);
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

    void setItemStorageStack(final ItemStack stack) {
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
    }

    private void clearFluidStorage() {
        fluidAmount = 0;
    }

    public void setFluid(@Nullable final Fluid fluid, final long amount) {
        if (fluid == null || fluid == Fluids.EMPTY || amount <= 0) {
            state = null;
            myFluid = null;
            bits = 0;
            clearFluidStorage();
        } else {
            state = null;
            myFluid = sourceFluid(fluid);
            fluidAmount = Math.max(0, Math.min(FluidUnits.BUCKET, amount));
            bits = dropletsToBits(fluidAmount);
        }
        saveAndUpdate();
    }

    @Nullable
    public Fluid getStoredFluid() {
        return myFluid;
    }

    public long getFluidAmount() {
        return fluidAmount;
    }

    public static long bitsToDroplets(final int bits) {
        final int clamped = Math.max(0, Math.min(MAX_CONTENTS, bits));
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
        return extractBits(slot, Math.min(amount, ModItems.ITEM_BLOCK_BIT.get().getDefaultMaxStackSize()), simulate);
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
        if ((playerIn.isShiftKeyDown() || bits == 0)
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
        return state;
    }

    public Fluid getMyFluid() {
        return myFluid;
    }

    public int getBits() {
        return bits;
    }
}
