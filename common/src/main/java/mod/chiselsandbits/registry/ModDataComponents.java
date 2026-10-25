package mod.chiselsandbits.registry;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import mod.chiselsandbits.components.BagContents;
import mod.chiselsandbits.components.ChiseledData;
import mod.chiselsandbits.components.FluidStorageData;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.DyeColor;

public final class ModDataComponents {
    public static final Supplier<DataComponentType<ChiseledData>> CHISELED_DATA =
            register("chiseled_data", () -> ChiseledData.CODEC);
    public static final Supplier<DataComponentType<Direction>> PLACEMENT_SIDE =
            register("placement_side", () -> Direction.CODEC);
    public static final Supplier<DataComponentType<Integer>> BIT_STATE = register("bit_state", () -> Codec.INT);
    public static final Supplier<DataComponentType<BagContents>> BAG_CONTENTS =
            register("bag_contents", () -> BagContents.CODEC);
    public static final Supplier<DataComponentType<String>> TOOL_MODE = register("tool_mode", () -> Codec.STRING);
    public static final Supplier<DataComponentType<DyeColor>> COLOR = register("color", () -> DyeColor.CODEC);
    public static final Supplier<DataComponentType<FluidStorageData>> FLUID_STORAGE =
            register("fluid_storage", () -> FluidStorageData.CODEC);

    private ModDataComponents() {
        throw new IllegalStateException("Tried to initialize ModDataComponents as an instance.");
    }

    public static void onModConstruction() {
        // Loading this class performs the registrations above.
    }

    private static <T> Supplier<DataComponentType<T>> register(final String name, final Supplier<Codec<T>> codec) {
        final Supplier<DataComponentType<T>> type = Suppliers.memoize(() -> DataComponentType.<T>builder()
                .persistent(codec.get())
                .cacheEncoding()
                .build());
        ModRegistry.add(ModRegistry.DATA_COMPONENT_TYPES, name, type);
        return type;
    }
}
