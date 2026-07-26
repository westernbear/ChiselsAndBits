package mod.chiselsandbits.components;

import com.mojang.serialization.Codec;
import java.util.Arrays;

public final class BagContents {
    public static final Codec<BagContents> CODEC = Codec.INT_STREAM.xmap(
            stream -> new BagContents(stream.toArray()), contents -> Arrays.stream(contents.values));

    private final int[] values;

    public BagContents(final int[] values) {
        this.values = values == null ? new int[0] : values.clone();
    }

    public int[] values() {
        return values.clone();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof BagContents contents && Arrays.equals(values, contents.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }
}
