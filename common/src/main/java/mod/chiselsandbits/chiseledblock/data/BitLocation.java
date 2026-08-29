package mod.chiselsandbits.chiseledblock.data;

import mod.chiselsandbits.api.IBitLocation;
import mod.chiselsandbits.helpers.BitOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class BitLocation implements IBitLocation {
    private static final double One32nd = 0.5 / VoxelBlob.dim;

    @NotNull
    public BlockPos blockPos;

    public int bitX, bitY, bitZ;

    public BitLocation(final BlockHitResult mop, final BitOperation type) {
        final Vec3 hitVec = mop.getLocation();
        final Vec3 accuratePos = new Vec3(
                mop.getBlockPos().getX(),
                mop.getBlockPos().getY(),
                mop.getBlockPos().getZ());
        final Vec3 faceOffset = new Vec3(
                mop.getDirection().getOpposite().getStepX() * One32nd,
                mop.getDirection().getOpposite().getStepY() * One32nd,
                mop.getDirection().getOpposite().getStepZ() * One32nd);
        final Vec3 hitDelta = hitVec.subtract(accuratePos).add(faceOffset);
        final Vec3 inBlockPosAccurate = hitDelta.scale(16d);
        final Vec3i inBlockPos =
                new Vec3i((int) inBlockPosAccurate.x(), (int) inBlockPosAccurate.y(), (int) inBlockPosAccurate.z());
        final Vec3i normalizedInBlockPos = new Vec3i(
                snapToValid(inBlockPos.getX()), snapToValid(inBlockPos.getY()), snapToValid(inBlockPos.getZ()));
        final Vec3i normalizedInBlockPosWithOffset =
                type.usePlacementOffset() ? normalizedInBlockPos.relative(mop.getDirection(), 1) : normalizedInBlockPos;

        this.blockPos = mop.getBlockPos();
        this.bitX = normalizedInBlockPosWithOffset.getX();
        this.bitY = normalizedInBlockPosWithOffset.getY();
        this.bitZ = normalizedInBlockPosWithOffset.getZ();

        normalize();
    }

    public BitLocation(final BlockPos pos, final int x, final int y, final int z) {
        blockPos = pos;
        bitX = x;
        bitY = y;
        bitZ = z;
        normalize();
    }

    public static BitLocation min(final BitLocation from, final BitLocation to) {
        final int bitX = Min(from.blockPos.getX(), to.blockPos.getX(), from.bitX, to.bitX);
        final int bitY = Min(from.blockPos.getY(), to.blockPos.getY(), from.bitY, to.bitY);
        final int bitZ = Min(from.blockPos.getZ(), to.blockPos.getZ(), from.bitZ, to.bitZ);

        return new BitLocation(
                new BlockPos(
                        Math.min(from.blockPos.getX(), to.blockPos.getX()),
                        Math.min(from.blockPos.getY(), to.blockPos.getY()),
                        Math.min(from.blockPos.getZ(), to.blockPos.getZ())),
                bitX,
                bitY,
                bitZ);
    }

    public static BitLocation max(final BitLocation from, final BitLocation to) {
        final int bitX = Max(from.blockPos.getX(), to.blockPos.getX(), from.bitX, to.bitX);
        final int bitY = Max(from.blockPos.getY(), to.blockPos.getY(), from.bitY, to.bitY);
        final int bitZ = Max(from.blockPos.getZ(), to.blockPos.getZ(), from.bitZ, to.bitZ);

        return new BitLocation(
                new BlockPos(
                        Math.max(from.blockPos.getX(), to.blockPos.getX()),
                        Math.max(from.blockPos.getY(), to.blockPos.getY()),
                        Math.max(from.blockPos.getZ(), to.blockPos.getZ())),
                bitX,
                bitY,
                bitZ);
    }

    private static int Min(final int x, final int x2, final int bitX2, final int bitX3) {
        if (x < x2) {
            return bitX2;
        }
        if (x2 == x) {
            return Math.min(bitX2, bitX3);
        }

        return bitX3;
    }

    private static int Max(final int x, final int x2, final int bitX2, final int bitX3) {
        if (x > x2) {
            return bitX2;
        }
        if (x2 == x) {
            return Math.max(bitX2, bitX3);
        }

        return bitX3;
    }

    @Override
    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public int getBitX() {
        return bitX;
    }

    @Override
    public int getBitY() {
        return bitY;
    }

    @Override
    public int getBitZ() {
        return bitZ;
    }

    @Override
    public IBitLocation offSet(final Direction direction) {
        final int newBitX = bitX + direction.getStepX();
        final int newBitY = bitY + direction.getStepY();
        final int newBitZ = bitZ + direction.getStepZ();

        return new BitLocation(blockPos, newBitX, newBitY, newBitZ);
    }

    public int snapToValid(final int x) {
        return Math.min(Math.max(0, x), 15);
    }

    private void normalize() {
        final double xOffset = Math.floor(bitX / 16d);
        final double yOffset = Math.floor(bitY / 16d);
        final double zOffset = Math.floor(bitZ / 16d);

        bitX = (bitX + 16) % 16;
        bitY = (bitY + 16) % 16;
        bitZ = (bitZ + 16) % 16;
        this.blockPos = new BlockPos.MutableBlockPos(
                blockPos.getX() + xOffset, blockPos.getY() + yOffset, blockPos.getZ() + zOffset);
    }
}
