package mod.chiselsandbits.chiseledblock.data;

import java.util.Collection;
import mod.chiselsandbits.api.BoxType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Calculates the block shape of a VoxelBlob.
 * <p>
 * Thanks to Daniel from C&B2.
 */
public class VoxelShapeCalculator {
    /**
     * Calculates both the selection shape and the collision shape for a voxel blob.
     */
    public static VoxelShape calculate(final VoxelBlob blob, final BoxType type) {
        final VoxelBlobStateReference reference = new VoxelBlobStateReference(blob, 0L);
        return calculateFromBB(reference.getBoxes(type));
    }

    private static VoxelShape calculateFromBB(final Collection<AABB> bbList) {
        return bbList.stream()
                .reduce(
                        Shapes.empty(),
                        (voxelShape, axisAlignedBB) -> {
                            final VoxelShape bbShape = Shapes.create(axisAlignedBB);
                            return Shapes.joinUnoptimized(voxelShape, bbShape, BooleanOp.OR);
                        },
                        (voxelShape, voxelShape2) -> Shapes.joinUnoptimized(voxelShape, voxelShape2, BooleanOp.OR))
                .optimize();
    }
}
