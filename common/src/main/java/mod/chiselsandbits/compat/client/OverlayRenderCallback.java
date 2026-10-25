package mod.chiselsandbits.compat.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface OverlayRenderCallback {
    ClientEvent<OverlayRenderCallback> EVENT = ClientEvent.arrayBacked(
            OverlayRenderCallback.class, callbacks -> (guiGraphics, partialTicks, window, type) -> {
                for (OverlayRenderCallback callback : callbacks) {
                    if (callback.onOverlayRender(guiGraphics, partialTicks, window, type)) {
                        return true;
                    }
                }
                return false;
            });

    boolean onOverlayRender(GuiGraphicsExtractor guiGraphics, float partialTicks, Window window, Types type);

    enum Types {
        AIR,
        CROSSHAIRS,
        PLAYER_HEALTH
    }
}
