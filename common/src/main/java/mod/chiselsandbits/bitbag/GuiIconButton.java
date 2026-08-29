package mod.chiselsandbits.bitbag;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;

public class GuiIconButton extends Button {
    TextureAtlasSprite icon;

    public GuiIconButton(final int x, final int y, final TextureAtlasSprite icon, Button.OnPress pressedAction) {
        super(x, y, 18, 18, Component.literal(""), pressedAction, Button.DEFAULT_NARRATION);
        this.icon = icon;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int i, int j, float f) {
        extractDefaultSprite(guiGraphics);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, getX() + 1, getY() + 1, 16, 16);
    }

    public interface OnToolTip {

        void onToolTip(double mouseX, double mouseY);
    }
}
