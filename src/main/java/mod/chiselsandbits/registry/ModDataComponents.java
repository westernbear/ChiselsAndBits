package mod.chiselsandbits.registry;

import com.mojang.serialization.Codec;
import mod.chiselsandbits.components.BagContents;
import mod.chiselsandbits.components.ChiseledData;
import mod.chiselsandbits.components.FluidStorageData;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

public final class ModDataComponents {
    public static final DataComponentType<ChiseledData> CHISELED_DATA = register("chiseled_data", ChiseledData.CODEC);
    public static final DataComponentType<Direction> PLACEMENT_SIDE = register("placement_side", Direction.CODEC);
    public static final DataComponentType<Integer> BIT_STATE = register("bit_state", Codec.INT);
    public static final DataComponentType<BagContents> BAG_CONTENTS = register("bag_contents", BagContents.CODEC);
    public static final DataComponentType<String> TOOL_MODE = register("tool_mode", Codec.STRING);
    public static final DataComponentType<DyeColor> COLOR = register("color", DyeColor.CODEC);
    public static final DataComponentType<FluidStorageData> FLUID_STORAGE =
            register("fluid_storage", FluidStorageData.CODEC);

    private ModDataComponents() {
        throw new IllegalStateException("Tried to initialize ModDataComponents as an instance.");
    }

    public static void onModConstruction() {
        // Loading this class performs the registrations above.
    }

    private static <T> DataComponentType<T> register(final String name, final Codec<T> codec) {
        return Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, name),
                DataComponentType.<T>builder().persistent(codec).cacheEncoding().build());
    }
}
