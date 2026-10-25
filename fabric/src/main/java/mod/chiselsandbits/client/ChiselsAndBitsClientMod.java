package mod.chiselsandbits.client;

import mod.chiselsandbits.ChiselsAndBitsModelLoadingPlugin;
import mod.chiselsandbits.bitbag.BagGui;
import mod.chiselsandbits.bitstorage.TileEntitySpecialRenderBitStorage;
import mod.chiselsandbits.compat.client.DrawSelectionEvents;
import mod.chiselsandbits.compat.client.TextureStitchCallback;
import mod.chiselsandbits.core.ChiselsAndBitsClient;
import mod.chiselsandbits.core.ClientSideImpl;
import mod.chiselsandbits.printer.ChiselPrinterScreen;
import mod.chiselsandbits.registry.ModContainerTypes;
import mod.chiselsandbits.registry.ModTileEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class ChiselsAndBitsClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        RenderPhaseBridge.install(new FabricRenderPhaseBridge());
        KeyBindings.install((translationKey, defaultKey, category) -> KeyMappingHelper.registerKeyMapping(
                new KeyMapping(translationKey, defaultKey.getType(), defaultKey.getValue(), category)));

        UseBlockCallback.EVENT.register((player, world, hand, hit) ->
                ClientEvents.fireUseBlock(new ClientEvents.UseBlockContext(player, world, hand, hit)));
        ClientTickEvents.START_CLIENT_TICK.register(client -> ClientEvents.fireStartTick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientEvents.fireEndTick());
        LevelExtractionEvents.END_EXTRACTION.register(
                context -> ClientEvents.fireExtract(new ClientEvents.ExtractContext(
                        context.levelState(),
                        context.level(),
                        context.deltaTracker(),
                        context.levelState().cameraRenderState)));
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> ClientEvents.fireSubmit(new ClientEvents.SubmitContext(
                context.levelState(),
                context.poseStack(),
                context.submitNodeCollector(),
                context.levelState().cameraRenderState)));
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(
                (context, outline) -> !DrawSelectionEvents.fireBlock(context.levelState(), outline));

        BlockEntityRenderers.register(ModTileEntityTypes.BIT_STORAGE.get(), TileEntitySpecialRenderBitStorage::new);

        final ClientSideImpl clientSide = new ClientSideImpl();
        clientSide.preInit();
        ModelLoadingPlugin.register(new ChiselsAndBitsModelLoadingPlugin());
        MenuScreens.register(ModContainerTypes.BAG_CONTAINER.get(), BagGui::new);
        MenuScreens.register(ModContainerTypes.CHISEL_STATION_CONTAINER.get(), ChiselPrinterScreen::new);
        ChiselsAndBitsClient.registerIconTextures();
        clientSide.init();
        clientSide.postInit();
        TextureStitchCallback.POST.register(ChiselsAndBitsClient::retrieveRegisteredIconSprites);
    }
}
