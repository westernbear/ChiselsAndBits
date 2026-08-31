package mod.chiselsandbits.platform.neoforge;

import mod.chiselsandbits.api.IChiselAndBitsClientAPI;

@SuppressWarnings("unused")
public final class ClientApiProviderImpl {

    private ClientApiProviderImpl() {}

    public static IChiselAndBitsClientAPI get() {
        throw new UnsupportedOperationException("Chisels & Bits client API is not available on NeoForge yet");
    }
}
