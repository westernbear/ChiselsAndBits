package mod.chiselsandbits.bitbag;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

public class GuiBagFontRenderer extends Font {
    Font talkto;

    int offsetX, offsetY;
    float scale;

    public GuiBagFontRenderer(final Font src, final int bagStackSize) {
        super(src.fonts, false);
        talkto = src;

        if (bagStackSize < 100) {
            scale = 1f;
        } else if (bagStackSize >= 100) {
            scale = 0.75f;
            offsetX = 3;
            offsetY = 2;
        }
    }

    @Override
    public int width(String text) {
        text = convertText(text);
        return talkto.width(text);
    }

    @Override
    protected int drawInternal(
            String text,
            float x,
            float y,
            int color,
            boolean dropShadow,
            Matrix4f matrix,
            MultiBufferSource multiBufferSource,
            DisplayMode displayMode,
            int j,
            int k,
            boolean bl2) {
        final PoseStack stack = new PoseStack();
        final Matrix4f original = new Matrix4f(matrix);

        try {
            stack.last().pose().mul(matrix);
            stack.scale(scale, scale, scale);

            x /= scale;
            y /= scale;
            x += offsetX;
            y += offsetY;

            return super.drawInternal(text, x, y, color, dropShadow, matrix, multiBufferSource, displayMode, j, k, bl2);
        } finally {
            matrix.set(original);
        }
    }

    @Override
    public int drawInBatch(
            String text,
            float x,
            float y,
            int color,
            boolean dropShadow,
            Matrix4f matrix,
            MultiBufferSource buffer,
            DisplayMode displayMode,
            int colorBackgroundIn,
            int packedLight,
            boolean transparentIn) {
        final PoseStack stack = new PoseStack();
        final Matrix4f original = new Matrix4f(matrix);

        try {
            stack.last().pose().mul(matrix);
            stack.scale(scale, scale, scale);

            x /= scale;
            y /= scale;
            x += offsetX;
            y += offsetY;

            return super.drawInBatch(
                    text,
                    x,
                    y,
                    color,
                    dropShadow,
                    matrix,
                    buffer,
                    displayMode,
                    colorBackgroundIn,
                    packedLight,
                    transparentIn);
        } finally {
            matrix.set(original);
        }
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
