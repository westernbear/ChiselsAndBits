package mod.chiselsandbits.printer;

import java.util.Objects;
import mod.chiselsandbits.bitstorage.TileEntityBitStorage;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.interfaces.IPatternItem;
import mod.chiselsandbits.items.ItemChisel;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModTileEntityTypes;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChiselPrinterTileEntity extends BlockEntity
        implements MenuProvider, WorldlyContainer, SidedStorageBlockEntity {

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
    public Storage<ItemVariant> getItemStorage(@Nullable final Direction side) {
        return InventoryStorage.of(this, side);
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        loadSlot(compoundTag, "pattern", PATTERN_SLOT);
        loadSlot(compoundTag, "tool", TOOL_SLOT);
        loadSlot(compoundTag, "result", RESULT_SLOT);
        progress = compoundTag.getInt("progress");
        currentRealisedWorkingStack.setValue(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        saveSlot(compoundTag, "pattern", PATTERN_SLOT);
        saveSlot(compoundTag, "tool", TOOL_SLOT);
        saveSlot(compoundTag, "result", RESULT_SLOT);
        compoundTag.putInt("progress", progress);
    }

    private void loadSlot(final CompoundTag compoundTag, final String key, final int slot) {
        final ListTag items = compoundTag.getCompound(key).getList("Items", Tag.TAG_COMPOUND);
        inventory.setItem(slot, items.isEmpty() ? ItemStack.EMPTY : ItemStack.of(items.getCompound(0)));
    }

    private void saveSlot(final CompoundTag compoundTag, final String key, final int slot) {
        final CompoundTag handler = new CompoundTag();
        handler.putInt("Size", 1);
        final ListTag items = new ListTag();
        final ItemStack stack = inventory.getItem(slot);
        if (!stack.isEmpty()) {
            items.add(stack.save(new CompoundTag()));
        }
        handler.put("Items", items);
        compoundTag.put(key, handler);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithFullMetadata();
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
        return ItemStack.isSameItemSameTags(output, realised)
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
        final CompoundTag tag = ModUtil.getSubCompound(realisedPattern, ModUtil.NBT_BLOCKENTITYTAG, false)
                .copy();
        c.readChisleData(tag, VoxelBlob.VERSION_ANY);
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
        c.writeChisleData(tag, false);

        itemstack.addTagElement(ModUtil.NBT_BLOCKENTITYTAG, tag);
        return itemstack;
    }

    void damageChisel() {
        if (getLevel() != null && !getLevel().isClientSide()) {
            getToolStack().hurt(1, getLevel().getRandom(), null);
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
