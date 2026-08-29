package mod.chiselsandbits.core;

import mod.chiselsandbits.client.IClientSide;
import mod.chiselsandbits.helpers.DeprecationHelper;
import mod.chiselsandbits.helpers.ModUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Loader-neutral client-side holder and shared client utilities. */
public final class ClientSide {

    public static IClientSide instance;

    public static TextureAtlasSprite undoIcon;
    public static TextureAtlasSprite redoIcon;
    public static TextureAtlasSprite trashIcon;
    public static TextureAtlasSprite sortIcon;
    public static TextureAtlasSprite swapIcon;
    public static TextureAtlasSprite placeIcon;
    public static TextureAtlasSprite roll_x;
    public static TextureAtlasSprite roll_z;
    public static TextureAtlasSprite white;

    private ClientSide() {}

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
        if (!(player instanceof LocalPlayer)) {
            return false;
        }

        if (player.level().isClientSide()) {
            return !getOffGridPlacementKey().isUnbound()
                    && getOffGridPlacementKey().isDown();
        }

        throw new RuntimeException("checking keybinds on server.");
    }

    public static KeyMapping getOffGridPlacementKey() {
        if (instance == null) {
            return Minecraft.getInstance().options.keyShift;
        }
        final KeyMapping offgrid = instance.getOffGridPlacementMapping();
        if (offgrid.isUnbound() && offgrid.isDefault()) {
            return Minecraft.getInstance().options.keyShift;
        }
        return offgrid;
    }
}
