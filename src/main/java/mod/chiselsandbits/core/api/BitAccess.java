package mod.chiselsandbits.core.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mod.chiselsandbits.api.APIExceptions.SpaceOccupied;
import mod.chiselsandbits.api.BitQueryResults;
import mod.chiselsandbits.api.IBitAccess;
import mod.chiselsandbits.api.IBitBrush;
import mod.chiselsandbits.api.IBitVisitor;
import mod.chiselsandbits.api.ItemType;
import mod.chiselsandbits.api.StateCount;
import mod.chiselsandbits.api.VoxelStats;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.BlockChiseled.ReplaceWithChiseledValue;
import mod.chiselsandbits.chiseledblock.ItemBlockChiseled;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.BitIterator;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.data.VoxelBlobStateReference;
import mod.chiselsandbits.client.UndoTracker;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

public class BitAccess implements IBitAccess {

    private final Level world;
    private final BlockPos pos;
    private final VoxelBlob blob;
    private final VoxelBlob filler;

    private final Map<Integer, IBitBrush> brushes = new HashMap<Integer, IBitBrush>();

    public BitAccess(final Level worldIn, final BlockPos pos, final VoxelBlob blob, final VoxelBlob filler) {
        world = worldIn;
        this.pos = pos;
        this.blob = blob;
        this.filler = filler;
    }

    public VoxelBlob getNativeBlob() {
        return blob;
    }

    @Override
    public IBitBrush getBitAt(final int x, final int y, final int z) {
        return getBrushForState(blob.getSafe(x, y, z));
    }

    private IBitBrush getBrushForState(final int state) {
        IBitBrush brush = brushes.get(state);

        if (brush == null) {
            brushes.put(state, brush = new BitBrush(state));
        }

        return brush;
    }

    @Override
    public void setBitAt(int x, int y, int z, final IBitBrush bit) throws SpaceOccupied {
        int state = 0;

        if (bit instanceof BitBrush) {
            state = bit.getStateID();
        }

        // make sure that they are only 0-15
        x = x & 0xf;
        y = y & 0xf;
        z = z & 0xf;

        if (filler.get(x, y, z) == 0) {
            blob.set(x, y, z, state);
        } else {
            throw new SpaceOccupied();
        }
    }

    @Override
    public void commitChanges(final boolean triggerUpdates) {
        final Level w = world;
        final BlockPos p = pos;

        if (w != null && p != null) {
            TileEntityBlockChiseled tile = ModUtil.getChiseledTileEntity(w, p, true);
            final VoxelStats cb = blob.getVoxelStats();
            ReplaceWithChiseledValue rv = null;

            if (tile == null
                    && (rv = BlockChiseled.replaceWithChiseled(w, p, world.getBlockState(p), cb.mostCommonState, false))
                            .success) {
                tile = rv.te;
            }

            if (tile != null) {
                final VoxelBlobStateReference before = tile.getBlobStateReference();
                tile.setBlob(blob, triggerUpdates);
                final VoxelBlobStateReference after = tile.getBlobStateReference();

                UndoTracker.getInstance().add(w, p, before, after);
            }
        }
    }

    @Override
    public ItemStack getBitsAsItem(
            final @Nullable Direction side, final @Nullable ItemType type, final boolean crossWorld) {
        if (type == null) {
            return ModUtil.getEmptyStack();
        }

        final VoxelStats cb = blob.getVoxelStats();
        if (cb.mostCommonState == 0) {
            return ModUtil.getEmptyStack();
        }

        final NBTBlobConverter c = new NBTBlobConverter();
        c.setBlob(blob);

        final CompoundTag nbttagcompound = new CompoundTag();
        c.writeChisleData(nbttagcompound, crossWorld);

        final ItemStack stack;

        if (type == ItemType.CHISELED_BLOCK) {
            final BlockChiseled blk = ModBlocks.getChiseledBlock();

            if (blk == null) {
                return ModUtil.getEmptyStack();
            }

            ItemBlockChiseled item = ModItems.ITEM_CHISELED_BLOCK.get();

            stack = new ItemStack(item, 1);
            stack.addTagElement(ModUtil.NBT_BLOCKENTITYTAG, nbttagcompound);
        } else {
            switch (type) {
                case MIRROR_DESIGN:
                    stack = ModUtil.makeStack(ModItems.ITEM_MIRROR_PRINT_WRITTEN.get());
                    break;
                case NEGATIVE_DESIGN:
                    stack = ModUtil.makeStack(ModItems.ITEM_NEGATIVE_PRINT_WRITTEN.get());
                    break;
                case POSITIVE_DESIGN:
                    stack = ModUtil.makeStack(ModItems.ITEM_POSITIVE_PRINT_WRITTEN.get());
                    break;
                default:
                    return ModUtil.getEmptyStack();
            }

            stack.setTag(nbttagcompound);
        }

        if (side != null) {
            ModUtil.setSide(stack, side);
        }

        return stack;
    }

    @Override
    public void visitBits(final IBitVisitor visitor) {
        final BitIterator bi = new BitIterator();
        IBitBrush brush = getBrushForState(0);
        while (bi.hasNext()) {
            if (bi.getNext(filler) == 0) {
                final int stateID = bi.getNext(blob);

                // Most blocks are mostly the same bit type, so if it dosn't
                // change just keep the current brush, only if they differ
                // should we bother looking it up again.
                if (stateID != brush.getStateID()) {
                    brush = getBrushForState(stateID);
                }

                final IBitBrush after = visitor.visitBit(bi.x, bi.y, bi.z, brush);

                if (brush != after) {
                    if (after == null) {
                        bi.setNext(blob, 0);
                    } else {
                        bi.setNext(blob, after.getStateID());
                    }
                }
            }
        }
    }

    @Override
    public BitQueryResults queryBitRange(final BlockPos a, final BlockPos b) {
        int air = 0, fluid = 0, solid = 0;

        final BitIterator bi = new BitIterator(a, b);

        while (bi.hasNext()) {
            final int state = bi.getNext(blob);

            if (state == 0) {
                ++air;
            } else if (VoxelBlob.isFluid(state)) {
                ++fluid;
            } else {
                ++solid;
            }
        }

        return new BitQueryResults(air, solid, fluid);
    }

    @Override
    public boolean mirror(final Direction.Axis axis) {
        VoxelBlob blobMirrored = blob.mirror(axis);
        if (filler.canMerge(blobMirrored)) {
            blob.fill(blobMirrored);
            return true;
        }
        return false;
    }

    @Override
    public boolean rotate(final Direction.Axis axis, final Rotation rotation) {
        VoxelBlob blobRotated = ModUtil.rotate(blob, axis, rotation);
        if (blobRotated != null && filler.canMerge(blobRotated)) {
            blob.fill(blobRotated);
            return true;
        }
        return false;
    }

    @Override
    public List<StateCount> getStateCounts() {
        return blob.getStateCounts();
    }

    @Override
    public VoxelStats getVoxelStats() {
        return blob.getVoxelStats();
    }
}
