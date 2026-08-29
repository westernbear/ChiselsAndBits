package mod.chiselsandbits.chiseledblock.iterators;

import mod.chiselsandbits.chiseledblock.data.IntegerBox;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public interface ChiselIterator {

    IntegerBox getVoxelBox(VoxelBlob blobAt, boolean b);

    AABB getBoundingBox(VoxelBlob nULL_BLOB, boolean b);

    boolean hasNext();

    Direction side();

    int x();

    int y();

    int z();
}
