package mod.chiselsandbits.crafting;

import mod.chiselsandbits.chiseledblock.ItemBlockChiseled;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.components.ChiseledData;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.registry.ModRecipeSerializers;
import mod.chiselsandbits.utils.ItemStackUtils;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class StackableCrafting extends CustomRecipe {

    public StackableCrafting() {
        super();
    }

    @Override
    public boolean matches(final CraftingInput craftingInv, final Level worldIn) {
        ItemStack target = null;

        for (int x = 0; x < craftingInv.size(); x++) {
            final ItemStack f = craftingInv.getItem(x);
            if (ModUtil.isEmpty(f)) {
                continue;
            }

            if (target == null) {
                target = f;
            } else {
                return false;
            }
        }

        return target != null && ModUtil.hasChiseledData(target) && target.getItem() instanceof ItemBlockChiseled;
    }

    @Override
    public ItemStack assemble(final CraftingInput craftingInv) {
        ItemStack target = null;

        for (int x = 0; x < craftingInv.size(); x++) {
            final ItemStack f = craftingInv.getItem(x);
            if (ModUtil.isEmpty(f)) {
                continue;
            }

            if (target == null) {
                target = f;
            } else {
                return ModUtil.getEmptyStack();
            }
        }

        if (target == null || !ModUtil.hasChiseledData(target) || !(target.getItem() instanceof ItemBlockChiseled)) {
            return ModUtil.getEmptyStack();
        }

        return getSortedVersion(target);
    }

    private ItemStack getSortedVersion(final @NotNull ItemStack stack) {
        final NBTBlobConverter tmp = new NBTBlobConverter();
        final ChiseledData data = NBTBlobConverter.getComponent(stack);
        if (data == null) {
            return ModUtil.getEmptyStack();
        }
        tmp.readChisleData(data, VoxelBlob.VERSION_ANY);

        VoxelBlob bestBlob = tmp.getBlob();
        byte[] bestValue = bestBlob.toLegacyByteArray();

        VoxelBlob lastBlob = bestBlob;
        for (int x = 0; x < 34; x++) {
            lastBlob = lastBlob.spin(Axis.Y);
            final byte[] aValue = lastBlob.toLegacyByteArray();

            if (arrayCompare(bestValue, aValue)) {
                bestBlob = lastBlob;
                bestValue = aValue;
            }
        }

        tmp.setBlob(bestBlob);
        return tmp.getItemStack(false);
    }

    private boolean arrayCompare(final byte[] bestValue, final byte[] aValue) {
        if (aValue.length < bestValue.length) {
            return true;
        }

        if (aValue.length > bestValue.length) {
            return false;
        }

        for (int x = 0; x < aValue.length; x++) {
            if (aValue[x] < bestValue[x]) {
                return true;
            }

            if (aValue[x] > bestValue[x]) {
                return false;
            }
        }

        return false;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingInput inv) {
        final NonNullList<ItemStack> aitemstack = NonNullList.withSize(inv.size(), ItemStack.EMPTY);

        for (int i = 0; i < aitemstack.size(); ++i) {
            final ItemStack itemstack = ModUtil.nonNull(inv.getItem(i));
            aitemstack.set(i, ItemStackUtils.getContainerItem(itemstack));
        }

        return aitemstack;
    }

    @Override
    public RecipeSerializer<StackableCrafting> getSerializer() {
        return ModRecipeSerializers.STACKABLE_CRAFTING.get();
    }
}
