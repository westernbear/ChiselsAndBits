package mod.chiselsandbits.items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mod.chiselsandbits.bitbag.BagInventory;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.network.packets.PacketOpenBagGui;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.render.helpers.SimpleInstanceCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ItemBitBag extends Item {

    public static final int INTS_PER_BIT_TYPE = 2;
    public static final int OFFSET_STATE_ID = 0;
    public static final int OFFSET_QUANTITY = 1;

    SimpleInstanceCache<ItemStack, List<Component>> tooltipCache = new SimpleInstanceCache<>(null, new ArrayList<>());

    public ItemBitBag(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void cleanupInventory(final Player player, final ItemStack is) {
        if (is != null && is.getItem() instanceof ItemChiseledBit) {
            // time to clean up your inventory...
            final Container inv = player.getInventory();
            final List<ItemBitBag.BagPos> bags = ItemBitBag.getBags(inv);

            int firstSeen = -1;
            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                int actingSlot = slot;
                @NotNull ItemStack which = ModUtil.nonNull(inv.getItem(actingSlot));

                if (which != null
                        && which.getItem() == is.getItem()
                        && (ItemChiseledBit.sameBit(which, ItemChiseledBit.getStackState(is)))) {
                    if (actingSlot == player.getInventory().getSelectedSlot()) {
                        if (firstSeen != -1) {
                            actingSlot = firstSeen;
                        } else {
                            continue;
                        }
                    }

                    which = ModUtil.nonNull(inv.getItem(actingSlot));

                    if (firstSeen == -1) {
                        firstSeen = actingSlot;
                    } else {
                        for (final ItemBitBag.BagPos i : bags) {
                            which = i.inv.insertItem(which);
                            if (ModUtil.isEmpty(which)) {
                                inv.setItem(actingSlot, which);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public static List<BagPos> getBags(final Container inv) {
        final ArrayList<BagPos> bags = new ArrayList<BagPos>();
        for (int x = 0; x < inv.getContainerSize(); x++) {
            final ItemStack which = inv.getItem(x);
            if (which != null && which.getItem() instanceof ItemBitBag) {
                bags.add(new BagPos(new BagInventory(which)));
            }
        }
        return bags;
    }

    public static ItemStack dyeBag(ItemStack bag, DyeColor color) {
        final ItemStack copy = bag.copy();

        if (color == null && bag.getItem() == ModItems.ITEM_BIT_BAG_DYED.get()) {
            final ItemStack unColoredStack = copy.transmuteCopy(ModItems.ITEM_BIT_BAG_DEFAULT.get());
            unColoredStack.remove(DataComponents.DYED_COLOR);
            return unColoredStack;
        } else if (color != null) {
            ItemStack coloredStack = copy;
            if (coloredStack.getItem() == ModItems.ITEM_BIT_BAG_DEFAULT.get()) {
                coloredStack = copy.transmuteCopy(ModItems.ITEM_BIT_BAG_DYED.get());
            }

            coloredStack.set(DataComponents.DYED_COLOR, new DyedItemColor(color.getTextureDiffuseColor()));
            return coloredStack;
        }

        return copy;
    }

    public static DyeColor getDyedColor(ItemStack stack) {
        if (stack.getItem() != ModItems.ITEM_BIT_BAG_DYED.get()) {
            return null;
        }

        final DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
        if (dyedColor != null) {
            for (final DyeColor color : DyeColor.values()) {
                if (dyedColor.rgb() == color.getTextureDiffuseColor()) {
                    return color;
                }
            }
        }

        return null;
    }

    @Override
    public Component getName(final ItemStack stack) {
        DyeColor color = getDyedColor(stack);
        final Component parent = super.getName(stack);
        if (parent instanceof MutableComponent && color != null) {
            return ((MutableComponent) parent)
                    .append(" - ")
                    .append(Component.translatable("chiselsandbits.color." + color.getName()));
        } else {
            return super.getName(stack);
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final TooltipDisplay display,
            final Consumer<Component> tooltip,
            final TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        final List<Component> helpText = new ArrayList<>();
        ChiselsAndBits.getConfig().getCommon().helpText(LocalStrings.HelpBitBag, helpText);
        helpText.forEach(tooltip);

        if (tooltipCache.needsUpdate(stack)) {
            final BagInventory bi = new BagInventory(stack);
            tooltipCache.updateCachedValue(bi.listContents(new ArrayList<>()));
        }

        final List<Component> details = tooltipCache.getCached();
        if (details.size() <= 2 || ClientSide.instance.holdingShift()) {
            details.forEach(tooltip);
        } else {
            tooltip.accept(Component.literal(LocalStrings.ShiftDetails.getLocal()));
        }
    }

    @Override
    public InteractionResult use(final Level worldIn, final Player playerIn, final InteractionHand hand) {
        if (worldIn.isClientSide()) {
            ChiselsAndBits.getNetworkChannel().sendToServer(new PacketOpenBagGui());
        }

        return InteractionResult.SUCCESS;
    }

    public static class BagPos {
        public final BagInventory inv;

        public BagPos(final BagInventory bagInventory) {
            inv = bagInventory;
        }
    }
}
