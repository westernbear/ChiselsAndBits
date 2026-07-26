package mod.chiselsandbits.core;

import com.google.common.base.Stopwatch;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import mod.chiselsandbits.api.APIExceptions.CannotBeChiseled;
import mod.chiselsandbits.api.IBitAccess;
import mod.chiselsandbits.api.IBitBrush;
import mod.chiselsandbits.api.ItemType;
import mod.chiselsandbits.api.ModKeyBinding;
import mod.chiselsandbits.api.ReplacementStateHandler;
import mod.chiselsandbits.bitstorage.TileEntitySpecialRenderBitStorage;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.ItemBlockChiseled;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.BitIterator;
import mod.chiselsandbits.chiseledblock.data.BitLocation;
import mod.chiselsandbits.chiseledblock.data.IntegerBox;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.data.VoxelBlobStateReference;
import mod.chiselsandbits.chiseledblock.iterators.ChiselIterator;
import mod.chiselsandbits.chiseledblock.iterators.ChiselTypeIterator;
import mod.chiselsandbits.client.CreativeClipboardTab;
import mod.chiselsandbits.client.RenderHelper;
import mod.chiselsandbits.client.TapeMeasures;
import mod.chiselsandbits.client.UndoTracker;
import mod.chiselsandbits.client.gui.ChiselsAndBitsMenu;
import mod.chiselsandbits.client.gui.SpriteIconPositioning;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.compat.client.DrawSelectionEvents;
import mod.chiselsandbits.compat.client.GameMouseEvents;
import mod.chiselsandbits.compat.client.OverlayRenderCallback;
import mod.chiselsandbits.helpers.BitOperation;
import mod.chiselsandbits.helpers.ChiselModeManager;
import mod.chiselsandbits.helpers.ChiselToolType;
import mod.chiselsandbits.helpers.DeprecationHelper;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.helpers.ReadyState;
import mod.chiselsandbits.helpers.VoxelRegionSrc;
import mod.chiselsandbits.interfaces.IItemScrollWheel;
import mod.chiselsandbits.interfaces.IPatternItem;
import mod.chiselsandbits.interfaces.IVoxelBlobItem;
import mod.chiselsandbits.items.ItemChisel;
import mod.chiselsandbits.items.ItemChiseledBit;
import mod.chiselsandbits.modes.ChiselMode;
import mod.chiselsandbits.modes.IToolMode;
import mod.chiselsandbits.modes.PositivePatternMode;
import mod.chiselsandbits.modes.TapeMeasureModes;
import mod.chiselsandbits.network.packets.PacketChisel;
import mod.chiselsandbits.network.packets.PacketRotateVoxelBlob;
import mod.chiselsandbits.network.packets.PacketSetColor;
import mod.chiselsandbits.network.packets.PacketSuppressInteraction;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.registry.ModTileEntityTypes;
import mod.chiselsandbits.render.SmartModelManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class ClientSide {

    private static final RenderStateDataKey<ClientRenderState> CLIENT_RENDER_STATE =
            RenderStateDataKey.create(() -> ChiselsAndBits.MODID + ":client_render_state");

    private static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ChiselsAndBits.MODID, "main"));

    public static final ClientSide instance = new ClientSide();
    private static final Random RANDOM = new SecureRandom();
    public static TextureAtlasSprite undoIcon;
    public static TextureAtlasSprite redoIcon;
    public static TextureAtlasSprite trashIcon;
    public static TextureAtlasSprite sortIcon;
    public static TextureAtlasSprite swapIcon;
    public static TextureAtlasSprite placeIcon;
    public static TextureAtlasSprite roll_x;
    public static TextureAtlasSprite roll_z;
    public static TextureAtlasSprite white;
    public final TapeMeasures tapeMeasures = new TapeMeasures();
    private final HashMap<IToolMode, SpriteIconPositioning> chiselModeIcons = new HashMap<>();
    ReadyState readyState = ReadyState.PENDING_PRE;
    boolean wasDrawing = false;
    int displayStatus = 0;

    @NotNull
    ChiselToolType lastTool = ChiselToolType.CHISEL;

    @NotNull
    InteractionHand lastHand = InteractionHand.MAIN_HAND;

    private KeyMapping rotateCCW;
    private KeyMapping rotateCW;
    private KeyMapping undo;
    private KeyMapping redo;
    private KeyMapping modeMenu;
    private KeyMapping addToClipboard;
    private KeyMapping pickBit;
    private KeyMapping offgridPlacement;
    private Stopwatch rotateTimer;
    private ItemStack previousItem;
    private int previousRotations;
    private LegacyBakedModel previousModel;
    private RenderHelper.PreparedModel previousPreparedModel;
    private int previousPreparedAlpha = Integer.MIN_VALUE;
    private BlockPos previousPreparedPosition;
    private Object previousCacheRef;
    private IntegerBox modelBounds;
    private boolean isVisible = true;
    private boolean isUnplaceable = true;
    private BlockPos lastPartial;
    private BlockPos lastPos;
    private BitLocation drawStart;
    private int ticksSinceRelease = 0;
    private int lastRenderedFrame = Integer.MIN_VALUE;

    private record ClientRenderState(
            TapeMeasures.RenderState tapeMeasures,
            List<SelectionBoxRenderState> selectionBoxes,
            GhostRenderState ghost,
            boolean cancelDefaultOutline) {
        private ClientRenderState {
            selectionBoxes = List.copyOf(selectionBoxes);
        }
    }

    private record SelectionBoxRenderState(AABB box, BlockPos blockPos) {}

    private record GhostRenderState(
            RenderHelper.PreparedModel model,
            BlockPos blockPos,
            Vec3 partialTranslation,
            boolean isUnplaceable,
            int combinedLight,
            boolean alwaysOnTop) {}

    public static void placeSound(final Level world, final BlockPos pos, final int stateID) {
        final BlockState state = ModUtil.getStateById(stateID);
        final Block block = state.getBlock();
        world.playLocalSound(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                DeprecationHelper.getSoundType(block).getPlaceSound(),
                SoundSource.BLOCKS,
                (DeprecationHelper.getSoundType(block).getVolume() + 1.0F) / 16.0F,
                DeprecationHelper.getSoundType(block).getPitch() * 0.9F,
                false);
    }

    public static void breakSound(final Level world, final BlockPos pos, final int extractedState) {
        final BlockState state = ModUtil.getStateById(extractedState);
        final Block block = state.getBlock();
        world.playLocalSound(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                DeprecationHelper.getSoundType(block).getBreakSound(),
                SoundSource.BLOCKS,
                (DeprecationHelper.getSoundType(block).getVolume() + 1.0F) / 16.0F,
                DeprecationHelper.getSoundType(block).getPitch() * 0.9F,
                false);
    }

    public static boolean offGridPlacement(Player player) {
        if (player instanceof FakePlayer) {
            return false;
        }

        if (player.level().isClientSide()) {
            return !getOffGridPlacementKey().isUnbound()
                    && getOffGridPlacementKey().isDown();
        }

        throw new RuntimeException("checking keybinds on server.");
    }

    public static KeyMapping getOffGridPlacementKey() {
        if (ClientSide.instance.offgridPlacement.isUnbound() && ClientSide.instance.offgridPlacement.isDefault()) {
            return Minecraft.getInstance().options.keyShift;
        }

        return ClientSide.instance.offgridPlacement;
    }

    public KeyMapping getKeyBinding(ModKeyBinding modKeyBinding) {
        return switch (modKeyBinding) {
            case ROTATE_CCW -> rotateCCW;
            case ROTATE_CW -> rotateCW;
            case UNDO -> undo;
            case REDO -> redo;
            case ADD_TO_CLIPBOARD -> addToClipboard;
            case PICK_BIT -> pickBit;
            case OFFGRID_PLACEMENT -> ClientSide.getOffGridPlacementKey();
            default -> modeMenu;
        };
    }

    public void preInit() {
        readyState = readyState.updateState(ReadyState.TRIGGER_PRE);
        OverlayRenderCallback.EVENT.register(this::onRenderGUI);
        UseBlockCallback.EVENT.register(this::drawingInteractionPrevention);
        ClientTickEvents.START_CLIENT_TICK.register(this::applyChiselDelay);
        ClientTickEvents.END_CLIENT_TICK.register(this::interaction);
        LevelExtractionEvents.END_EXTRACTION.register(this::extractLevelRenderState);
        LevelRenderEvents.COLLECT_SUBMITS.register(this::collectLevelRenderState);
        DrawSelectionEvents.registerFabricBridge();
        DrawSelectionEvents.BLOCK.register(this::drawHighlight);

        GameMouseEvents.BEFORE_SCROLL.register(this::wheelEvent);
    }

    public void init() {
        readyState = readyState.updateState(ReadyState.TRIGGER_INIT);
        BlockEntityRenderers.register(ModTileEntityTypes.BIT_STORAGE.get(), TileEntitySpecialRenderBitStorage::new);

        for (final ChiselMode mode : ChiselMode.values()) {
            mode.binding = registerKeybind(mode.string.toString(), InputConstants.UNKNOWN, "itemGroup.chiselsandbits");
        }

        for (final PositivePatternMode mode : PositivePatternMode.values()) {
            mode.binding = registerKeybind(mode.string.toString(), InputConstants.UNKNOWN, "itemGroup.chiselsandbits");
        }

        for (final TapeMeasureModes mode : TapeMeasureModes.values()) {
            mode.binding = registerKeybind(mode.string.toString(), InputConstants.UNKNOWN, "itemGroup.chiselsandbits");
        }

        modeMenu = registerKeybind("mod.chiselsandbits.other.mode", InputConstants.UNKNOWN, "itemGroup.chiselsandbits");
        rotateCCW = registerKeybind(
                "mod.chiselsandbits.other.rotate.ccw", InputConstants.UNKNOWN, "itemGroup.chiselsandbits");
        rotateCW = registerKeybind(
                "mod.chiselsandbits.other.rotate.cw", InputConstants.UNKNOWN, "itemGroup.chiselsandbits");
        pickBit =
                registerKeybind("mod.chiselsandbits.other.pickbit", InputConstants.UNKNOWN, "itemGroup.chiselsandbits");
        offgridPlacement =
                registerKeybind("mod.chiselsandbits.other.offgrid", InputConstants.UNKNOWN, "itemGroup.chiselsandbits");
        undo = registerKeybind("mod.chiselsandbits.other.undo", InputConstants.UNKNOWN, "itemGroup.chiselsandbits");
        redo = registerKeybind("mod.chiselsandbits.other.redo", InputConstants.UNKNOWN, "itemGroup.chiselsandbits");
        addToClipboard = registerKeybind(
                "mod.chiselsandbits.other.add_to_clipboard", InputConstants.UNKNOWN, "itemGroup.chiselsandbits");
    }

    private KeyMapping registerKeybind(
            final String bindingName, final InputConstants.Key defaultKey, final String groupName) {
        return new KeyMapping(bindingName, defaultKey.getType(), defaultKey.getValue(), KEY_CATEGORY);
    }

    public void postInit() {
        readyState = readyState.updateState(ReadyState.TRIGGER_POST);
    }

    public SpriteIconPositioning getIconForMode(final IToolMode mode) {
        return chiselModeIcons.get(mode);
    }

    public void setIconForMode(final IToolMode mode, final SpriteIconPositioning positioning) {
        chiselModeIcons.put(mode, positioning);
    }

    public boolean onRenderGUI(
            GuiGraphicsExtractor guiGraphics, float partialTicks, Window window, OverlayRenderCallback.Types type) {
        final ChiselToolType tool = getHeldToolType(lastHand);
        if (type == OverlayRenderCallback.Types.CROSSHAIRS && tool != null && tool.hasMenu()) {
            final boolean wasVisible = ChiselsAndBitsMenu.instance.isVisible();

            if (!modeMenu.isUnbound() && modeMenu.isDown()) {
                ChiselsAndBitsMenu.instance.actionUsed = false;
                if (ChiselsAndBitsMenu.instance.raiseVisibility()) {
                    Minecraft.getInstance().mouseHandler.releaseMouse();
                }
            } else {
                if (!ChiselsAndBitsMenu.instance.actionUsed) {
                    if (ChiselsAndBitsMenu.instance.switchTo != null) {
                        ClientSide.instance.playRadialMenu();
                        ChiselModeManager.changeChiselMode(
                                tool,
                                ChiselModeManager.getChiselMode(getPlayer(), tool, InteractionHand.MAIN_HAND),
                                ChiselsAndBitsMenu.instance.switchTo);
                    }

                    if (ChiselsAndBitsMenu.instance.doAction != null) {
                        ClientSide.instance.playRadialMenu();
                        switch (ChiselsAndBitsMenu.instance.doAction) {
                            case ROLL_X:
                                PacketRotateVoxelBlob pri = new PacketRotateVoxelBlob(Axis.X, Rotation.CLOCKWISE_90);
                                ChiselsAndBits.getNetworkChannel().sendToServer(pri);
                                break;

                            case ROLL_Z:
                                PacketRotateVoxelBlob pri2 = new PacketRotateVoxelBlob(Axis.Z, Rotation.CLOCKWISE_90);
                                ChiselsAndBits.getNetworkChannel().sendToServer(pri2);
                                break;

                            case REPLACE_TOGGLE:
                                ReplacementStateHandler.getInstance()
                                        .setReplacing(!ReplacementStateHandler.getInstance()
                                                .isReplacing());
                                ReflectionWrapper.instance.clearHighlightedStack();
                                break;

                            case UNDO:
                                UndoTracker.getInstance().undo();
                                break;

                            case REDO:
                                UndoTracker.getInstance().redo();
                                break;

                            case BLACK:
                            case BLUE:
                            case BROWN:
                            case CYAN:
                            case GRAY:
                            case GREEN:
                            case LIGHT_BLUE:
                            case LIME:
                            case MAGENTA:
                            case ORANGE:
                            case PINK:
                            case PURPLE:
                            case RED:
                            case LIGHT_GRAY:
                            case WHITE:
                            case YELLOW:
                                final PacketSetColor setColor = new PacketSetColor(
                                        DyeColor.valueOf(ChiselsAndBitsMenu.instance.doAction.name()),
                                        getHeldToolType(InteractionHand.MAIN_HAND),
                                        ChiselsAndBits.getConfig()
                                                .getClient()
                                                .chatModeNotification
                                                .get());
                                ChiselsAndBits.getNetworkChannel().sendToServer(setColor);
                                ReflectionWrapper.instance.clearHighlightedStack();

                                break;
                        }
                    }
                }

                ChiselsAndBitsMenu.instance.actionUsed = true;
                ChiselsAndBitsMenu.instance.decreaseVisibility();
            }

            if (ChiselsAndBitsMenu.instance.isVisible()) {
                ChiselsAndBitsMenu.instance.init(window.getGuiScaledWidth(), window.getGuiScaledHeight());
                ChiselsAndBitsMenu.instance.configure(window.getGuiScaledWidth(), window.getGuiScaledHeight());

                if (!wasVisible) {
                    Minecraft.getInstance().gui.setScreen(ChiselsAndBitsMenu.instance);
                    Minecraft.getInstance().mouseHandler.releaseMouse();
                }

                if (Minecraft.getInstance().mouseHandler.isMouseGrabbed()) {
                    KeyMapping.releaseAll();
                }
                /*
                final int k1 = (int) (Minecraft.getInstance().mouseHelper.getMouseX() * window.getScaledWidth() / window.getWidth());
                final int l1 = (int) (window.getScaledHeight() - Minecraft.getInstance().mouseHelper.getMouseY() * window.getScaledHeight() / window.getHeight() - 1);

                net.minecraftforge.client.ForgeHooksClient.drawScreen(ChiselsAndBitsMenu.instance, event.getMatrixStack(), k1, l1, event.getPartialTicks());*/
            } else {
                if (wasVisible) {
                    Minecraft.getInstance().mouseHandler.grabMouse();
                }
            }
        }

        if (!undo.isUnbound() && undo.consumeClick()) {
            UndoTracker.getInstance().undo();
        }

        if (!redo.isUnbound() && redo.consumeClick()) {
            UndoTracker.getInstance().redo();
        }

        if (!addToClipboard.isUnbound() && addToClipboard.consumeClick()) {
            final Minecraft mc = Minecraft.getInstance();
            if (mc.hitResult != null
                    && mc.hitResult.getType() == HitResult.Type.BLOCK
                    && mc.hitResult instanceof BlockHitResult rayTraceResult) {

                try {
                    final IBitAccess access =
                            ChiselsAndBits.getApi().getBitAccess(mc.level, rayTraceResult.getBlockPos());
                    final ItemStack is = access.getBitsAsItem(null, ItemType.CHISELED_BLOCK, false);

                    CreativeClipboardTab.getInstance().addItem(is);
                } catch (final CannotBeChiseled e) {
                    // nope.
                }
            }
        }

        if (isHoldingVoxelBlob() && !pickBit.isUnbound() && pickBit.consumeClick()) {
            final Minecraft mc = Minecraft.getInstance();
            if (mc.hitResult != null
                    && mc.hitResult.getType() == HitResult.Type.BLOCK
                    && mc.hitResult instanceof BlockHitResult rayTraceResult) {

                try {
                    final BitLocation bl = new BitLocation(rayTraceResult, BitOperation.CHISEL);
                    final IBitAccess access = ChiselsAndBits.getApi().getBitAccess(mc.level, bl.getBlockPos());
                    final IBitBrush brush = access.getBitAt(bl.getBitX(), bl.getBitY(), bl.getBitZ());
                    final ItemStack is = brush.getItemStack(1);
                    doPick(is);
                } catch (final CannotBeChiseled e) {
                    // nope.
                }
            }
        }

        if (
        /*type == OverlayRenderCallback.Types.PLAYER_HEALTH
        &&*/ ChiselsAndBits.getConfig().getClient().enableToolbarIcons.get()) {
            final Minecraft mc = Minecraft.getInstance();

            if (!mc.player.isSpectator()) {
                final Gui sc = mc.gui;

                for (int slot = 0; slot < 9; ++slot) {
                    final ItemStack stack = mc.player.getInventory().getItem(slot);
                    if (stack.getItem() instanceof ItemChisel) {
                        final ChiselToolType toolType = getToolTypeForItem(stack);
                        IToolMode mode = toolType.getMode(stack);

                        if (!ChiselsAndBits.getConfig()
                                        .getClient()
                                        .perChiselMode
                                        .get()
                                && tool == ChiselToolType.CHISEL) {
                            mode = ChiselModeManager.getChiselMode(mc.player, ChiselToolType.CHISEL, lastHand);
                        }

                        final int x = window.getGuiScaledWidth() / 2 - 90 + slot * 20 + 2;
                        final int y = window.getGuiScaledHeight() - 16 - 3;
                        final TextureAtlasSprite sprite =
                                chiselModeIcons.get(mode) == null ? getMissingIcon() : chiselModeIcons.get(mode).sprite;
                        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + 1, y + 1, 8, 8);
                    }
                }
            }
        }
        return false;
    }

    public void playRadialMenu() {
        final double volume =
                ChiselsAndBits.getConfig().getClient().radialMenuVolume.get();
        if (volume >= 0.0001f) {
            final SoundInstance psr = new SimpleSoundInstance(
                    SoundEvents.UI_BUTTON_CLICK.value(),
                    SoundSource.MASTER,
                    (float) volume,
                    1.0f,
                    RandomSource.create(),
                    getPlayer().blockPosition());
            Minecraft.getInstance().getSoundManager().play(psr);
        }
    }

    private boolean doPick(final @NotNull ItemStack result) {
        final Player player = getPlayer();

        for (int x = 0; x < 9; x++) {
            final ItemStack stack = player.getInventory().getItem(x);
            if (stack != null
                    && ItemStack.isSameItem(stack, result)
                    && ItemStack.isSameItemSameComponents(stack, result)) {
                player.getInventory().setSelectedSlot(x);
                return true;
            }
        }

        if (!player.isCreative()) {
            return false;
        }

        int slot = player.getInventory().getFreeSlot();
        if (slot < 0 || slot >= 9) {
            slot = player.getInventory().getSelectedSlot();
        }

        // update inventory..
        player.getInventory().setItem(slot, result);
        player.getInventory().setSelectedSlot(slot);

        // update server...
        final int j =
                player.inventoryMenu.slots.size() - 9 + player.getInventory().getSelectedSlot();
        Minecraft.getInstance()
                .gameMode
                .handleCreativeModeItemAdd(
                        player.getInventory().getItem(player.getInventory().getSelectedSlot()), j);
        return true;
    }

    public ChiselToolType getHeldToolType(final InteractionHand Hand) {
        final Player player = getPlayer();

        if (player == null) {
            return null;
        }

        final ItemStack is = player.getItemInHand(Hand);
        return getToolTypeForItem(is);
    }

    private ChiselToolType getToolTypeForItem(final ItemStack is) {
        return ChiselToolType.fromItemStack(is);
    }

    public InteractionResult drawingInteractionPrevention(
            Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {

        if (world.isClientSide()) {
            final ChiselToolType tool = getHeldToolType(hand);
            final IToolMode chMode = ChiselModeManager.getChiselMode(getPlayer(), tool, hand);

            final BitLocation other = getStartPos();
            if ((chMode == ChiselMode.DRAWN_REGION || tool == ChiselToolType.TAPEMEASURE) && other != null) {
                // this handles the client side, but the server side will fire
                // separately.
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    public void applyChiselDelay(Minecraft inst) {
        if (!Minecraft.getInstance().options.keyAttack.isUnbound()
                && !Minecraft.getInstance().options.keyAttack.isDown()) {
            ItemChisel.resetDelay();
        }
    }

    public void interaction(Minecraft inst) {
        if (!readyState.isReady()) {
            return;
        }

        // used to prevent hyper chisels.. its actually far worse then you might
        // think...

        if (!getToolKey().isDown() && lastTool == ChiselToolType.CHISEL) {
            if (ticksSinceRelease >= 4) {
                if (drawStart != null) {
                    drawStart = null;
                    lastHand = InteractionHand.MAIN_HAND;
                }

                lastTool = ChiselToolType.CHISEL;
                ticksSinceRelease = 0;
            } else {
                ticksSinceRelease++;
            }
        } else {
            ticksSinceRelease = 0;
        }

        if (isHoldingVoxelBlob() && !rotateCCW.isUnbound() && rotateCCW.isDown()) {
            if (rotateTimer == null || rotateTimer.elapsed(TimeUnit.MILLISECONDS) > 200) {
                rotateTimer = Stopwatch.createStarted();
                final PacketRotateVoxelBlob p = new PacketRotateVoxelBlob(Axis.Y, Rotation.COUNTERCLOCKWISE_90);
                ChiselsAndBits.getNetworkChannel().sendToServer(p);
            }
        }

        if (isHoldingVoxelBlob() && !rotateCW.isUnbound() && rotateCW.isDown()) {
            if (rotateTimer == null || rotateTimer.elapsed(TimeUnit.MILLISECONDS) > 200) {
                rotateTimer = Stopwatch.createStarted();
                final PacketRotateVoxelBlob p = new PacketRotateVoxelBlob(Axis.Y, Rotation.CLOCKWISE_90);
                ChiselsAndBits.getNetworkChannel().sendToServer(p);
            }
        }

        for (final ChiselMode mode : ChiselMode.values()) {
            final KeyMapping kb = (KeyMapping) mode.binding;
            if (!kb.isUnbound() && kb.isDown()) {
                final ChiselToolType tool = getHeldToolType(lastHand);
                if (tool != null && tool.isBitOrChisel()) {
                    ChiselModeManager.changeChiselMode(
                            tool, ChiselModeManager.getChiselMode(getPlayer(), tool, lastHand), mode);
                }
            }
        }

        for (final PositivePatternMode mode : PositivePatternMode.values()) {
            final KeyMapping kb = (KeyMapping) mode.binding;
            if (!kb.isUnbound() && kb.isDown()) {
                final ChiselToolType tool = getHeldToolType(lastHand);
                if (tool == ChiselToolType.POSITIVEPATTERN) {
                    ChiselModeManager.changeChiselMode(
                            tool, ChiselModeManager.getChiselMode(getPlayer(), tool, lastHand), mode);
                }
            }
        }

        for (final TapeMeasureModes mode : TapeMeasureModes.values()) {
            final KeyMapping kb = (KeyMapping) mode.binding;
            if (!kb.isUnbound() && kb.isDown()) {
                final ChiselToolType tool = getHeldToolType(lastHand);
                if (tool == ChiselToolType.TAPEMEASURE) {
                    ChiselModeManager.changeChiselMode(
                            tool, ChiselModeManager.getChiselMode(getPlayer(), tool, lastHand), mode);
                }
            }
        }
    }

    private boolean isHoldingVoxelBlob() {
        final Player player = getPlayer();
        return player != null && player.getMainHandItem().getItem() instanceof IVoxelBlobItem;
    }

    @Environment(EnvType.CLIENT)
    private void extractLevelRenderState(final LevelExtractionContext context) {
        ++lastRenderedFrame;

        final float partialTicks = context.deltaTracker().getGameTimeDeltaPartialTick(false);
        final Player player = getPlayer();
        final ClientRenderState state = player == null
                ? new ClientRenderState(new TapeMeasures.RenderState(List.of()), List.of(), null, false)
                : new ClientRenderState(
                        extractTapeMeasureRenderState(partialTicks),
                        extractSelectionBoxes(partialTicks),
                        Minecraft.getInstance().gui.hud.isHidden() ? null : extractGhostRenderState(),
                        false);

        ((FabricRenderState) context.levelState()).setData(CLIENT_RENDER_STATE, state);
    }

    @Environment(EnvType.CLIENT)
    private void collectLevelRenderState(final LevelRenderContext context) {
        final ClientRenderState state = ((FabricRenderState) context.levelState()).getData(CLIENT_RENDER_STATE);
        if (state == null) {
            return;
        }

        final PoseStack matrixStack = context.poseStack();
        final SubmitNodeCollector collector = context.submitNodeCollector();
        final CameraRenderState camera = context.levelState().cameraRenderState;

        RenderHelper.withSubmitCollector(collector, () -> {
            tapeMeasures.submitRenderState(state.tapeMeasures(), matrixStack, collector, camera);
            if (camera.pos == null) {
                return;
            }

            matrixStack.pushPose();
            matrixStack.translate(-camera.pos.x, -camera.pos.y, -camera.pos.z);
            for (final SelectionBoxRenderState selectionBox : state.selectionBoxes()) {
                RenderHelper.drawSelectionBoundingBoxIfExists(
                        matrixStack, collector, selectionBox.box(), selectionBox.blockPos(), false);
            }
            matrixStack.popPose();

            final GhostRenderState ghost = state.ghost();
            if (ghost == null) {
                return;
            }

            matrixStack.pushPose();
            if (ghost.partialTranslation() != null) {
                final Vec3 translation = ghost.partialTranslation();
                matrixStack.translate(translation.x, translation.y, translation.z);
            }
            matrixStack.translate(
                    ghost.blockPos().getX() - camera.pos.x - 0.000125,
                    ghost.blockPos().getY() - camera.pos.y + 0.000125,
                    ghost.blockPos().getZ() - camera.pos.z - 0.000125);
            matrixStack.scale(1.001F, 1.001F, 1.001F);
            RenderHelper.submitPreparedModel(
                    collector,
                    matrixStack.last().copy(),
                    ghost.model(),
                    ghost.combinedLight(),
                    OverlayTexture.NO_OVERLAY,
                    ghost.alwaysOnTop());
            matrixStack.popPose();
        });
    }

    @Environment(EnvType.CLIENT)
    private boolean drawHighlight(final LevelRenderContext context, final BlockOutlineRenderState outline) {
        final ClientRenderState state = ((FabricRenderState) context.levelState()).getData(CLIENT_RENDER_STATE);
        return state != null && state.cancelDefaultOutline();
    }

    @Environment(EnvType.CLIENT)
    private TapeMeasures.RenderState extractTapeMeasureRenderState(final float partialTicks) {
        ChiselToolType tool = getHeldToolType(lastHand);
        final IToolMode chMode = ChiselModeManager.getChiselMode(getPlayer(), tool, lastHand);
        if (chMode == ChiselMode.DRAWN_REGION) {
            tool = lastTool;
        }

        tapeMeasures.setPreviewMeasure(null, null, chMode, null);

        if (tool == ChiselToolType.TAPEMEASURE) {
            final Player player = getPlayer();
            final HitResult mop = Minecraft.getInstance().hitResult;

            final Level theWorld = player.level();

            if (mop != null && mop.getType() == HitResult.Type.BLOCK) {
                final BlockHitResult blockRayTraceResult = (BlockHitResult) mop;
                final BitLocation location = new BitLocation(blockRayTraceResult, BitOperation.CHISEL);
                if (theWorld.getWorldBorder().isWithinBounds(location.blockPos)) {
                    final BitLocation other = getStartPos();
                    if (other != null) {
                        tapeMeasures.setPreviewMeasure(
                                other, location, chMode, getPlayer().getItemInHand(lastHand));

                        if (!getToolKey().isUnbound() && !getToolKey().isDown()) {
                            tapeMeasures.addMeasure(
                                    other, location, chMode, getPlayer().getItemInHand(lastHand));
                            drawStart = null;
                            lastHand = InteractionHand.MAIN_HAND;
                        }
                    }
                }
            }
        }

        final boolean isDrawing =
                (chMode == ChiselMode.DRAWN_REGION || tool == ChiselToolType.TAPEMEASURE) && getStartPos() != null;
        if (isDrawing != wasDrawing) {
            wasDrawing = isDrawing;
            final PacketSuppressInteraction packet = new PacketSuppressInteraction(isDrawing);
            ChiselsAndBits.getNetworkChannel().sendToServer(packet);
        }

        return tapeMeasures.extractRenderState(partialTicks);
    }

    @Environment(EnvType.CLIENT)
    private List<SelectionBoxRenderState> extractSelectionBoxes(final float partialTicks) {
        final List<SelectionBoxRenderState> selectionBoxes = new ArrayList<>();
        ChiselToolType tool = getHeldToolType(lastHand);

        final IToolMode chMode = ChiselModeManager.getChiselMode(getPlayer(), tool, lastHand);
        if (chMode == ChiselMode.DRAWN_REGION) {
            tool = lastTool;
        }

        if (tool != null && tool.isBitOrChisel() && chMode != null) {
            final Player player = Minecraft.getInstance().player;
            final HitResult mop = Minecraft.getInstance().hitResult;

            final Level theWorld = player.level();

            if (mop == null || mop.getType() != HitResult.Type.BLOCK) {
                return selectionBoxes;
            }

            boolean showBox = false;
            if (mop.getType() == HitResult.Type.BLOCK) {
                final BlockHitResult rayTraceResult = (BlockHitResult) mop;
                final BitLocation location = new BitLocation(
                        rayTraceResult,
                        getLastBitOperation(player, lastHand, getPlayer().getItemInHand(lastHand)));
                if (theWorld.getWorldBorder().isWithinBounds(location.blockPos)) {
                    // this logic originated in the vanilla bounding box...
                    final BlockState state = theWorld.getBlockState(location.blockPos);

                    final boolean isChisel = getDrawnTool() == ChiselToolType.CHISEL;
                    final boolean isBit = getHeldToolType(InteractionHand.MAIN_HAND) == ChiselToolType.BIT;
                    final TileEntityBlockChiseled data =
                            ModUtil.getChiseledTileEntity(theWorld, location.blockPos, false);

                    final VoxelRegionSrc region = new VoxelRegionSrc(theWorld, location.blockPos, 1);
                    final VoxelBlob vb = data != null ? data.getBlob() : new VoxelBlob();

                    if ((isChisel) && data == null) {
                        showBox = true;
                        vb.fill(1);
                    }

                    final BitLocation other = getStartPos();

                    if (chMode == ChiselMode.DRAWN_REGION && other != null) {
                        final ChiselIterator oneEnd = ChiselTypeIterator.create(
                                VoxelBlob.dim,
                                location.bitX,
                                location.bitY,
                                location.bitZ,
                                VoxelBlob.NULL_BLOB,
                                ChiselMode.SINGLE,
                                Direction.UP,
                                tool == ChiselToolType.BIT);
                        final ChiselIterator otherEnd = ChiselTypeIterator.create(
                                VoxelBlob.dim,
                                other.bitX,
                                other.bitY,
                                other.bitZ,
                                VoxelBlob.NULL_BLOB,
                                ChiselMode.SINGLE,
                                Direction.UP,
                                tool == ChiselToolType.BIT);

                        final AABB a = oneEnd.getBoundingBox(VoxelBlob.NULL_BLOB, false)
                                .move(location.blockPos.getX(), location.blockPos.getY(), location.blockPos.getZ());
                        final AABB b = otherEnd.getBoundingBox(VoxelBlob.NULL_BLOB, false)
                                .move(other.blockPos.getX(), other.blockPos.getY(), other.blockPos.getZ());

                        final AABB bb = a.minmax(b);

                        final double maxChiseSize = ChiselsAndBits.getConfig()
                                        .getClient()
                                        .maxDrawnRegionSize
                                        .get()
                                + 0.001;

                        if (bb.maxX - bb.minX <= maxChiseSize
                                && bb.maxY - bb.minY <= maxChiseSize
                                && bb.maxZ - bb.minZ <= maxChiseSize) {
                            selectionBoxes.add(new SelectionBoxRenderState(bb, BlockPos.ZERO));

                            if (!getToolKey().isDown()) {
                                final PacketChisel pc = new PacketChisel(
                                        getLastBitOperation(player, lastHand, player.getItemInHand(lastHand)),
                                        location,
                                        other,
                                        Direction.UP,
                                        ChiselMode.DRAWN_REGION,
                                        lastHand);

                                if (pc.doAction(getPlayer()) > 0) {
                                    ChiselsAndBits.getNetworkChannel().sendToServer(pc);
                                    ClientSide.placeSound(theWorld, location.blockPos, 0);
                                }

                                drawStart = null;
                                lastHand = InteractionHand.MAIN_HAND;
                                lastTool = ChiselToolType.CHISEL;
                            }
                        }
                    } else {
                        final BlockEntity te = theWorld.getChunkAt(location.blockPos)
                                .getBlockEntity(location.blockPos, LevelChunk.EntityCreationType.CHECK);

                        boolean isBitBlock = te instanceof TileEntityBlockChiseled;
                        final boolean isBlockSupported = BlockBitInfo.canChisel(state);

                        if (!(isBitBlock || isBlockSupported)) {
                            final TileEntityBlockChiseled tebc =
                                    ModUtil.getChiseledTileEntity(theWorld, location.blockPos, false);
                            if (tebc != null) {
                                final VoxelBlob vx = tebc.getBlob();
                                if (vx.get(location.bitX, location.bitY, location.bitZ) != 0) {
                                    isBitBlock = true;
                                }
                            }
                        }

                        if (theWorld.isEmptyBlock(location.blockPos) || isBitBlock || isBlockSupported) {
                            final ChiselIterator i = ChiselTypeIterator.create(
                                    VoxelBlob.dim,
                                    location.bitX,
                                    location.bitY,
                                    location.bitZ,
                                    region,
                                    ChiselMode.castMode(chMode),
                                    rayTraceResult.getDirection(),
                                    !isChisel);
                            final AABB bb = i.getBoundingBox(
                                    getLastBitOperation(
                                                            player,
                                                            lastHand,
                                                            getPlayer().getItemInHand(lastHand))
                                                    != BitOperation.REPLACE
                                            ? vb
                                            : new VoxelBlob(),
                                    isChisel);
                            selectionBoxes.add(new SelectionBoxRenderState(bb, location.blockPos));
                            showBox = false;
                        } else if (isBit) {
                            final VoxelBlob j = new VoxelBlob();
                            j.fill(1);
                            final ChiselIterator i = ChiselTypeIterator.create(
                                    VoxelBlob.dim,
                                    location.bitX,
                                    location.bitY,
                                    location.bitZ,
                                    j,
                                    ChiselMode.castMode(chMode),
                                    rayTraceResult.getDirection(),
                                    !isChisel);
                            final AABB bb = snapToSide(i.getBoundingBox(j, isChisel), rayTraceResult.getDirection());
                            selectionBoxes.add(new SelectionBoxRenderState(bb, location.blockPos));
                        }
                    }
                }

                if (!showBox) {
                    return selectionBoxes;
                }
            }
        }

        return selectionBoxes;
    }

    private BitOperation getLastBitOperation(
            final Player player, final InteractionHand lastHand2, final ItemStack heldItem) {
        return lastTool == ChiselToolType.BIT
                ? ItemChiseledBit.getBitOperation(player, lastHand, player.getItemInHand(lastHand))
                : BitOperation.CHISEL;
    }

    private AABB snapToSide(final AABB boundingBox, final Direction sideHit) {
        if (boundingBox != null) {
            switch (sideHit) {
                case DOWN:
                    return new AABB(
                            boundingBox.minX,
                            boundingBox.minY,
                            boundingBox.minZ,
                            boundingBox.maxX,
                            boundingBox.minY,
                            boundingBox.maxZ);
                case EAST:
                    return new AABB(
                            boundingBox.maxX,
                            boundingBox.minY,
                            boundingBox.minZ,
                            boundingBox.maxX,
                            boundingBox.maxY,
                            boundingBox.maxZ);
                case NORTH:
                    return new AABB(
                            boundingBox.minX,
                            boundingBox.minY,
                            boundingBox.minZ,
                            boundingBox.maxX,
                            boundingBox.maxY,
                            boundingBox.minZ);
                case SOUTH:
                    return new AABB(
                            boundingBox.minX,
                            boundingBox.minY,
                            boundingBox.maxZ,
                            boundingBox.maxX,
                            boundingBox.maxY,
                            boundingBox.maxZ);
                case UP:
                    return new AABB(
                            boundingBox.minX,
                            boundingBox.maxY,
                            boundingBox.minZ,
                            boundingBox.maxX,
                            boundingBox.maxY,
                            boundingBox.maxZ);
                case WEST:
                    return new AABB(
                            boundingBox.minX,
                            boundingBox.minY,
                            boundingBox.minZ,
                            boundingBox.minX,
                            boundingBox.maxY,
                            boundingBox.maxZ);
                default:
                    break;
            }
        }

        return boundingBox;
    }

    @Environment(EnvType.CLIENT)
    private GhostRenderState extractGhostRenderState() {
        final Player player = Minecraft.getInstance().player;
        final HitResult mop = Minecraft.getInstance().hitResult;
        final Level theWorld = player.level();
        final ItemStack currentItem = player.getMainHandItem();

        if (mop == null) {
            return null;
        }

        final AtomicBoolean isPositivePatten = new AtomicBoolean(false);
        if (ModUtil.isHoldingPattern(player, isPositivePatten)) {
            if (mop.getType() != HitResult.Type.BLOCK) {
                return null;
            }

            final BlockHitResult rayTraceResult = (BlockHitResult) mop;
            final IToolMode mode =
                    ChiselModeManager.getChiselMode(player, ChiselToolType.POSITIVEPATTERN, InteractionHand.MAIN_HAND);
            final BlockPos pos = rayTraceResult.getBlockPos();
            final BlockState state = theWorld.getBlockState(pos);
            if (!(state.getBlock() instanceof BlockChiseled) && !BlockBitInfo.canChisel(state)) {
                return null;
            }

            if (!ModItems.ITEM_NEGATIVE_PRINT_WRITTEN.get().isWritten(currentItem)
                    && !ModItems.ITEM_POSITIVE_PRINT_WRITTEN.get().isWritten(currentItem)) {
                return null;
            }

            final ItemStack item = isPositivePatten.get()
                    ? ModItems.ITEM_POSITIVE_PRINT_WRITTEN.get().getPatternedItem(currentItem, false)
                    : ModItems.ITEM_NEGATIVE_PRINT_WRITTEN.get().getPatternedItem(currentItem, false);
            if (item == null || !ModUtil.hasChiseledData(item)) {
                return null;
            }

            final int rotations = ModUtil.getRotations(player, ModUtil.getSide(currentItem));
            if (mode == PositivePatternMode.PLACEMENT) {
                return doGhostForChiseledBlock(theWorld, player, rayTraceResult, item, item, rotations);
            }

            final TileEntityBlockChiseled tebc = ModUtil.getChiseledTileEntity(theWorld, pos, false);
            Object cacheRef = tebc != null ? tebc : state;
            if (cacheRef instanceof TileEntityBlockChiseled chiseled) {
                cacheRef = chiseled.getBlobStateReference();
            }

            return showGhost(
                    currentItem, item, pos, player, rotations, rayTraceResult.getDirection(), null, cacheRef, true);
        }

        if (ModUtil.isHoldingChiseledBlock(player) && mop.getType() == HitResult.Type.BLOCK) {
            if (!ModUtil.hasChiseledData(currentItem)) {
                return null;
            }

            return doGhostForChiseledBlock(
                    theWorld,
                    player,
                    (BlockHitResult) mop,
                    currentItem,
                    currentItem,
                    ModUtil.getRotations(player, ModUtil.getSide(currentItem)));
        }

        return null;
    }

    private GhostRenderState doGhostForChiseledBlock(
            final Level theWorld,
            final Player player,
            final BlockHitResult mop,
            final ItemStack currentItem,
            final ItemStack item,
            final int rotations) {
        final BlockPos offset = mop.getBlockPos();

        if (ClientSide.offGridPlacement(player)) {
            final BitLocation location = new BitLocation(mop, BitOperation.PLACE);
            return showGhost(
                    currentItem,
                    item,
                    location.blockPos,
                    player,
                    rotations,
                    mop.getDirection(),
                    new BlockPos(location.bitX, location.bitY, location.bitZ),
                    null,
                    false);
        }

        boolean canMerge = false;
        if (ModUtil.hasChiseledData(currentItem)) {
            final TileEntityBlockChiseled tebc = ModUtil.getChiseledTileEntity(theWorld, offset, true);
            if (tebc != null) {
                canMerge = tebc.canMerge(ModUtil.getBlobFromStack(currentItem, player));
            }
        }

        BlockPos newOffset = offset;
        final InteractionHand hand =
                player.getUsedItemHand() != null ? player.getUsedItemHand() : InteractionHand.MAIN_HAND;
        if (!canMerge
                && !ClientSide.offGridPlacement(player)
                && !theWorld.getBlockState(newOffset)
                        .canBeReplaced(new BlockPlaceContext(player, hand, player.getItemInHand(hand), mop))) {
            newOffset = offset.relative(mop.getDirection());
        }

        final BlockEntity newTarget = theWorld.getBlockEntity(newOffset);
        final BlockHitResult offsetHit = new BlockHitResult(
                mop.getLocation()
                        .add(
                                mop.getDirection().getStepX(),
                                mop.getDirection().getStepY(),
                                mop.getDirection().getStepZ()),
                mop.getDirection(),
                mop.getBlockPos().relative(mop.getDirection()),
                mop.isInside());

        if (theWorld.isEmptyBlock(newOffset)
                || newTarget instanceof TileEntityBlockChiseled
                || (theWorld.getBlockEntity(newOffset) instanceof TileEntityBlockChiseled
                        && theWorld.getBlockState(newOffset)
                                .canBeReplaced(
                                        new BlockPlaceContext(player, hand, player.getItemInHand(hand), offsetHit)))
                || (!(theWorld.getBlockEntity(newOffset) instanceof TileEntityBlockChiseled)
                        && theWorld.getBlockState(newOffset)
                                .canBeReplaced(new BlockPlaceContext(player, hand, player.getItemInHand(hand), mop)))) {
            final TileEntityBlockChiseled target = ModUtil.getChiseledTileEntity(theWorld, newOffset, false);
            return showGhost(
                    currentItem,
                    item,
                    newOffset,
                    player,
                    rotations,
                    mop.getDirection(),
                    null,
                    target == null ? null : target.getBlobStateReference(),
                    false);
        }

        return null;
    }

    private GhostRenderState showGhost(
            final ItemStack refItem,
            final ItemStack item,
            final BlockPos blockPos,
            final Player player,
            final int rotationCount,
            final Direction side,
            final BlockPos partial,
            final Object cacheRef,
            final boolean alwaysOnTop) {
        LegacyBakedModel baked = null;

        if (previousCacheRef == cacheRef
                && samePos(lastPos, blockPos)
                && previousItem == refItem
                && previousRotations == rotationCount
                && previousModel != null
                && samePos(lastPartial, partial)) {
            baked = previousModel;
        } else {
            int rotations = rotationCount;

            previousItem = refItem;
            previousRotations = rotations;
            previousCacheRef = cacheRef;
            lastPos = blockPos;
            lastPartial = partial;
            previousPreparedModel = null;

            final NBTBlobConverter converter = new NBTBlobConverter();
            converter.readFromStack(item, VoxelBlob.VERSION_ANY);
            VoxelBlob blob = converter.getBlob();

            while (rotations-- > 0) {
                blob = blob.spin(Axis.Y);
            }

            modelBounds = blob.getBounds();

            fail:
            if (refItem.getItem() == ModItems.ITEM_NEGATIVE_PRINT_WRITTEN.get()) {
                final VoxelBlob pattern = blob;

                if (cacheRef instanceof VoxelBlobStateReference reference) {
                    blob = reference.getVoxelBlob();
                } else if (cacheRef instanceof BlockState blockState) {
                    blob = new VoxelBlob();
                    blob.fill(ModUtil.getStateId(blockState));
                } else {
                    break fail;
                }

                final BitIterator iterator = new BitIterator();
                while (iterator.hasNext()) {
                    if (iterator.getNext(pattern) == 0) {
                        iterator.setNext(blob, 0);
                    }
                }
            }

            converter.setBlob(blob);

            final Block block = Block.byItem(item.getItem());
            final ItemStack modelStack = converter.getItemStack(false);
            if (modelStack == null || modelStack.isEmpty()) {
                isVisible = false;
            } else {
                previousModel = baked = SmartModelManager.getInstance()
                        .resolveLegacyItemModel(modelStack, (ClientLevel) player.level(), player);
                isVisible = true;
                if (!(refItem.getItem() instanceof IPatternItem)) {
                    isUnplaceable = !ItemBlockChiseled.tryPlaceBlockAt(
                            block,
                            item,
                            player,
                            player.level(),
                            blockPos,
                            side,
                            InteractionHand.MAIN_HAND,
                            0.5,
                            0.5,
                            0.5,
                            partial,
                            false);
                }
            }
        }

        if (!isVisible || baked == null) {
            return null;
        }

        Vec3 partialTranslation = null;
        if (partial != null) {
            final BlockPos offset = ModUtil.getPartialOffset(side, partial, modelBounds);
            final double scale = 1.0 / VoxelBlob.dim;
            partialTranslation = new Vec3(offset.getX() * scale, offset.getY() * scale, offset.getZ() * scale);
        }

        final int alpha = isUnplaceable ? 0x22000000 : 0xaa000000;
        if (previousPreparedModel == null
                || previousPreparedAlpha != alpha
                || !samePos(previousPreparedPosition, blockPos)) {
            previousPreparedModel = RenderHelper.prepareModel(baked, player.level(), blockPos, alpha);
            previousPreparedAlpha = alpha;
            previousPreparedPosition = blockPos;
        }

        return new GhostRenderState(
                previousPreparedModel,
                blockPos,
                partialTranslation,
                isUnplaceable,
                LightCoordsUtil.getLightCoords(player.level(), blockPos),
                alwaysOnTop);
    }

    private boolean samePos(final BlockPos lastPartial2, final BlockPos partial) {
        if (lastPartial2 == partial) {
            return true;
        }

        if (lastPartial2 == null || partial == null) {
            return false;
        }

        return partial.equals(lastPartial2);
    }

    public Player getPlayer() {
        return Minecraft.getInstance().player;
    }

    public boolean addHitEffects(
            final Level world,
            final BlockHitResult target,
            final BlockState state,
            final ParticleEngine effectRenderer) {
        final ItemStack hitWith = getPlayer().getMainHandItem();
        if (hitWith != null
                && (hitWith.getItem() instanceof ItemChisel || hitWith.getItem() instanceof ItemChiseledBit)) {
            return true; // no
            // effects!
        }

        final BlockPos pos = target.getBlockPos();
        final float boxOffset = 0.1F;

        AABB bb = state.getShape(world, pos, CollisionContext.empty()).bounds();

        double x = RANDOM.nextDouble() * (bb.maxX - bb.minX - boxOffset * 2.0F) + boxOffset + bb.minX;
        double y = RANDOM.nextDouble() * (bb.maxY - bb.minY - boxOffset * 2.0F) + boxOffset + bb.minY;
        double z = RANDOM.nextDouble() * (bb.maxZ - bb.minZ - boxOffset * 2.0F) + boxOffset + bb.minZ;

        switch (target.getDirection()) {
            case DOWN:
                y = bb.minY - boxOffset;
                break;
            case EAST:
                x = bb.maxX + boxOffset;
                break;
            case NORTH:
                z = bb.minZ - boxOffset;
                break;
            case SOUTH:
                z = bb.maxZ + boxOffset;
                break;
            case UP:
                y = bb.maxY + boxOffset;
                break;
            case WEST:
                x = bb.minX - boxOffset;
                break;
            default:
                break;
        }

        effectRenderer.add((new TerrainParticle((ClientLevel) world, x, y, z, 0.0D, 0.0D, 0.0D, state, pos))
                .setPower(0.2F)
                .scale(0.6F));

        return true;
    }

    public boolean wheelEvent(double deltaX, double deltaY) {
        final int dwheel = (int) deltaY;
        if (dwheel == 0) {
            return false;
        }

        final Player player = ClientSide.instance.getPlayer();
        final ItemStack is = player.getMainHandItem();

        if (dwheel != 0 && is != null && is.getItem() instanceof IItemScrollWheel && player.isShiftKeyDown()) {
            ((IItemScrollWheel) is.getItem()).scroll(player, is, dwheel);
            return true;
        }
        return false;
    }

    public int getLastRenderedFrame() {
        return lastRenderedFrame;
    }

    public BitLocation getStartPos() {
        return drawStart;
    }

    public void pointAt(
            @NotNull final ChiselToolType type, @NotNull final BitLocation pos, @NotNull final InteractionHand hand) {
        if (drawStart == null) {
            drawStart = pos;
            lastTool = type;
            lastHand = hand;
        }
    }

    public void setLastTool(@NotNull final ChiselToolType lastTool) {
        this.lastTool = lastTool;
    }

    KeyMapping getToolKey() {
        if (lastTool == ChiselToolType.CHISEL) {
            return Minecraft.getInstance().options.keyAttack;
        } else {
            return Minecraft.getInstance().options.keyUse;
        }
    }

    public boolean addBlockDestroyEffects(@NotNull final Level world, @NotNull final BlockPos pos, BlockState state) {
        if (!state.isAir()) {
            VoxelShape voxelshape = state.getShape(world, pos);
            double d0 = 0.25D;
            voxelshape.forAllBoxes((p_228348_3_, p_228348_5_, p_228348_7_, p_228348_9_, p_228348_11_, p_228348_13_) -> {
                double d1 = Math.min(1.0D, p_228348_9_ - p_228348_3_);
                double d2 = Math.min(1.0D, p_228348_11_ - p_228348_5_);
                double d3 = Math.min(1.0D, p_228348_13_ - p_228348_7_);
                int i = Math.max(2, Mth.ceil(d1 / 0.25D));
                int j = Math.max(2, Mth.ceil(d2 / 0.25D));
                int k = Math.max(2, Mth.ceil(d3 / 0.25D));

                for (int l = 0; l < i; ++l) {
                    for (int i1 = 0; i1 < j; ++i1) {
                        for (int j1 = 0; j1 < k; ++j1) {
                            double d4 = ((double) l + 0.5D) / (double) i;
                            double d5 = ((double) i1 + 0.5D) / (double) j;
                            double d6 = ((double) j1 + 0.5D) / (double) k;
                            double d7 = d4 * d1 + p_228348_3_;
                            double d8 = d5 * d2 + p_228348_5_;
                            double d9 = d6 * d3 + p_228348_7_;

                            Minecraft.getInstance()
                                    .particleEngine
                                    .add((new TerrainParticle(
                                            (ClientLevel) world,
                                            (double) pos.getX() + d7,
                                            (double) pos.getY() + d8,
                                            (double) pos.getZ() + d9,
                                            d4 - 0.5D,
                                            d5 - 0.5D,
                                            d6 - 0.5D,
                                            state,
                                            pos)));
                        }
                    }
                }
            });
        }

        return true;
    }

    public TextureAtlasSprite getMissingIcon() {
        return Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS)
                .missingSprite();
    }

    public String getModeKey() {
        return getKeyName(modeMenu);
    }

    public ChiselToolType getDrawnTool() {
        return lastTool;
    }

    public boolean holdingShift() {
        return (!Minecraft.getInstance().options.keyShift.isUnbound()
                        && Minecraft.getInstance().options.keyShift.isDown())
                || Minecraft.getInstance().hasShiftDown();
    }

    public String getKeyName(KeyMapping bind) {
        if (bind == null) {
            return LocalStrings.noBind.getLocal();
        }

        if (bind.isUnbound() && bind.getDefaultKey().getValue() != 0) {
            // TODO: This previously changed the resulting string to something easier to understand. Not sure that is
            // still needed.
            return DeprecationHelper.translateToLocal(bind.saveString());
        }

        if (bind.isUnbound()) {
            return '"' + DeprecationHelper.translateToLocal(bind.getName());
        }

        return makeMoreFrendly(bind.saveString());
    }

    private String makeMoreFrendly(String displayName) {
        return DeprecationHelper.translateToLocal(displayName)
                .replace("LMENU", LocalStrings.leftAlt.getLocal())
                .replace("RMENU", LocalStrings.rightAlt.getLocal())
                .replace("LSHIFT", LocalStrings.leftShift.getLocal())
                .replace("RSHIFT", LocalStrings.rightShift.getLocal())
                .replace("key.keyboard.", "");
    }
}
