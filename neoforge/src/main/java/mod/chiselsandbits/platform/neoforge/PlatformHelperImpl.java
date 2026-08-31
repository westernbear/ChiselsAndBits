package mod.chiselsandbits.platform.neoforge;

import dev.architectury.platform.Platform;
import java.nio.file.Path;

@SuppressWarnings("unused")
public final class PlatformHelperImpl {

    private PlatformHelperImpl() {}

    public static Path getConfigDir() {
        return Platform.getConfigFolder();
    }

    public static boolean isModLoaded(String modId) {
        return Platform.isModLoaded(modId);
    }
}
