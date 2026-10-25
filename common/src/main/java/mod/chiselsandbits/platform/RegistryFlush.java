package mod.chiselsandbits.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

/**
 * Flushes {@link mod.chiselsandbits.registry.ModRegistry} entries. Fabric registers immediately;
 * NeoForge registers them from {@code RegisterEvent} and treats this as a no-op.
 */
public final class RegistryFlush {

    private RegistryFlush() {}

    @ExpectPlatform
    public static void flush() {
        throw new AssertionError("ExpectPlatform implementation missing");
    }
}
