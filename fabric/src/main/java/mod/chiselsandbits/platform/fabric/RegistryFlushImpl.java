package mod.chiselsandbits.platform.fabric;

import mod.chiselsandbits.registry.ModRegistry;
import net.minecraft.core.Registry;

@SuppressWarnings("unused")
public final class RegistryFlushImpl {

    private RegistryFlushImpl() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void flush() {
        for (final ModRegistry.Entry<?> entry : ModRegistry.entries()) {
            Registry.register(
                    (Registry) entry.registrar().registry(),
                    entry.id(),
                    entry.value().get());
        }
    }
}
