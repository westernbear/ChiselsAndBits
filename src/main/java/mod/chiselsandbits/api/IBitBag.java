package mod.chiselsandbits.api;

import net.minecraft.world.item.ItemStack;

public interface IBitBag {

    int getSlots();

    ItemStack getStackInSlot(int slot);

    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    boolean isItemValid(int slot, ItemStack stack);

    /**
     * @return get max stack size of bits inside the bag.
     */
    int getBitbagStackSize();

    /**
     * @return how many slots contain bits.
     */
    int getSlotsUsed();
}
