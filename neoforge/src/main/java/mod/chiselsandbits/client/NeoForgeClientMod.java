package mod.chiselsandbits.client;

import mod.chiselsandbits.bitbag.BagGui;
import mod.chiselsandbits.bitstorage.TileEntitySpecialRenderBitStorage;
import mod.chiselsandbits.client.model.EmptyChiseledBlockModelLoader;
import mod.chiselsandbits.client.model.baked.BaseSmartModel;
import mod.chiselsandbits.client.model.loader.LegacyItemModelDelegate;
import mod.chiselsandbits.compat.client.DrawSelectionEvents;
import mod.chiselsandbits.compat.client.GameMouseEvents;
import mod.chiselsandbits.compat.client.OverlayRenderCallback;
import mod.chiselsandbits.compat.client.TextureStitchCallback;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.printer.ChiselPrinterScreen;
import mod.chiselsandbits.registry.ModContainerTypes;
import mod.chiselsandbits.registry.ModTileEntityTypes;
import mod.chiselsandbits.render.SmartModelManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** NeoForge client wiring: registration, model hooks, render hooks and the shared client event hub. */
@EventBusSubscriber(modid = ChiselsAndBits.MODID, value = Dist.CLIENT)
public final class NeoForgeClientMod {

    private NeoForgeClientMod() {}

    @SubscribeEvent
    public static void registerMenuScreens(final RegisterMenuScreensEvent event) {
        event.register(ModContainerTypes.BAG_CONTAINER.get(), BagGui::new);
        event.register(ModContainerTypes.CHISEL_STATION_CONTAINER.get(), ChiselPrinterScreen::new);
    }

    @SubscribeEvent
    public static void registerKeys(final RegisterKeyMappingsEvent event) {
        for (final KeyMapping mapping : KeyBindings.created()) {
            event.register(mapping);
        }
    }

    @SubscribeEvent
    public static void registerLoaders(final ModelEvent.RegisterLoaders event) {
        event.register(EmptyChiseledBlockModelLoader.ID, new EmptyChiseledBlockModelLoader());
    }

    @SubscribeEvent
    public static void modifyBakingResult(final ModelEvent.ModifyBakingResult event) {
        final SmartModelManager.Installed models =
                SmartModelManager.getInstance().install();
        final java.util.Map<Identifier, ItemModel> itemModels =
                event.getBakingResult().itemStackModels();

        for (final java.util.Map.Entry<Identifier, BaseSmartModel> entry :
                models.itemModels().entrySet()) {
            final ItemModel fallback = itemModels.get(entry.getKey());
            if (fallback != null) {
                itemModels.put(entry.getKey(), new LegacyItemModelDelegate(entry.getValue(), fallback, null));
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModTileEntityTypes.CHISELED.get(), context -> new ChiseledBlockRenderer());
        event.registerBlockEntityRenderer(ModTileEntityTypes.BIT_STORAGE.get(), TileEntitySpecialRenderBitStorage::new);
    }

    @SubscribeEvent
    public static void startTick(final ClientTickEvent.Pre event) {
        ClientEvents.fireStartTick();
    }

    @SubscribeEvent
    public static void endTick(final ClientTickEvent.Post event) {
        ClientEvents.fireEndTick();
        NeoForgeClientSmoke.tick();
    }

    @SubscribeEvent
    public static void rightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().level().isClientSide()) {
            return;
        }

        final InteractionResult result = ClientEvents.fireUseBlock(new ClientEvents.UseBlockContext(
                event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec()));
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void extractLevel(final ExtractLevelRenderStateEvent event) {
        ClientEvents.fireExtract(new ClientEvents.ExtractContext(
                event.getRenderState(),
                event.getLevel(),
                event.getDeltaTracker(),
                event.getRenderState().cameraRenderState));
    }

    @SubscribeEvent
    public static void submitCustomGeometry(final SubmitCustomGeometryEvent event) {
        ClientEvents.fireSubmit(new ClientEvents.SubmitContext(
                event.getLevelRenderState(),
                event.getPoseStack(),
                event.getSubmitNodeCollector(),
                event.getLevelRenderState().cameraRenderState));
    }

    @SubscribeEvent
    public static void extractBlockOutline(final ExtractBlockOutlineRenderStateEvent event) {
        if (DrawSelectionEvents.fireBlock(
                event.getLevelRenderState(), event.getLevelRenderState().blockOutlineRenderState)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void mouseScroll(final InputEvent.MouseScrollingEvent event) {
        if (GameMouseEvents.BEFORE_SCROLL.invoker().wheelScroll(event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void atlasStitched(final TextureAtlasStitchedEvent event) {
        TextureStitchCallback.POST.invoker().stitch(event.getAtlas());
    }

    @SubscribeEvent
    public static void renderOverlay(final RenderGuiEvent.Post event) {
        OverlayRenderCallback.EVENT
                .invoker()
                .onOverlayRender(
                        event.getGuiGraphics(),
                        event.getPartialTick().getGameTimeDeltaPartialTick(false),
                        Minecraft.getInstance().getWindow(),
                        OverlayRenderCallback.Types.CROSSHAIRS);
    }
}
