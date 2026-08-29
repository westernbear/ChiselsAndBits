package mod.chiselsandbits.helpers;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemStackSlot implements IItemInInventory {
    private final Container inv;
    private final int slot;
    private final @NotNull ItemStack originalStack;
    private final boolean isCreative;
    private final boolean isEditable;
    private final int toolSlot;
    private @NotNull ItemStack stack;

    ItemStackSlot(
            final Container i,
            final int s,
            final @NotNull ItemStack st,
            final ActingPlayer player,
            final boolean canEdit) {
        inv = i;
        slot = s;
        stack = st;
        originalStack = ModUtil.copy(st);
        toolSlot = player.getCurrentItem();
        isCreative = player.isCreative();
        isEditable = canEdit;
    }

    @Override
    public boolean isValid() {
        return isEditable && (isCreative || !ModUtil.isEmpty(stack) && ModUtil.getStackSize(stack) > 0);
    }

    @Override
    public void damage(final ActingPlayer who) {
        if (isCreative) {
            return;
        }

        who.damageItem(stack, 1);
        if (ModUtil.getStackSize(stack) <= 0) {
            who.playerDestroyItem(stack, who.getHand());
            inv.setItem(slot, ModUtil.getEmptyStack());
        }
    }

    @Override
    public boolean consume() {
        if (isCreative) {
            return true;
        }

        if (ModUtil.getStackSize(stack) > 0) {
            ModUtil.adjustStackSize(stack, -1);
            if (ModUtil.getStackSize(stack) <= 0) {
                inv.setItem(slot, ModUtil.getEmptyStack());
            }

            return true;
        }

        return false;
    }

    @Override
    public ItemStack getStack() {
        return stack;
    }

    @Override
    public void swapWithWeapon() {
        final ItemStack it = inv.getItem(toolSlot);
        inv.setItem(toolSlot, inv.getItem(slot));
        inv.setItem(slot, it);
    }

    @Override
    public ItemStack getStackType() {
        return originalStack;
    }

    public void replaceStack(final @NotNull ItemStack restockItem) {
        stack = restockItem;
        inv.setItem(slot, restockItem);
    }
}
