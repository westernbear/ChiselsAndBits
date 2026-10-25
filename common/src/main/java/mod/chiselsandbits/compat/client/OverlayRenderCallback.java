package mod.chiselsandbits.compat.client;

import com.mojang.blaze3d.platform.Window;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface OverlayRenderCallback {
    Event<OverlayRenderCallback> EVENT = EventFactory.of(callbacks -> (guiGraphics, partialTicks, window, type) -> {
        for (final OverlayRenderCallback callback : callbacks) {
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
