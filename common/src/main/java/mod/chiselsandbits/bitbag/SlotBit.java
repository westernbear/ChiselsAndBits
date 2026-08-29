package mod.chiselsandbits.bitbag;

import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.items.ItemChiseledBit;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotBit extends Slot {

    public SlotBit(final Container inventoryIn, final int index, final int xPosition, final int yPosition) {
        super(inventoryIn, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return ModUtil.notEmpty(stack) && stack.getItem() instanceof ItemChiseledBit;
    }
}
