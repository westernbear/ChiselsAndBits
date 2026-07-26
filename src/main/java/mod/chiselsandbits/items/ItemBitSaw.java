package mod.chiselsandbits.items;

import java.util.List;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.LocalStrings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ItemBitSaw extends Item {

    public ItemBitSaw(Tier tier, Item.Properties properties) {
        super(properties);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(
            final ItemStack stack, final Level worldIn, final List<Component> tooltip, final TooltipFlag advanced) {
        super.appendHoverText(stack, worldIn, tooltip, advanced);
        ChiselsAndBits.getConfig().getCommon().helpText(LocalStrings.HelpBitSaw, tooltip);
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
