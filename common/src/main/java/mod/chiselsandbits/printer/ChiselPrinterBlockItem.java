package mod.chiselsandbits.printer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.LocalStrings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class ChiselPrinterBlockItem extends BlockItem {
    public ChiselPrinterBlockItem(final Block block, final Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final TooltipDisplay display,
            final Consumer<Component> tooltip,
            final TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        final List<Component> helpText = new ArrayList<>();
        ChiselsAndBits.getConfig().getCommon().helpText(LocalStrings.ChiselStationHelp, helpText);
        helpText.forEach(tooltip);
    }
}
