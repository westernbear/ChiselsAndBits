package mod.chiselsandbits.helpers;

import java.util.Arrays;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerCopiedInventory implements Container {

    private Inventory logicBase;
    private ItemStack[] slots;

    public PlayerCopiedInventory(Inventory original) {
        logicBase = original;
        slots = new ItemStack[original.getContainerSize()];

        for (int x = 0; x < slots.length; ++x) {
            slots[x] = original.getItem(x);

            if (slots[x] != null) {
                slots[x] = slots[x].copy();
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return Arrays.stream(slots).allMatch(ItemStack::isEmpty);
    }

    @Override
    public boolean stillValid(final Player player) {
        return true;
    }

    @Override
    public int getContainerSize() {
        return slots.length;
    }

    @Override
    public ItemStack getItem(final int index) {
        return slots[index];
    }

    @Override
    public ItemStack removeItem(final int index, final int count) {
        if (slots[index] != null) {
            if (ModUtil.getStackSize(slots[index]) <= count) {
                return removeItemNoUpdate(index);
            } else {
                return slots[index].split(count);
            }
        }

        return ModUtil.getEmptyStack();
    }

    @Override
    public ItemStack removeItemNoUpdate(final int index) {
        final ItemStack r = slots[index];
        slots[index] = ModUtil.getEmptyStack();
        return r;
    }

    @Override
    public void setItem(final int index, final ItemStack stack) {
        slots[index] = stack;
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean canPlaceItem(final int index, final ItemStack stack) {
        return logicBase.canPlaceItem(index, stack);
    }

    @Override
    public void clearContent() {
        for (int x = 0; x < slots.length; ++x) {
            slots[x] = ModUtil.getEmptyStack();
        }
    }
}
