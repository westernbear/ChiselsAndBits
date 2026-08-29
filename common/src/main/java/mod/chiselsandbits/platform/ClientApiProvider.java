package mod.chiselsandbits.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import mod.chiselsandbits.api.IChiselAndBitsClientAPI;

public final class ClientApiProvider {

    private ClientApiProvider() {}

    @ExpectPlatform
    public static IChiselAndBitsClientAPI get() {
        throw new AssertionError("ExpectPlatform implementation missing");
    }
}
