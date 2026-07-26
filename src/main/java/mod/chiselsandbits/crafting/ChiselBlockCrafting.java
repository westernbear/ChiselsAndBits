package mod.chiselsandbits.crafting;

import mod.chiselsandbits.api.APIExceptions.InvalidBitItem;
import mod.chiselsandbits.api.IBitAccess;
import mod.chiselsandbits.api.IBitBag;
import mod.chiselsandbits.api.IBitBrush;
import mod.chiselsandbits.api.IBitVisitor;
import mod.chiselsandbits.api.ItemType;
import mod.chiselsandbits.chiseledblock.ItemBlockChiseled;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.items.ItemBitBag;
import mod.chiselsandbits.items.ItemChisel;
import mod.chiselsandbits.registry.ModRecipeSerializers;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ChiselBlockCrafting extends CustomRecipe {

    public ChiselBlockCrafting() {
        super();
    }

    private ChiselBlockInfo getInfo(final CraftingInput inv) {
        final ChiselBlockInfo i = new ChiselBlockInfo();
        boolean noDuplicates = true;
        boolean noStrangeitems = true;

        for (int x = 0; x < inv.size(); ++x) {
            final ItemStack is = inv.getItem(x);

            if (ModUtil.isEmpty(is)) {
                continue;
            }

            if (is.getItem() instanceof ItemBitBag) {
                if (i.bag_slot != -1) {
                    noDuplicates = false;
                }

                i.bag = is;
                i.bag_slot = x;
                continue;
            }

            if (is.getItem() instanceof ItemChisel) {
                if (i.chisel_slot != -1) {
                    noDuplicates = false;
                }

                i.chisel = is;
                i.chisel_slot = x;
                continue;
            }

            if (is.getItem() instanceof BlockItem) {
                if (i.block_slot != -1) {
                    noDuplicates = false;
                }

                final BlockState actingState = ModUtil.getStateFromItem(is);
                if (actingState.getBlock() != Blocks.AIR) {
                    try {
                        final IBitBrush state = ChiselsAndBits.getApi().createBrushFromState(actingState);
                        final IBitAccess item = ChiselsAndBits.getApi().createBitItem(ModUtil.getEmptyStack());
                        assert item != null;

                        item.visitBits(new IBitVisitor() {

                            @Override
                            public IBitBrush visitBit(
                                    final int x, final int y, final int z, final IBitBrush currentValue) {
                                return state;
                            }
                        });

                        i.block = item.getBitsAsItem(Direction.EAST, ItemType.CHISELED_BLOCK, false);
                        if (i.block != null) {
                            ModUtil.setStackSize(i.block, ModUtil.getStackSize(is));
                            i.block_slot = x;
                            continue;
                        }
                    } catch (final InvalidBitItem err) {
                        // not supported.
                    }
                }
            }

            if (is.getItem() instanceof ItemBlockChiseled) {
                if (i.block_slot != -1) {
                    noDuplicates = false;
                }

                i.block = is;
                i.block_slot = x;
                continue;
            }

            noStrangeitems = false;
        }

        i.isValid = i.chisel_slot != -1 && i.bag_slot != -1 && i.block_slot != -1 && noDuplicates && noStrangeitems;

        return i;
    }

    @Override
    public boolean matches(final CraftingInput inv, final Level worldIn) {
        return inv.width() * inv.height() > 3 && getInfo(inv).isValid;
    }

    @Override
    public ItemStack assemble(final CraftingInput inv) {
        final ChiselBlockInfo cbc = getInfo(inv);
        cbc.doLogic();

        if (cbc.isValid && cbc.modified) {
            return cbc.bag;
        }

        return ModUtil.getEmptyStack();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingInput inv) {
        final NonNullList<ItemStack> list = NonNullList.create();

        final ChiselBlockInfo cbc = getInfo(inv);
        cbc.doLogic();

        boolean damageTools = ChiselsAndBits.getConfig().getServer().damageTools.get();
        for (int x = 0; x < inv.size(); ++x) {
            if (cbc.isValid
                    && x == cbc.chisel_slot
                    && !ModUtil.isEmpty(cbc.chisel)
                    && (!damageTools || cbc.chisel.getDamageValue() < cbc.chisel.getMaxDamage())) {
                list.add(cbc.chisel);
            } else if (cbc.isValid && x == cbc.block_slot && !ModUtil.isEmpty(cbc.block)) {
                ItemStack block = ModUtil.copy(cbc.block);
                ModUtil.setStackSize(block, 1);
                list.add(block);
            } else {
                list.add(ModUtil.getEmptyStack());
            }
        }

        return list;
    }

    @Override
    public RecipeSerializer<ChiselBlockCrafting> getSerializer() {
        return ModRecipeSerializers.CHISEL_BLOCK_CRAFTING.get();
    }

    private static class ChiselBlockInfo {
        public ItemStack chisel = ModUtil.getEmptyStack();
        public int chisel_slot = -1;

        public ItemStack bag = ModUtil.getEmptyStack();
        public int bag_slot = -1;

        public ItemStack block = ModUtil.getEmptyStack();
        public int block_slot = -1;

        public boolean isValid;
        public boolean modified = false;

        public void doLogic() {
            bag = ModUtil.copy(bag);
            block = ModUtil.copy(block);
            chisel = ModUtil.copy(chisel);

            try {
                final IBitAccess ba = ChiselsAndBits.getApi().createBitItem(block);
                final Chiseler c = new Chiseler(chisel, ChiselsAndBits.getApi().getBitbag(bag));

                if (ba == null) {
                    return;
                }

                ba.visitBits(c);

                modified = c.modified;

                if (!c.isAir) {
                    block = ba.getBitsAsItem(Direction.NORTH, ItemType.CHISELED_BLOCK, false);
                } else {
                    block = ModUtil.getEmptyStack();
                }
            } catch (final InvalidBitItem e) {

            }
        }

        private static class Chiseler implements IBitVisitor {
            final IBitBrush airBrush;
            private final ItemStack chisel;
            private final IBitBag bbag;
            private final RandomSource r = RandomSource.create();
            public boolean isAir = true;
            public boolean modified = false;

            public Chiseler(final ItemStack chisel, final IBitBag bag) throws InvalidBitItem {
                airBrush = ChiselsAndBits.getApi().createBrushFromState(null);
                this.chisel = chisel;
                bbag = bag;
                r.setSeed(0); // ensure that the results are always the same,
                // crafting needs to be 'regular'
            }

            @Override
            public IBitBrush visitBit(final int x, final int y, final int z, final IBitBrush currentValue) {
                if (currentValue.isAir()) {
                    return currentValue;
                }

                boolean damageTools =
                        ChiselsAndBits.getConfig().getServer().damageTools.get();

                if (chisel.getDamageValue() < chisel.getMaxDamage() || !damageTools) {
                    if (damageTools) {
                        ModUtil.damageItem(chisel, r);
                    }

                    final ItemStack is = currentValue.getItemStack(1);
                    if (is != null) {
                        for (int idx = 0; idx < bbag.getSlots(); ++idx) {
                            if (ModUtil.isEmpty(bbag.insertItem(idx, is, false))) {
                                modified = true;
                                return airBrush;
                            }
                        }
                    }
                }

                isAir = false;
                return currentValue;
            }
        }
    }
}
