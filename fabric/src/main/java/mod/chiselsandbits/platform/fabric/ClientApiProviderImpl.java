package mod.chiselsandbits.platform.fabric;

import mod.chiselsandbits.api.IChiselAndBitsClientAPI;
import mod.chiselsandbits.core.api.ChiselAndBitsClientAPI;

@SuppressWarnings("unused")
public final class ClientApiProviderImpl {

    private static final IChiselAndBitsClientAPI API = new ChiselAndBitsClientAPI();

    private ClientApiProviderImpl() {}

    public static IChiselAndBitsClientAPI get() {
        return API;
    }
}
