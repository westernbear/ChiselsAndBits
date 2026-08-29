package mod.chiselsandbits.helpers;

import net.minecraft.world.item.ItemStack;

public interface IItemInInventory {

    boolean isValid();

    void damage(ActingPlayer who);

    boolean consume();

    ItemStack getStack();

    void swapWithWeapon();

    ItemStack getStackType();
}
