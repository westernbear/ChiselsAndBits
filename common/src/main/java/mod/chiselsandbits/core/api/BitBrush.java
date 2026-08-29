package mod.chiselsandbits.core.api;

import mod.chiselsandbits.api.IBitBrush;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.items.ItemChiseledBit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BitBrush implements IBitBrush {

    protected final int stateID;

    public BitBrush(final int blockStateID) {
        stateID = blockStateID;
    }

    @Override
    public ItemStack getItemStack(final int count) {
        if (stateID == 0) {
            return ModUtil.getEmptyStack();
        }

        return ItemChiseledBit.createStack(stateID, count, true);
    }

    @Override
    public boolean isAir() {
        return stateID == 0;
    }

    @Override
    public @Nullable BlockState getState() {
        if (stateID == 0) {
            return null;
        }

        return ModUtil.getStateById(stateID);
    }

    @Override
    public int getStateID() {
        return stateID;
    }
}
