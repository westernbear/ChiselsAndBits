package mod.chiselsandbits.client;

import mod.chiselsandbits.ChiselsAndBitsModelLoadingPlugin;
import mod.chiselsandbits.compat.client.TextureStitchCallback;
import mod.chiselsandbits.core.ChiselsAndBitsClient;
import mod.chiselsandbits.core.ClientSideImpl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

public class ChiselsAndBitsClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        final ClientSideImpl clientSide = new ClientSideImpl();
        clientSide.preInit();
        ModelLoadingPlugin.register(new ChiselsAndBitsModelLoadingPlugin());
        ChiselsAndBitsClient.onClientInit();
        ChiselsAndBitsClient.registerIconTextures();
        clientSide.init();
        clientSide.postInit();
        TextureStitchCallback.POST.register(ChiselsAndBitsClient::retrieveRegisteredIconSprites);
    }
}
