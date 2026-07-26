package mod.chiselsandbits.core;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import mod.chiselsandbits.bitbag.BagGui;
import mod.chiselsandbits.client.gui.SpriteIconPositioning;
import mod.chiselsandbits.core.textures.IconSpriteUploader;
import mod.chiselsandbits.modes.ChiselMode;
import mod.chiselsandbits.modes.IToolMode;
import mod.chiselsandbits.modes.PositivePatternMode;
import mod.chiselsandbits.modes.TapeMeasureModes;
import mod.chiselsandbits.printer.ChiselPrinterScreen;
import mod.chiselsandbits.registry.ModContainerTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

public class ChiselsAndBitsClient {

    private static IconSpriteUploader spriteUploader;

    @Environment(EnvType.CLIENT)
    public static void onClientInit() {

        ClientSide.instance.preInit();
        ClientSide.instance.init();
        ClientSide.instance.postInit();
        MenuScreens.register(ModContainerTypes.BAG_CONTAINER.get(), BagGui::new);
        MenuScreens.register(ModContainerTypes.CHISEL_STATION_CONTAINER.get(), ChiselPrinterScreen::new);
    }

    @Environment(EnvType.CLIENT)
    public static void registerIconTextures() {
        // AtlasManager owns block-atlas reloads in 26.2. The C&B atlas source is merged through
        // assets/minecraft/atlases/blocks.json and the existing post-stitch callback refreshes these handles.
        spriteUploader = new IconSpriteUploader();
    }

    @Environment(EnvType.CLIENT)
    public static void retrieveRegisteredIconSprites(TextureAtlas map) {
        if (!map.location().equals(IconSpriteUploader.TEXTURE_MAP_NAME)) {
            return;
        }
        ClientSide.swapIcon = spriteUploader.getSprite(Identifier.fromNamespaceAndPath("chiselsandbits", "swap"));
        ClientSide.placeIcon = spriteUploader.getSprite(Identifier.fromNamespaceAndPath("chiselsandbits", "place"));
        ClientSide.undoIcon = spriteUploader.getSprite(Identifier.fromNamespaceAndPath("chiselsandbits", "undo"));
        ClientSide.redoIcon = spriteUploader.getSprite(Identifier.fromNamespaceAndPath("chiselsandbits", "redo"));
        ClientSide.trashIcon = spriteUploader.getSprite(Identifier.fromNamespaceAndPath("chiselsandbits", "trash"));
        ClientSide.sortIcon = spriteUploader.getSprite(Identifier.fromNamespaceAndPath("chiselsandbits", "sort"));
        ClientSide.roll_x = spriteUploader.getSprite(Identifier.fromNamespaceAndPath("chiselsandbits", "roll_x"));
        ClientSide.roll_z = spriteUploader.getSprite(Identifier.fromNamespaceAndPath("chiselsandbits", "roll_z"));
        ClientSide.white = spriteUploader.getSprite(Identifier.fromNamespaceAndPath("chiselsandbits", "white"));

        for (final ChiselMode mode : ChiselMode.values()) {
            loadIcon(spriteUploader, mode);
        }

        for (final PositivePatternMode mode : PositivePatternMode.values()) {
            loadIcon(spriteUploader, mode);
        }

        for (final TapeMeasureModes mode : TapeMeasureModes.values()) {
            loadIcon(spriteUploader, mode);
        }
    }

    @Environment(EnvType.CLIENT)
    private static void loadIcon(final IconSpriteUploader spriteUploader, final IToolMode mode) {
        final SpriteIconPositioning sip = new SpriteIconPositioning();

        final Identifier sprite =
                Identifier.fromNamespaceAndPath("chiselsandbits", mode.name().toLowerCase());
        final Identifier png = Identifier.fromNamespaceAndPath(
                "chiselsandbits", "textures/icons/" + mode.name().toLowerCase() + ".png");

        sip.sprite = spriteUploader.getSprite(sprite);

        try (final var imageStream = Minecraft.getInstance()
                .getResourceManager()
                .getResource(png)
                .get()
                .open()) {
            final BufferedImage bi = ImageIO.read(imageStream);

            int bottom = 0;
            int right = 0;
            sip.left = bi.getWidth();
            sip.top = bi.getHeight();

            for (int x = 0; x < bi.getWidth(); x++) {
                for (int y = 0; y < bi.getHeight(); y++) {
                    final int color = bi.getRGB(x, y);
                    final int a = color >> 24 & 0xff;
                    if (a > 0) {
                        sip.left = Math.min(sip.left, x);
                        right = Math.max(right, x);

                        sip.top = Math.min(sip.top, y);
                        bottom = Math.max(bottom, y);
                    }
                }
            }

            sip.height = bottom - sip.top + 1;
            sip.width = right - sip.left + 1;

            sip.left /= bi.getWidth();
            sip.width /= bi.getWidth();
            sip.top /= bi.getHeight();
            sip.height /= bi.getHeight();
        } catch (final IOException e) {
            sip.height = 1;
            sip.width = 1;
            sip.left = 0;
            sip.top = 0;
        }

        ClientSide.instance.setIconForMode(mode, sip);
    }
}
