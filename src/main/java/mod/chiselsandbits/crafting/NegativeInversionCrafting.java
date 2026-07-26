package mod.chiselsandbits.crafting;

import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.components.ChiseledData;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.registry.ModDataComponents;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.registry.ModRecipeSerializers;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class NegativeInversionCrafting extends CustomRecipe {

    public NegativeInversionCrafting() {
        super();
    }

    @Override
    public boolean matches(final CraftingInput craftingInv, final Level worldIn) {
        return (craftingInv.width() > 1 || craftingInv.height() > 1)
                && analzyeCraftingInventory(craftingInv, true) != null;
    }

    public ItemStack analzyeCraftingInventory(final CraftingInput craftingInv, final boolean generatePattern) {
        ItemStack targetA = null;
        ItemStack targetB = null;

        for (int x = 0; x < craftingInv.size(); x++) {
            final ItemStack f = craftingInv.getItem(x);
            if (f.isEmpty()) {
                continue;
            }

            if (f.getItem().equals(ModItems.ITEM_NEGATIVE_PRINT.get())) {
                if (ModItems.ITEM_NEGATIVE_PRINT.get().isWritten(f)) {
                    if (targetA != null) {
                        return null;
                    }

                    targetA = f;
                } else {
                    if (targetB != null) {
                        return null;
                    }

                    targetB = f;
                }
            } else if (!ModUtil.isEmpty(f)) {
                return null;
            }
        }

        if (targetA != null && targetB != null) {
            if (generatePattern) {
                return targetA;
            }

            final NBTBlobConverter tmp = new NBTBlobConverter();
            final ChiseledData sourceData = NBTBlobConverter.getComponent(targetA);
            if (sourceData == null) {
                return null;
            }
            tmp.readChisleData(sourceData, VoxelBlob.VERSION_ANY);

            final VoxelBlob bestBlob = tmp.getBlob();
            bestBlob.binaryReplacement(ModUtil.getStateId(Blocks.STONE.defaultBlockState()), 0);

            tmp.setBlob(bestBlob);

            final ItemStack outputPattern = new ItemStack(ModItems.ITEM_NEGATIVE_PRINT_WRITTEN.get());
            final ChiseledData outputData = tmp.toComponent(false);
            if (outputData == null) {
                return null;
            }
            outputPattern.set(ModDataComponents.CHISELED_DATA, outputData);
            ModUtil.setSide(outputPattern, ModUtil.getSide(targetA));

            return outputPattern;
        }

        return null;
    }

    @Override
    public ItemStack assemble(final CraftingInput craftingInv) {
        return analzyeCraftingInventory(craftingInv, false);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingInput craftingInv) {
        final NonNullList<ItemStack> aitemstack = NonNullList.withSize(craftingInv.size(), ItemStack.EMPTY);

        for (int i = 0; i < aitemstack.size(); ++i) {
            final ItemStack itemstack = craftingInv.getItem(i);
            if (itemstack != null
                    && itemstack.getItem() == ModItems.ITEM_NEGATIVE_PRINT_WRITTEN.get()
                    && ModUtil.hasChiseledData(itemstack)) {
                ModUtil.adjustStackSize(itemstack, 1);
            }
        }

        return aitemstack;
    }

    @Override
    public RecipeSerializer<NegativeInversionCrafting> getSerializer() {
        return ModRecipeSerializers.NEGATIVE_INVERSION_CRAFTING.get();
    }
}
