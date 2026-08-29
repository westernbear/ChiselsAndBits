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
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemBitSaw extends Item {

    public ItemBitSaw(ToolMaterial material, Item.Properties properties) {
        super(properties.tool(material, mod.chiselsandbits.registry.ModTags.Blocks.CHISELED_BLOCK, 0.0F, -3.0F, 0.0F));
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
        ChiselsAndBits.getConfig().getCommon().helpText(LocalStrings.HelpBitSaw, helpText);
        helpText.forEach(tooltip);
    }

    public ItemStack getContainerItem(final ItemStack itemStack) {
        if (ChiselsAndBits.getConfig().getServer().damageTools.get()) {
            itemStack.setDamageValue(itemStack.getDamageValue() + 1);
            if (itemStack.getDamageValue() == itemStack.getMaxDamage()) {
                return ItemStack.EMPTY;
            }
        }

        return itemStack.copy();
    }
}
