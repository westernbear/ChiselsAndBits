package mod.chiselsandbits.helpers;

import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NullInventory implements Container {

    final int size;

    public NullInventory(final int size) {
        this.size = size;
    }

    @Override
    public int getContainerSize() {
        return size;
    }

    @Override
    public ItemStack getItem(final int index) {
        return ModUtil.getEmptyStack();
    }

    @Override
    public ItemStack removeItem(final int index, final int count) {
        return ModUtil.getEmptyStack();
    }

    @Override
    public ItemStack removeItemNoUpdate(final int index) {
        return ModUtil.getEmptyStack();
    }

    @Override
    public void setItem(final int index, final ItemStack stack) {}

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(final Player player) {
        return false;
    }

    @Override
    public void startOpen(final ContainerUser player) {}

    @Override
    public void stopOpen(final ContainerUser player) {}

    @Override
    public boolean canPlaceItem(final int index, final ItemStack stack) {
        return false;
    }

    @Override
    public void clearContent() {}

    @Override
    public boolean isEmpty() {
        return true;
    }
}
