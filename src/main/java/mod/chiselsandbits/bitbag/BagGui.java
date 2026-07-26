package mod.chiselsandbits.bitbag;

import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.network.packets.PacketBagGui;
import mod.chiselsandbits.network.packets.PacketClearBagGui;
import mod.chiselsandbits.network.packets.PacketSortBagGui;
import mod.chiselsandbits.registry.ModItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BagGui extends AbstractContainerScreen<BagContainer> {

    private static final Identifier BAG_GUI_TEXTURE =
            Identifier.fromNamespaceAndPath(ChiselsAndBits.MODID, "textures/gui/container/bitbag.png");

    private static GuiBagFontRenderer specialFontRenderer = null;
    boolean requireConfirm = true;
    boolean dontThrow = false;
    private GuiIconButton trashBtn;
    private GuiIconButton sortBtn;
    private Slot hoveredBitSlot = null;

    public BagGui(final BagContainer container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title, 176, 239);
    }

    @Override
    protected void init() {
        super.init();
        trashBtn = addRenderableWidget(new GuiIconButton(leftPos - 18, topPos, ClientSide.trashIcon, button -> {
            if (requireConfirm) {
                dontThrow = true;
                if (isValidBitItem()) {
                    requireConfirm = false;
                }
            } else {
                requireConfirm = true;
                ChiselsAndBits.getNetworkChannel().sendToServer(new PacketClearBagGui(getInHandItem()));
                dontThrow = false;
            }
        }));

        sortBtn = addRenderableWidget(
                new GuiIconButton(leftPos - 18, topPos + 18, ClientSide.sortIcon, new Button.OnPress() {
                    @Override
                    public void onPress(final Button button) {
                        ChiselsAndBits.getNetworkChannel().sendToServer(new PacketSortBagGui());
                    }
                }));
    }

    BagContainer getBagContainer() {
        return menu;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float ticks) {
        if (trashBtn.isMouseOver(mouseX, mouseY)) {
            if (isValidBitItem()) {
                final String msgNotConfirm = ModUtil.notEmpty(getInHandItem())
                        ? LocalStrings.TrashItem.getLocal(
                                getInHandItem().getHoverName().getString())
                        : LocalStrings.Trash.getLocal();
                final String msgConfirm = ModUtil.notEmpty(getInHandItem())
                        ? LocalStrings.ReallyTrashItem.getLocal(
                                getInHandItem().getHoverName().getString())
                        : LocalStrings.ReallyTrash.getLocal();
                trashBtn.setTooltip(Tooltip.create(Component.literal(requireConfirm ? msgNotConfirm : msgConfirm)));
            } else {
                trashBtn.setTooltip(Tooltip.create(Component.literal(LocalStrings.TrashInvalidItem.getLocal(
                        getInHandItem().getHoverName().getString()))));
            }
        } else {
            requireConfirm = true;
        }

        if (sortBtn.isMouseOver(mouseX, mouseY)) {
            sortBtn.setTooltip(Tooltip.create(Component.literal(LocalStrings.Sort.getLocal())));
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, ticks);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float ticks) {
        super.extractBackground(guiGraphics, mouseX, mouseY, ticks);
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BAG_GUI_TEXTURE,
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                256);
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (specialFontRenderer == null) {
            specialFontRenderer = new GuiBagFontRenderer(
                    font, ChiselsAndBits.getConfig().getServer().bagStackSize.get());
        }

        hoveredBitSlot = null;
        for (final Slot slot : getBagContainer().customSlots) {
            if (!slot.isActive()) {
                continue;
            }

            extractBitSlot(guiGraphics, slot);

            if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                hoveredBitSlot = slot;
                guiGraphics.fillGradient(slot.x, slot.y, slot.x + 16, slot.y + 16, -2130706433, -2130706433);
            }
        }

        super.extractSlots(guiGraphics, mouseX, mouseY);
    }

    private void extractBitSlot(final GuiGraphicsExtractor guiGraphics, final Slot slot) {
        final ItemStack itemStack = slot.getItem();
        if (itemStack.isEmpty()) {
            final Identifier icon = slot.getNoItemIcon();
            if (icon != null) {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, slot.x, slot.y, 16, 16);
            }
            return;
        }

        final int seed = slot.x + slot.y * imageWidth;
        if (slot.isFake()) {
            guiGraphics.fakeItem(itemStack, slot.x, slot.y, seed);
        } else {
            guiGraphics.item(itemStack, slot.x, slot.y, seed);
        }

        guiGraphics.itemDecorations(font, itemStack, slot.x, slot.y, itemStack.getCount() == 1 ? null : "");
        specialFontRenderer.extractCount(guiGraphics, itemStack.getCount(), slot.x, slot.y);
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        final boolean duplicateButton = minecraft.options.keyPickItem.matchesMouse(event);

        Slot slot = hoveredSlot;
        if (slot == null) {
            slot = hoveredBitSlot;
        }
        if (slot != null && slot.container instanceof TargetedInventory) {
            final PacketBagGui bagGuiPacket =
                    new PacketBagGui(slot.index, event.button(), duplicateButton, ClientSide.instance.holdingShift());
            bagGuiPacket.doAction(ClientSide.instance.getPlayer());

            ChiselsAndBits.getNetworkChannel().sendToServer(bagGuiPacket);

            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private ItemStack getInHandItem() {
        return getBagContainer().getCarried();
    }

    private boolean isValidBitItem() {
        return ModUtil.isEmpty(getInHandItem()) || getInHandItem().getItem() == ModItems.ITEM_BLOCK_BIT.get();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int x, int y) {
        guiGraphics.text(
                font,
                Language.getInstance()
                        .getVisualOrder(ModItems.ITEM_BIT_BAG_DEFAULT.get().getName(ModUtil.getEmptyStack())),
                8,
                6,
                0xFF404040,
                false);
        guiGraphics.text(font, I18n.get("container.inventory"), 8, imageHeight - 93, 0xFF404040, false);
    }
}
