package mod.chiselsandbits.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import java.nio.file.Path;

public final class PlatformHelper {

    private PlatformHelper() {}

    @ExpectPlatform
    public static Path getConfigDir() {
        throw new AssertionError("ExpectPlatform implementation missing");
    }

    @ExpectPlatform
    public static boolean isModLoaded(String modId) {
        throw new AssertionError("ExpectPlatform implementation missing");
    }
}
