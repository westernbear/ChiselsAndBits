package mod.chiselsandbits.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

public record FluidStorageData(FluidVariant variant, long amount) {
    public static final Codec<FluidStorageData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    FluidVariant.CODEC.fieldOf("variant").forGetter(FluidStorageData::variant),
                    Codec.LONG.optionalFieldOf("amount", 0L).forGetter(FluidStorageData::amount))
            .apply(instance, FluidStorageData::new));

    public FluidStorageData {
        Objects.requireNonNull(variant, "variant");
        amount = Math.max(0L, amount);
    }
}
