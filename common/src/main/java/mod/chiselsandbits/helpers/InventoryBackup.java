package mod.chiselsandbits.helpers;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class InventoryBackup {

    Container original;
    ItemStack[] slots;

    public InventoryBackup(final Container inventory) {
        original = inventory;
        slots = new ItemStack[original.getContainerSize()];

        for (int x = 0; x < slots.length; ++x) {
            slots[x] = original.getItem(x);

            if (slots[x] != null) {
                slots[x] = slots[x].copy();
            }
        }
    }

    public void rollback() {
        for (int x = 0; x < slots.length; ++x) {
            original.setItem(x, slots[x]);
        }
    }
}
