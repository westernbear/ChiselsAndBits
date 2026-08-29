package mod.chiselsandbits.chiseledblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;

public interface LegacyBlockEntityProperties {

    default BlockState mirror(LevelAccessor level, BlockPos pos, BlockState blockState, Mirror mirror) {
        return blockState;
    }
}
