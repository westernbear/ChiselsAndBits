package mod.chiselsandbits.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockExtension {
    BlockState rotate(
            final BlockState state,
            final LevelAccessor world,
            final BlockPos pos,
            final Direction axis,
            final Rotation rotation);
}
