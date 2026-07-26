package mod.chiselsandbits.helpers;

import java.util.ArrayList;
import java.util.List;
import mod.chiselsandbits.bitbag.BagInventory;
import mod.chiselsandbits.items.ItemBitBag;
import mod.chiselsandbits.items.ItemChiseledBit;
import mod.chiselsandbits.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class ContinousBits implements IContinuousInventory {
    final int stateID;
    private final List<IItemInInventory> options = new ArrayList<IItemInInventory>();
    private final List<BagInventory> bags = new ArrayList<BagInventory>();

    public ContinousBits(final ActingPlayer src, final BlockPos pos, final int stateID) {
        this.stateID = stateID;
        final Container inv = src.getInventory();

        // test can edit...
        final boolean canEdit =
                src.canPlayerManipulate(pos, Direction.UP, new ItemStack(ModItems.ITEM_CHISEL_DIAMOND.get(), 1), true);

        ItemStackSlot handSlot = null;

        for (int zz = 0; zz < inv.getContainerSize(); zz++) {
            final ItemStack which = inv.getItem(zz);
            if (which != null && which.getItem() != null) {
                if (which.getItem() instanceof ItemChiseledBit) {
                    if (ItemChiseledBit.getStackState(which) == stateID) {
                        if (zz == src.getCurrentItem()) {
                            handSlot = new ItemStackSlot(inv, zz, which, src, canEdit);
                        } else {
                            options.add(new ItemStackSlot(inv, zz, which, src, canEdit));
                        }
                    }
                } else if (which.getItem() instanceof ItemBitBag) {
                    bags.add(new BagInventory(which));
                }
            }
        }

        if (handSlot != null) {
            options.add(handSlot);
        }
    }

    @Override
    public IItemInInventory getItem(final int BlockID) {
        return options.get(0);
    }

    @Override
    public boolean useItem(final int blk) {
        final IItemInInventory slot = options.get(0);

        if (slot instanceof ItemStackSlot && ModUtil.getStackSize(slot.getStack()) <= 1) {
            for (final BagInventory bag : bags) {
                ((ItemStackSlot) slot).replaceStack(bag.restockItem(slot.getStack(), slot.getStackType()));
            }
        }

        boolean worked = slot.consume();

        if (slot.isValid()) {
            if (slot instanceof ItemStackSlot) {
                for (final BagInventory bag : bags) {
                    ((ItemStackSlot) slot).replaceStack(bag.restockItem(slot.getStack(), slot.getStackType()));
                }
            }
        } else {
            options.remove(0);
        }

        return worked;
    }

    @Override
    public void fail(final int BlockID) {
        // hmm.. nope?
    }

    @Override
    public boolean isValid() {
        return !options.isEmpty();
    }
}
