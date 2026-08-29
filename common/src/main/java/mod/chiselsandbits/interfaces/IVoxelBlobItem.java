package mod.chiselsandbits.interfaces;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;

public interface IVoxelBlobItem {

    void rotate(final ItemStack is, final Direction.Axis axis, final Rotation rotation);
}
