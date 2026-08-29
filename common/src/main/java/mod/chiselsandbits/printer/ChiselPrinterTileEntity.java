package mod.chiselsandbits.printer;

import java.util.Objects;
import mod.chiselsandbits.bitstorage.TileEntityBitStorage;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.components.ChiseledData;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.interfaces.IPatternItem;
import mod.chiselsandbits.items.ItemChisel;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModTileEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChiselPrinterTileEntity extends BlockEntity implements MenuProvider, WorldlyContainer {

    private static final int PATTERN_SLOT = 0;
    private static final int TOOL_SLOT = 1;
    private static final int RESULT_SLOT = 2;
    private static final int[] RESULT_SLOTS = {RESULT_SLOT};
    private static final int[] TOOL_SLOTS = {TOOL_SLOT};

    final MutableObject<ItemStack> currentRealisedWorkingStack = new MutableObject<>(ItemStack.EMPTY);
    private final SimpleContainer inventory = new SimpleContainer(3);
    int progress = 0;
    protected final ContainerData stationData = new ContainerData() {
        public int get(int index) {
            if (index == 0) {
                return ChiselPrinterTileEntity.this.progress;
            }
            return 0;
        }

        public void set(int index, int value) {
            if (index == 0) {
                ChiselPrinterTileEntity.this.progress = value;
            }
        }

        public int getCount() {
            return 1;
        }
    };
    long lastTickTime = 0L;

    public ChiselPrinterTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntityTypes.CHISEL_PRINTER.get(), pos, state);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        loadSlot(input, "pattern", PATTERN_SLOT);
        loadSlot(input, "tool", TOOL_SLOT);
        loadSlot(input, "result", RESULT_SLOT);
        progress = input.getIntOr("progress", 0);
        currentRealisedWorkingStack.setValue(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        saveSlot(output, "pattern", PATTERN_SLOT);
        saveSlot(output, "tool", TOOL_SLOT);
        saveSlot(output, "result", RESULT_SLOT);
        output.putInt("progress", progress);
    }

    private void loadSlot(final ValueInput input, final String key, final int slot) {
        inventory.setItem(slot, input.read(key, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    private void saveSlot(final ValueOutput output, final String key, final int slot) {
        output.store(key, ItemStack.OPTIONAL_CODEC, inventory.getItem(slot));
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return saveWithFullMetadata(registries);
    }

    public boolean hasPatternStack() {
        return !getPatternStack().isEmpty();
    }

    public boolean hasToolStack() {
        return !getToolStack().isEmpty();
    }

    public boolean hasRealisedStack() {
        return !getRealisedStack().isEmpty();
    }

    public boolean hasOutputStack() {
        return !getOutputStack().isEmpty();
    }

    public boolean canMergeOutputs() {
        if (!hasOutputStack()) {
            return true;
        }

        if (!hasRealisedStack()) {
            return false;
        }

        final ItemStack output = getOutputStack();
        final ItemStack realised = getRealisedStack();
        return ItemStack.isSameItemSameComponents(output, realised)
                && output.getCount() + realised.getCount() <= output.getMaxStackSize();
    }

    public boolean canWork() {
        return hasPatternStack() && hasToolStack() && canMergeOutputs();
    }

    public boolean couldWork() {
        return hasPatternStack() && hasToolStack();
    }

    public ItemStack getPatternStack() {
        return inventory.getItem(PATTERN_SLOT);
    }

    public ItemStack getToolStack() {
        return inventory.getItem(TOOL_SLOT);
    }

    public ItemStack getRealisedStack() {
        ItemStack realisedStack = currentRealisedWorkingStack.getValue();
        if (realisedStack.isEmpty()) {
            realisedStack = realisePattern(false);
            currentRealisedWorkingStack.setValue(realisedStack);
        }

        return realisedStack;
    }

    public ItemStack getOutputStack() {
        return inventory.getItem(RESULT_SLOT);
    }

    public void addOutput(final ItemStack stack) {
        if (getOutputStack().isEmpty()) {
            inventory.setItem(RESULT_SLOT, stack);
        } else {
            getOutputStack().grow(stack.getCount());
        }
        setChanged();
    }

    public ItemStack realisePattern(final boolean consumeResources) {
        if (!hasPatternStack()) {
            return ItemStack.EMPTY;
        }

        final ItemStack stack = getPatternStack();
        if (!(stack.getItem() instanceof IPatternItem patternItem)) {
            return ItemStack.EMPTY;
        }

        final ItemStack realisedPattern = patternItem.getPatternedItem(stack.copy(), true);
        if (realisedPattern == null || realisedPattern.isEmpty()) {
            return ItemStack.EMPTY;
        }

        BlockState firstState = getPrimaryBlockState();
        BlockState secondState = getSecondaryBlockState();
        BlockState thirdState = getTertiaryBlockState();

        if (firstState == null) {
            firstState = Blocks.AIR.defaultBlockState();
        }

        if (secondState == null) {
            secondState = Blocks.AIR.defaultBlockState();
        }

        if (thirdState == null) {
            thirdState = Blocks.AIR.defaultBlockState();
        }

        if ((!BlockBitInfo.isSupported(firstState) && !firstState.isAir())
                || (!BlockBitInfo.isSupported(secondState) && !secondState.isAir())
                || (!BlockBitInfo.isSupported(thirdState) && !thirdState.isAir())) {
            return ItemStack.EMPTY;
        }

        final NBTBlobConverter c = new NBTBlobConverter();
        final ChiseledData data = NBTBlobConverter.getComponent(realisedPattern);
        if (data == null) {
            return ItemStack.EMPTY;
        }
        c.readChisleData(data, VoxelBlob.VERSION_ANY);
        VoxelBlob blob = c.getBlob();

        final VoxelBlob.PartialFillResult fillResult = blob.clearAllBut(
                ModUtil.getStateId(firstState), ModUtil.getStateId(secondState), ModUtil.getStateId(thirdState));

        if (fillResult.getFirstStateUsedCount() == 0
                && fillResult.getSecondStateUsedCount() == 0
                && fillResult.getThirdStateUsedCount() == 0) {
            return ItemStack.EMPTY;
        }

        if (fillResult.getFirstStateUsedCount() > getAvailablePrimaryBlockState()
                || fillResult.getSecondStateUsedCount() > getAvailableSecondaryBlockState()
                || fillResult.getThirdStateUsedCount() > getAvailableTertiaryBlockState()) {
            return ItemStack.EMPTY;
        }

        if (consumeResources) {
            drainPrimaryStorage(fillResult.getFirstStateUsedCount());
            drainSecondaryStorage(fillResult.getSecondStateUsedCount());
            drainTertiaryStorage(fillResult.getThirdStateUsedCount());
        }

        c.setBlob(blob);

        final ItemStack itemstack = new ItemStack(ModBlocks.getChiseledBlock(), 1);
        c.writeToStack(itemstack, false);
        ModUtil.setSide(itemstack, ModUtil.getSide(realisedPattern));
        return itemstack;
    }

    void damageChisel() {
        if (getLevel() != null && !getLevel().isClientSide()) {
            ModUtil.damageItem(getToolStack(), getLevel().getRandom());
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            final int containerId, @NotNull final Inventory playerInventory, @NotNull final Player playerEntity) {
        return new ChiselPrinterContainer(containerId, playerInventory, this, stationData);
    }

    @Override
    public Component getDisplayName() {
        return LocalStrings.ChiselStationName.getLocalText();
    }

    public int getAvailablePrimaryBlockState() {
        final Direction facing = Objects.requireNonNull(this.getLevel())
                .getBlockState(this.getBlockPos())
                .getValue(ChiselPrinterBlock.FACING);
        final Direction targetedFacing = facing.getClockWise();

        return getStorageContents(targetedFacing);
    }

    public int getAvailableSecondaryBlockState() {
        final Direction facing = Objects.requireNonNull(this.getLevel())
                .getBlockState(this.getBlockPos())
                .getValue(ChiselPrinterBlock.FACING);
        final Direction targetedFacing = facing.getClockWise().getClockWise();

        return getStorageContents(targetedFacing);
    }

    public int getAvailableTertiaryBlockState() {
        final Direction facing = Objects.requireNonNull(this.getLevel())
                .getBlockState(this.getBlockPos())
                .getValue(ChiselPrinterBlock.FACING);
        final Direction targetedFacing = facing.getCounterClockWise();

        return getStorageContents(targetedFacing);
    }

    private int getStorageContents(final Direction targetedFacing) {
        final BlockEntity targetedTileEntity =
                this.getLevel().getBlockEntity(this.getBlockPos().relative(targetedFacing));
        if (targetedTileEntity instanceof TileEntityBitStorage storage) {
            return storage.getBits();
        }

        return 0;
    }

    public BlockState getPrimaryBlockState() {
        final Direction facing = Objects.requireNonNull(this.getLevel())
                .getBlockState(this.getBlockPos())
                .getValue(ChiselPrinterBlock.FACING);
        final Direction targetedFacing = facing.getClockWise();

        return getStorage(targetedFacing);
    }

    public BlockState getSecondaryBlockState() {
        final Direction facing = Objects.requireNonNull(this.getLevel())
                .getBlockState(this.getBlockPos())
                .getValue(ChiselPrinterBlock.FACING);
        final Direction targetedFacing = facing.getClockWise().getClockWise();

        return getStorage(targetedFacing);
    }

    public BlockState getTertiaryBlockState() {
        final Direction facing = Objects.requireNonNull(this.getLevel())
                .getBlockState(this.getBlockPos())
                .getValue(ChiselPrinterBlock.FACING);
        final Direction targetedFacing = facing.getCounterClockWise();

        return getStorage(targetedFacing);
    }

    private BlockState getStorage(final Direction targetedFacing) {
        final BlockEntity targetedTileEntity =
                this.getLevel().getBlockEntity(this.getBlockPos().relative(targetedFacing));
        if (targetedTileEntity instanceof TileEntityBitStorage storage) {
            return storage.getState();
        }

        return Blocks.AIR.defaultBlockState();
    }

    public void drainPrimaryStorage(final int amount) {
        final Direction facing = Objects.requireNonNull(this.getLevel())
                .getBlockState(this.getBlockPos())
                .getValue(ChiselPrinterBlock.FACING);
        final Direction targetedFacing = facing.getClockWise();

        drainStorage(amount, targetedFacing);
    }

    public void drainSecondaryStorage(final int amount) {
        final Direction facing = Objects.requireNonNull(this.getLevel())
                .getBlockState(this.getBlockPos())
                .getValue(ChiselPrinterBlock.FACING);
        final Direction targetedFacing = facing.getClockWise().getClockWise();

        drainStorage(amount, targetedFacing);
    }

    public void drainTertiaryStorage(final int amount) {
        final Direction facing = Objects.requireNonNull(this.getLevel())
                .getBlockState(this.getBlockPos())
                .getValue(ChiselPrinterBlock.FACING);
        final Direction targetedFacing = facing.getCounterClockWise();

        drainStorage(amount, targetedFacing);
    }

    private void drainStorage(final int amount, final Direction targetedFacing) {
        final BlockEntity targetedTileEntity =
                this.getLevel().getBlockEntity(this.getBlockPos().relative(targetedFacing));
        if (targetedTileEntity instanceof TileEntityBitStorage storage) {
            storage.extractBits(0, amount, false);
        }
    }

    @Override
    public int[] getSlotsForFace(final Direction side) {
        return side == Direction.DOWN ? RESULT_SLOTS : TOOL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack, final Direction side) {
        return slot == RESULT_SLOT && side == Direction.DOWN
                || slot == TOOL_SLOT && side != Direction.DOWN && stack.getItem() instanceof ItemChisel;
    }

    @Override
    public boolean canTakeItemThroughFace(final int slot, final ItemStack stack, final Direction side) {
        return slot == RESULT_SLOT && side == Direction.DOWN || slot == TOOL_SLOT && side != Direction.DOWN;
    }

    @Override
    public int getContainerSize() {
        return inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(final int slot) {
        return inventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        final ItemStack removed = inventory.removeItem(slot, amount);
        if (!removed.isEmpty()) {
            inventoryChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        return inventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        inventory.setItem(slot, stack);
        inventoryChanged();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return switch (slot) {
            case PATTERN_SLOT -> stack.getItem() instanceof IPatternItem;
            case TOOL_SLOT -> stack.getItem() instanceof ItemChisel;
            case RESULT_SLOT -> true;
            default -> false;
        };
    }

    private void inventoryChanged() {
        currentRealisedWorkingStack.setValue(ItemStack.EMPTY);
        super.setChanged();
    }

    @Override
    public boolean stillValid(final Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
        inventoryChanged();
    }
}
