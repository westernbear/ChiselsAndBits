package mod.chiselsandbits.registry;

import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import mod.chiselsandbits.bitbag.BagContainer;
import mod.chiselsandbits.printer.ChiselPrinterContainer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;

public final class ModContainerTypes {

    private ModContainerTypes() {
        throw new IllegalStateException("Tried to initialize: ModContainerTypes but this is a Utility class.");
    }

    public static void onModConstruction() {
        ModRegistry.add(ModRegistry.MENU_TYPES, "bag", BAG_CONTAINER);
        ModRegistry.add(ModRegistry.MENU_TYPES, "chisel_station", CHISEL_STATION_CONTAINER);
    }

    public static final Supplier<MenuType<BagContainer>> BAG_CONTAINER =
            Suppliers.memoize(() -> new MenuType<>(BagContainer::new, FeatureFlagSet.of()));
    public static final Supplier<MenuType<ChiselPrinterContainer>> CHISEL_STATION_CONTAINER =
            Suppliers.memoize(() -> new MenuType<>(ChiselPrinterContainer::new, FeatureFlagSet.of()));
}
