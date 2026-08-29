package mod.chiselsandbits.bitbag;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class GuiBagFontRenderer {
    private final Font font;
    private final int offsetX;
    private final int offsetY;
    private final float scale;

    public GuiBagFontRenderer(final Font font, final int bagStackSize) {
        this.font = font;
        scale = bagStackSize < 100 ? 1f : 0.75f;
        offsetX = bagStackSize < 100 ? 0 : 3;
        offsetY = bagStackSize < 100 ? 0 : 2;
    }

    public void extractCount(final GuiGraphicsExtractor graphics, final int count, final int x, final int y) {
        if (count == 1) {
            return;
        }

        final String text = convertText(Integer.toString(count));
        final int textX = x + 17 - font.width(text);
        final int textY = y + 9;

        graphics.pose().pushMatrix();
        graphics.pose().translate(textX, textY);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, offsetX, offsetY, -1, true);
        graphics.pose().popMatrix();
    }

    private String convertText(final String text) {
        try {
            final int value = Integer.parseInt(text);

            if (value >= 1000) {
                return value / 1000 + "k";
            }

            return text;
        } catch (final NumberFormatException e) {
            return text;
        }
    }
}
