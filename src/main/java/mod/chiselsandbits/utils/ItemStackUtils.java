package mod.chiselsandbits.utils;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public final class ItemStackUtils {

    private ItemStackUtils() {}

    public static ItemStack getContainerItem(ItemStack stack) {
        final ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
        return remainder == null ? ItemStack.EMPTY : remainder.create();
    }
}
