package mod.chiselsandbits.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Do not implement, acquire from {@link IChiselAndBitsAPI}
 */
public interface IBitLocation {

    /**
     * Get block position that the bit is inside.
     *
     * @return Block Pos
     */
    BlockPos getBlockPos();

    /**
     * get Bit X coordinate.
     *
     * @return X coordinate
     */
    int getBitX();

    /**
     * get Bit Y coordinate.
     *
     * @return Y coordinate
     */
    int getBitY();

    /**
     * get Bit Z coordinate.
     *
     * @return Z coordinate
     */
    int getBitZ();

    /**
     * Offsets the bit location into the given direction by one.
     * Possibly moving it to a new block if need be.
     *
     * @param direction The direction.
     */
    IBitLocation offSet(final Direction direction);
}
