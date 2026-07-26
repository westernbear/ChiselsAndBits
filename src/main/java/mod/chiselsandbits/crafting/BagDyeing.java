package mod.chiselsandbits.crafting;

import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.items.ItemBitBag;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.registry.ModRecipeSerializers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class BagDyeing extends CustomRecipe {

    public BagDyeing() {
        super();
    }

    @Override
    public ItemStack assemble(final CraftingInput inv) {
        dyed_output output = getOutput(inv);

        if (output != null) {
            ModItems.ITEM_BIT_BAG_DEFAULT.get();
            return ItemBitBag.dyeBag(output.bag, output.color);
        }

        return ModUtil.getEmptyStack();
    }

    private dyed_output getOutput(final CraftingInput inv) {
        ItemStack bag = null;
        ItemStack dye = null;

        for (int x = 0; x < inv.size(); ++x) {
            ItemStack is = inv.getItem(x);
            if (is != null && !ModUtil.isEmpty(is)) {
                if (is.getItem() == Items.WATER_BUCKET || getDye(is) != null) {
                    if (dye == null) {
                        dye = is;
                    } else {
                        return null;
                    }
                } else if (is.getItem() instanceof ItemBitBag) {
                    if (bag == null) {
                        bag = is;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        }

        if (bag != null && dye != null) {
            return new dyed_output(bag, getDye(dye));
        }

        return null;
    }

    private DyeColor getDye(ItemStack is) {
        return is.get(DataComponents.DYE);
    }

    @Override
    public boolean matches(final CraftingInput inv, final Level worldIn) {
        return getOutput(inv) != null;
    }

    @Override
    public RecipeSerializer<BagDyeing> getSerializer() {
        return ModRecipeSerializers.BAG_DYEING.get();
    }

    private static class dyed_output {
        ItemStack bag;
        DyeColor color;

        public dyed_output(ItemStack bag, DyeColor dye) {
            this.bag = bag;
            this.color = dye;
        }
    }
}
