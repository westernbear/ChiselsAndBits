package mod.chiselsandbits.api;

import java.util.Collection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

/**
 * This interface is implemented by Chiseled Block Tile Entities.
 */
public interface IChiseledBlockTileEntity {

    /**
     * Used to access the contents of the chiseled block tile entity, context
     * about world will be derived from the world on the tile entity.
     * <p>
     * Note: that invalid world / position data will prevent some operations
     * such as caving changes.
     *
     * @return {@link IBitAccess} for the tile entity.
     */
    IBitAccess getBitAccess();

    /**
     * Used to write Tile Data into cross world format, can be invoked via
     * interface or via reflection on the tile itself.
     * <p>
     * functions identically to writeToNBT(...)
     *
     * @param tag
     * @param crossWorld
     * @return modified input tag.
     */
    CompoundTag writeTileEntityToTag(final CompoundTag tag, final boolean crossWorld);

    /**
     * Used for access to the collision, occlusion, and swimming boxes of the
     * chiseled block tile entity.
     *
     * @param type the type of boxes to return
     * @return a collection of the boxes for the tile entity
     */
    Collection<AABB> getBoxes(final BoxType type);
}
