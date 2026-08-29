package mod.chiselsandbits.interfaces;

import net.minecraft.world.item.ItemStack;

public interface IPatternItem {

    ItemStack getPatternedItem(ItemStack stack, final boolean wantRealBlocks);

    boolean isWritten(ItemStack stack);
}
