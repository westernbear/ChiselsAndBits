package mod.chiselsandbits.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public final class ChiseledData {
    private static final Codec<byte[]> BYTE_ARRAY_CODEC =
            Codec.BYTE_BUFFER.xmap(ChiseledData::copyBytes, bytes -> ByteBuffer.wrap(bytes.clone()));

    public static final Codec<ChiseledData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("primary_state").forGetter(ChiseledData::primaryState),
                    BYTE_ARRAY_CODEC.fieldOf("voxel_data").forGetter(ChiseledData::voxelData),
                    Codec.INT.optionalFieldOf("side_state", 0).forGetter(ChiseledData::sideState),
                    Codec.INT.optionalFieldOf("light_value", 0).forGetter(ChiseledData::lightValue),
                    Codec.BOOL.optionalFieldOf("normal_cube", false).forGetter(ChiseledData::normalCube))
            .apply(instance, ChiseledData::new));

    private final String primaryState;
    private final byte[] voxelData;
    private final int sideState;
    private final int lightValue;
    private final boolean normalCube;

    public ChiseledData(
            final String primaryState,
            final byte[] voxelData,
            final int sideState,
            final int lightValue,
            final boolean normalCube) {
        this.primaryState = Objects.requireNonNull(primaryState, "primaryState");
        this.voxelData = voxelData == null ? new byte[0] : voxelData.clone();
        this.sideState = sideState;
        this.lightValue = lightValue;
        this.normalCube = normalCube;
    }

    public String primaryState() {
        return primaryState;
    }

    public byte[] voxelData() {
        return voxelData.clone();
    }

    public int sideState() {
        return sideState;
    }

    public int lightValue() {
        return lightValue;
    }

    public boolean normalCube() {
        return normalCube;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ChiseledData data)) {
            return false;
        }
        return sideState == data.sideState
                && lightValue == data.lightValue
                && normalCube == data.normalCube
                && primaryState.equals(data.primaryState)
                && Arrays.equals(voxelData, data.voxelData);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(primaryState, sideState, lightValue, normalCube);
        result = 31 * result + Arrays.hashCode(voxelData);
        return result;
    }

    private static byte[] copyBytes(final ByteBuffer source) {
        final ByteBuffer duplicate = source.duplicate();
        final byte[] bytes = new byte[duplicate.remaining()];
        duplicate.get(bytes);
        return bytes;
    }
}
