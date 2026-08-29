package mod.chiselsandbits.client.culling;

import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.helpers.ModUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Determine Culling using Block's Native Check.
 * <p>
 * hardcode vanilla stained glass because that looks horrible.
 */
public class MCCullTest implements ICullTest, BlockGetter {

    private BlockState a;
    private BlockState b;

    @Override
    public boolean isVisible(final int mySpot, final int secondSpot) {
        if (mySpot == 0 || mySpot == secondSpot) {
            return false;
        }

        a = ModUtil.getStateById(mySpot);
        if (a == null) {
            a = Blocks.AIR.defaultBlockState();
        }
        b = ModUtil.getStateById(secondSpot);
        if (b == null) {
            b = Blocks.AIR.defaultBlockState();
        }

        if (a.getBlock().getClass() == StainedGlassBlock.class && a.getBlock() == b.getBlock()) {
            return false;
        }

        if (a.getBlock() instanceof LiquidBlock) {
            return true;
        }

        try {
            return !a.skipRendering(b, Direction.NORTH);
        } catch (final Throwable t) {
            // revert to older logic in the event of some sort of issue.
            return BlockBitInfo.getTypeFromStateID(mySpot).shouldShow(BlockBitInfo.getTypeFromStateID(secondSpot));
        }
    }

    public boolean isVisible(final int mySpot, final int secondSpot, Direction face) {
        if (mySpot == 0 || mySpot == secondSpot) {
            return false;
        }

        a = ModUtil.getStateById(mySpot);
        if (a == null) {
            a = Blocks.AIR.defaultBlockState();
        }
        b = ModUtil.getStateById(secondSpot);
        if (b == null) {
            b = Blocks.AIR.defaultBlockState();
        }
        if (!a.skipRendering(b, face)) {
            return false;
        }

        if (a.getBlock().getClass() == StainedGlassBlock.class && a.getBlock() == b.getBlock()) {
            return false;
        }

        if (a.getBlock() instanceof LiquidBlock) {
            return true;
        }

        try {
            return !a.skipRendering(b, Direction.NORTH);
        } catch (final Throwable t) {
            // revert to older logic in the event of some sort of issue.
            return BlockBitInfo.getTypeFromStateID(mySpot).shouldShow(BlockBitInfo.getTypeFromStateID(secondSpot));
        }
    }

    @Override
    public BlockEntity getBlockEntity(final BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(final BlockPos pos) {
        return pos.equals(BlockPos.ZERO) ? a : b;
    }

    @Override
    public FluidState getFluidState(final BlockPos pos) {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }
}
