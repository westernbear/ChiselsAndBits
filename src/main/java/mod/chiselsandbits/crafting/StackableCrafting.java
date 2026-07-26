package mod.chiselsandbits.crafting;

import mod.chiselsandbits.chiseledblock.ItemBlockChiseled;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.registry.ModRecipeSerializers;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class StackableCrafting extends CustomRecipe {

    public StackableCrafting(CraftingBookCategory name) {
        super(name);
    }

    @Override
    public boolean matches(final CraftingContainer craftingInv, final Level worldIn) {
        ItemStack target = null;

        for (int x = 0; x < craftingInv.getContainerSize(); x++) {
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

        return target != null && target.hasTag() && target.getItem() instanceof ItemBlockChiseled;
    }

    @Override
    public ItemStack assemble(CraftingContainer craftingInv, RegistryAccess registryAccess) {
        ItemStack target = null;

        for (int x = 0; x < craftingInv.getContainerSize(); x++) {
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

        if (target == null || !target.hasTag() || !(target.getItem() instanceof ItemBlockChiseled)) {
            return ModUtil.getEmptyStack();
        }

        return getSortedVersion(target);
    }

    private ItemStack getSortedVersion(final @NotNull ItemStack stack) {
        final NBTBlobConverter tmp = new NBTBlobConverter();
        tmp.readChisleData(ModUtil.getSubCompound(stack, ModUtil.NBT_BLOCKENTITYTAG, false), VoxelBlob.VERSION_ANY);

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
    public boolean canCraftInDimensions(final int width, final int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ModUtil.getEmptyStack();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingContainer inv) {
        final NonNullList<ItemStack> aitemstack = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);

        for (int i = 0; i < aitemstack.size(); ++i) {
            final ItemStack itemstack = ModUtil.nonNull(inv.getItem(i));
            if (itemstack.getItem().hasCraftingRemainingItem()) {
                aitemstack.set(i, new ItemStack(itemstack.getItem().getCraftingRemainingItem()));
            }
        }

        return aitemstack;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.STACKABLE_CRAFTING.get();
    }
}
