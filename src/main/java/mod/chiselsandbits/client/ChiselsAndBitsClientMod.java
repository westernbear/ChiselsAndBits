package mod.chiselsandbits.client;

import mod.chiselsandbits.ChiselsAndBitsModelLoadingPlugin;
import mod.chiselsandbits.compat.client.TextureStitchCallback;
import mod.chiselsandbits.core.ChiselsAndBitsClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

public class ChiselsAndBitsClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(new ChiselsAndBitsModelLoadingPlugin());
        ChiselsAndBitsClient.onClientInit();
        ChiselsAndBitsClient.registerIconTextures();
        TextureStitchCallback.POST.register(ChiselsAndBitsClient::retrieveRegisteredIconSprites);
    }
}
