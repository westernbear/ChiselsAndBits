package mod.chiselsandbits.bitbag;

import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TargetedInventory implements Container {

    private Container src;

    public TargetedInventory() {
        src = null;
    }

    public void setInventory(final Container a) {
        src = a;
    }

    @Override
    public int getContainerSize() {
        return src.getContainerSize();
    }

    @Override
    public ItemStack getItem(final int index) {
        return src.getItem(index);
    }

    @Override
    public ItemStack removeItem(final int index, final int count) {
        return src.removeItem(index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(final int index) {
        return src.removeItemNoUpdate(index);
    }

    @Override
    public void setItem(final int index, final ItemStack stack) {
        src.setItem(index, stack);
    }

    @Override
    public int getMaxStackSize() {
        return src.getMaxStackSize();
    }

    @Override
    public void setChanged() {
        src.setChanged();
    }

    @Override
    public boolean stillValid(final Player player) {
        return src.stillValid(player);
    }

    @Override
    public void startOpen(final ContainerUser user) {
        src.startOpen(user);
    }

    @Override
    public void stopOpen(final ContainerUser user) {
        src.stopOpen(user);
    }

    @Override
    public boolean canPlaceItem(final int index, final ItemStack stack) {
        return src.canPlaceItem(index, stack);
    }

    @Override
    public void clearContent() {
        src.clearContent();
    }

    @Override
    public boolean isEmpty() {
        return src.isEmpty();
    }

    public Container getSrc() {
        return src;
    }
}
