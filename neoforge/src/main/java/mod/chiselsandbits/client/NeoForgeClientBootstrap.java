package mod.chiselsandbits.client;

import mod.chiselsandbits.core.ChiselsAndBitsClient;
import mod.chiselsandbits.core.ClientSideImpl;
import net.minecraft.client.KeyMapping;

/**
 * Runs the shared client setup early enough that key mappings exist before NeoForge's
 * {@code RegisterKeyMappingsEvent} (it fires ahead of {@code FMLClientSetupEvent}).
 */
public final class NeoForgeClientBootstrap {

    private static boolean initialized;

    private NeoForgeClientBootstrap() {}

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        RenderPhaseBridge.install(new NeoRenderPhaseBridge());
        KeyBindings.install((translationKey, defaultKey, category) ->
                new KeyMapping(translationKey, defaultKey.getType(), defaultKey.getValue(), category));

        final ClientSideImpl clientSide = new ClientSideImpl();
        clientSide.preInit();
        ChiselsAndBitsClient.registerIconTextures();
        clientSide.init();
        clientSide.postInit();
    }
}
