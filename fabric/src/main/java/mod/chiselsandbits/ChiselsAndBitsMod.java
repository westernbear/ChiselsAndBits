package mod.chiselsandbits;

import mod.chiselsandbits.bitstorage.FabricBitStorageRegistration;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.printer.FabricChiselPrinterRegistration;
import net.fabricmc.api.ModInitializer;

public class ChiselsAndBitsMod implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricBitStorageRegistration.register();
        FabricChiselPrinterRegistration.register();
        ChiselsAndBits.init();
    }
}
