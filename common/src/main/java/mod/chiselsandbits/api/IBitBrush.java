package mod.chiselsandbits.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Do not implement, acquire from {@link IChiselAndBitsAPI}
 */
public interface IBitBrush {

    /**
     * @return true when the brush is air...
     */
    boolean isAir();

    /**
     * Gets the corresponding block state.
     *
     * @return BlockState of brush, null for air.
     */
    @Nullable
    BlockState getState();

    /**
     * Get the ItemStack for a bit, which is empty for air.
     * <p>
     * VERY IMPORTANT: C&B lets you disable bits, if this happens the Item in
     * this ItemStack WILL BE NULL, if you put this ItemStack in an inventory, drop
     * it on the ground, or anything else.. CHECK THIS!!!!!
     *
     * @param count
     * @return ItemStack, which is empty for air.
     */
    ItemStack getItemStack(int count);

    /**
     * @return the state id for the {@link BlockState}
     */
    int getStateID();
}
