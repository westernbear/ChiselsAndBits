package mod.chiselsandbits.printer;

import mod.chiselsandbits.registry.ModTileEntityTypes;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

public final class FabricChiselPrinterRegistration {

    private FabricChiselPrinterRegistration() {}

    public static void register() {
        ItemStorage.SIDED.registerForBlockEntity(
                (blockEntity, direction) -> ContainerStorage.of((ChiselPrinterTileEntity) blockEntity, direction),
                ModTileEntityTypes.CHISEL_PRINTER.get());
    }
}
