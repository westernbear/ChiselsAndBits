package mod.chiselsandbits.items;

import dev.architectury.injectables.annotations.Environment;
import dev.architectury.utils.Env;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.LocalStrings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemMagnifyingGlass extends Item {

    public ItemMagnifyingGlass(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    @Environment(Env.CLIENT)
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final TooltipDisplay display,
            final Consumer<Component> tooltip,
            final TooltipFlag advanced) {
        super.appendHoverText(stack, context, display, tooltip, advanced);
        final List<Component> helpText = new ArrayList<>();
        ChiselsAndBits.getConfig().getCommon().helpText(LocalStrings.HelpMagnifyingGlass, helpText);
        helpText.forEach(tooltip);
    }
}
