package mod.chiselsandbits.registry;

import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import mod.chiselsandbits.bitbag.BagContainer;
import mod.chiselsandbits.printer.ChiselPrinterContainer;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;

public final class ModContainerTypes {

    private ModContainerTypes() {
        throw new IllegalStateException("Tried to initialize: ModContainerTypes but this is a Utility class.");
    }

    public static void onModConstruction() {
        Registry.register(
                BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bag"), BAG_CONTAINER.get());
        Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chisel_station"),
                CHISEL_STATION_CONTAINER.get());
    }

    public static final Supplier<MenuType<BagContainer>> BAG_CONTAINER =
            Suppliers.memoize(() -> new MenuType<>(BagContainer::new, FeatureFlagSet.of()));
    public static final Supplier<MenuType<ChiselPrinterContainer>> CHISEL_STATION_CONTAINER =
            Suppliers.memoize(() -> new MenuType<>(ChiselPrinterContainer::new, FeatureFlagSet.of()));
}
