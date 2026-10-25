package mod.chiselsandbits.neoforge;

import mod.chiselsandbits.core.ChiselsAndBits;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ChiselsAndBits.MODID)
public class ChiselsAndBitsNeoForge {

    public ChiselsAndBitsNeoForge(final IEventBus modEventBus) {
        // Queues registry entries; NeoForgeRegistryFlush replays them from RegisterEvent.
        ChiselsAndBits.init();
        if (net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()) {
            mod.chiselsandbits.client.NeoForgeClientBootstrap.initialize();
        }
    }
}
