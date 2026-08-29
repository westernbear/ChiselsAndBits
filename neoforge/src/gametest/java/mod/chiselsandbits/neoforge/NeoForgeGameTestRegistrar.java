package mod.chiselsandbits.neoforge;

import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.gametest.ApiServerGameTests;
import mod.chiselsandbits.gametest.LegacyCompatibilityGameTests;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.gametest.RegisterGameTestsEvent;

@EventBusSubscriber(modid = ChiselsAndBits.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NeoForgeGameTestRegistrar {

    private NeoForgeGameTestRegistrar() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        event.register(ApiServerGameTests.class);
        event.register(LegacyCompatibilityGameTests.class);
    }
}
