package mod.chiselsandbits.crafting;

import java.util.ArrayList;
import java.util.List;
import mod.chiselsandbits.api.StateCount;
import mod.chiselsandbits.bitbag.BagInventory;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.items.ItemBitBag;
import mod.chiselsandbits.items.ItemChiseledBit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

class ChiselCraftingRequirements {
    final ItemStack pattern;
    final ItemStack[] pile;
    private final VoxelBlob voxelBlob;
    private final ArrayList<ItemStack> stacks = new ArrayList<ItemStack>();
    private final ArrayList<BagInventory> bags = new ArrayList<BagInventory>();
    private Boolean isValid = null;

    public ChiselCraftingRequirements(final CraftingInput inv, final ItemStack inPattern, final boolean copy) {
        pile = new ItemStack[inv.size()];
        pattern = inPattern;

        for (int x = 0; x < inv.size(); x++) {
            final ItemStack source = inv.getItem(x);
            final ItemStack is = source.copy();
            pile[x] = is;

            if (is == null) {
                continue;
            }

            if (is.getItem() instanceof ItemBitBag) {
                bags.add(new BagInventory(copy ? is.copy() : is));
            }

            if (is.getItem() instanceof ItemChiseledBit) {
                stacks.add(copy ? is.copy() : is);
            }
        }

        voxelBlob = ModUtil.getBlobFromStack(inPattern, null);
    }

    public boolean isValid() {
        if (isValid != null) {
            return isValid;
        }

        final List<StateCount> count = voxelBlob.getStateCounts();

        isValid = true;
        for (final StateCount ref : count) {
            if (ref.stateId != 0) {

                for (final ItemStack is : stacks) {
                    if (ItemChiseledBit.getStackState(is) == ref.stateId && ModUtil.notEmpty(is)) {
                        final int original = ModUtil.getStackSize(is);
                        ModUtil.setStackSize(is, Math.max(0, ModUtil.getStackSize(is) - ref.quantity));
                        ref.quantity -= original - ModUtil.getStackSize(is);
                    }
                }

                for (final BagInventory bag : bags) {
                    ref.quantity -= bag.extractBit(ref.stateId, ref.quantity);
                }

                if (ref.quantity > 0) {
                    isValid = false;
                    break;
                }
            }
        }
        return isValid;
    }
}
