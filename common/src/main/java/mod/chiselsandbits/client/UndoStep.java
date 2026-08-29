package mod.chiselsandbits.client;

import mod.chiselsandbits.chiseledblock.data.VoxelBlobStateReference;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public class UndoStep {
    public final Identifier dimensionId;
    public final BlockPos pos;
    public VoxelBlobStateReference before;
    public VoxelBlobStateReference after;
    public UndoStep next = null; // groups form a linked chain.

    public UndoStep(
            final Identifier dimensionId,
            final BlockPos pos,
            final VoxelBlobStateReference before,
            final VoxelBlobStateReference after) {
        this.dimensionId = dimensionId;
        this.pos = pos;
        this.before = before != null ? before : new VoxelBlobStateReference(0, 0);
        this.after = after != null ? after : new VoxelBlobStateReference(0, 0);
    }

    public void onNetworkUpdate(final VoxelBlobStateReference beforeUpdate, final VoxelBlobStateReference afterUpdate) {
        if (this.before == beforeUpdate) {
            this.before = afterUpdate;
        }

        if (this.after == beforeUpdate) {
            this.after = afterUpdate;
        }
    }
}
