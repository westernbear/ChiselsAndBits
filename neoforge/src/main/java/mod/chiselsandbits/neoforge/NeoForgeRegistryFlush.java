package mod.chiselsandbits.neoforge;

import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.registry.ModRegistry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Replays queued {@link ModRegistry} entries inside NeoForge's per-registry registration event. */
@EventBusSubscriber(modid = ChiselsAndBits.MODID)
public final class NeoForgeRegistryFlush {

    private NeoForgeRegistryFlush() {}

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onRegister(final RegisterEvent event) {
        for (final ModRegistry.Entry<?> entry : ModRegistry.entries()) {
            if (event.getRegistryKey().equals(entry.registrar().key())) {
                event.register((ResourceKey) entry.registrar().key(), entry.id(), entry.value());
            }
        }
    }
}
