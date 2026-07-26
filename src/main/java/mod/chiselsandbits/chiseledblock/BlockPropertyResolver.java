package mod.chiselsandbits.chiseledblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves block properties that vanilla 26.2 reduced to context-free accessors.
 *
 * <p>Chiseled blocks still derive these properties from their primary contained block state, so vanilla caller
 * mixins route through this class rather than silently falling back to the static properties of the container
 * block.
 */
public final class BlockPropertyResolver {

    private BlockPropertyResolver() {}

    public static SoundType resolveSoundType(
            final BlockState state, final LevelReader level, final BlockPos pos, final @Nullable Entity entity) {
        if (state.getBlock() instanceof BlockChiseled chiseledBlock) {
            return chiseledBlock.getSoundType(state, level, pos, entity);
        }

        return state.getSoundType();
    }

    public static float resolveFriction(
            final BlockState state, final LevelReader level, final BlockPos pos, final @Nullable Entity entity) {
        if (state.getBlock() instanceof BlockChiseled chiseledBlock) {
            return chiseledBlock.getFriction(state, level, pos, entity);
        }

        return state.getBlock().getFriction();
    }
}
