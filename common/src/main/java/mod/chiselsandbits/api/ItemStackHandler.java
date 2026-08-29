package mod.chiselsandbits.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public interface ItemStackHandler {

    void apply(BlockState state, ItemStack stack);
}
