package mod.chiselsandbits.client;

import mod.chiselsandbits.api.ModKeyBinding;
import mod.chiselsandbits.chiseledblock.data.BitLocation;
import mod.chiselsandbits.client.gui.SpriteIconPositioning;
import mod.chiselsandbits.helpers.ChiselToolType;
import mod.chiselsandbits.modes.IToolMode;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/** Client-side gameplay surface implemented by loader-specific modules. */
public interface IClientSide {

    KeyMapping getOffGridPlacementMapping();

    KeyMapping getKeyBinding(ModKeyBinding modKeyBinding);

    void preInit();

    void init();

    void postInit();

    SpriteIconPositioning getIconForMode(IToolMode mode);

    void setIconForMode(IToolMode mode, SpriteIconPositioning positioning);

    ChiselToolType getHeldToolType(InteractionHand hand);

    InteractionResult drawingInteractionPrevention(Player player, InteractionHand hand, BlockHitResult hitResult);

    Player getPlayer();

    BitLocation getStartPos();

    void pointAt(ChiselToolType tool, BitLocation loc, InteractionHand hand);

    void setLastTool(@NotNull ChiselToolType lastTool);

    TextureAtlasSprite getMissingIcon();

    String getModeKey();

    ChiselToolType getDrawnTool();

    boolean holdingShift();

    String getKeyName(KeyMapping bind);

    boolean addBlockDestroyEffects(@NotNull Level world, @NotNull BlockPos pos, BlockState state);

    void clearTapeMeasures();
}
