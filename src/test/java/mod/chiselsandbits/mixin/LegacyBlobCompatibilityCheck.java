package mod.chiselsandbits.mixin;

import java.util.Base64;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.legacy.LegacyChiseledBlockFix;

public final class LegacyBlobCompatibilityCheck {

    private static final byte[] LEGACY_1_12_BLOB =
            Base64.getDecoder().decode("eJxjYGFgXHyA+f9/RoYGDgYGBiaGUTAKaAYODLQDRgEO4ADEACSRBPU=");

    private LegacyBlobCompatibilityCheck() {}

    public static void main(final String[] args) throws Exception {
        final VoxelBlob blob = LegacyChiseledBlockFix.decodeLegacyCompact(LEGACY_1_12_BLOB, state -> state + 10);
        require(blob.get(0, 0, 0) == 11, "stone fixture voxel was not decoded");
        require(blob.get(7, 8, 9) == 32777, "modded fixture voxel was not decoded");
        require(blob.get(15, 15, 15) == 57389, "metadata fixture voxel was not decoded");
        require(blob.get(1, 1, 1) == 10, "air fixture voxel was not remapped");
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
