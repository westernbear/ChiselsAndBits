package mod.chiselsandbits.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public record FluidStorageData(Identifier fluidId, long amount) {
    public static final Identifier EMPTY_FLUID = BuiltInRegistries.FLUID.getKey(Fluids.EMPTY);

    public static final Codec<FluidStorageData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Identifier.CODEC.fieldOf("fluid").forGetter(FluidStorageData::fluidId),
                    Codec.LONG.optionalFieldOf("amount", 0L).forGetter(FluidStorageData::amount))
            .apply(instance, FluidStorageData::new));

    public FluidStorageData {
        Objects.requireNonNull(fluidId, "fluidId");
        amount = Math.max(0L, amount);
    }

    public boolean isEmpty() {
        return amount <= 0 || fluidId.equals(EMPTY_FLUID);
    }

    public Fluid fluid() {
        return BuiltInRegistries.FLUID.getValue(fluidId);
    }

    public static FluidStorageData of(final Fluid fluid, final long amount) {
        return new FluidStorageData(BuiltInRegistries.FLUID.getKey(fluid), amount);
    }
}
