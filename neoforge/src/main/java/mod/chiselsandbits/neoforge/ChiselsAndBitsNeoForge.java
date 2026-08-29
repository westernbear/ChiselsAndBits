package mod.chiselsandbits.neoforge;

import mod.chiselsandbits.core.ChiselsAndBits;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ChiselsAndBits.MODID)
public class ChiselsAndBitsNeoForge {
    public ChiselsAndBitsNeoForge(IEventBus modEventBus) {
        ChiselsAndBits.init();
        NeoForgeModEvents.register(modEventBus);
    }
}
