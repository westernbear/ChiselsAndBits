package mod.chiselsandbits.printer;

import java.util.Objects;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ChiselPrinterScreen extends AbstractContainerScreen<ChiselPrinterContainer> {

    private static final Identifier GUI_TEXTURES =
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/container/chisel_printer.png");

    public ChiselPrinterScreen(
            final ChiselPrinterContainer screenContainer, final Inventory inv, final Component titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void init() {
        super.init();

        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                GUI_TEXTURES,
                this.leftPos,
                this.topPos,
                0,
                0,
                this.imageWidth,
                this.imageHeight,
                256,
                256);

        if (this.menu.getToolStack().isEmpty()) {
            return;
        }

        guiGraphics.item(
                Objects.requireNonNull(this.minecraft.player),
                this.menu.getToolStack(),
                this.leftPos + 81,
                this.topPos + 47,
                0);

        int scaledProgress = this.menu.getChiselProgressionScaled();
        guiGraphics.nextStratum();
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                GUI_TEXTURES,
                this.leftPos + 73 + 10 + scaledProgress,
                this.topPos + 49,
                this.imageWidth + scaledProgress,
                0,
                16 - scaledProgress,
                16,
                256,
                256);
    }
}
