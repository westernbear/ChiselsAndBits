package mod.chiselsandbits.bitbag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.items.ItemBitBag;
import mod.chiselsandbits.items.ItemChiseledBit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class BagInventory implements Container {

    // internal storage, the capability.
    BagStorage inv;

    // tmp storage, the IInventory
    ItemStack[] stackSlots;

    public BagInventory(final ItemStack is) {
        inv = new BagStorage(is);
        stackSlots = new ItemStack[BagStorage.BAG_STORAGE_SLOTS];

        for (int x = 0; x < stackSlots.length; ++x) {
            stackSlots[x] = ModUtil.getEmptyStack();
        }
    }

    public ItemStack getItemStack() {
        return inv.stack;
    }

    @Override
    public int getContainerSize() {
        return stackSlots.length;
    }

    private int getStateInSlot(int index) {
        final int qty = inv.contents[ItemBitBag.INTS_PER_BIT_TYPE * index + ItemBitBag.OFFSET_QUANTITY];
        final int id = inv.contents[ItemBitBag.INTS_PER_BIT_TYPE * index + ItemBitBag.OFFSET_STATE_ID];

        if (qty > 0) {
            return id;
        }

        return 0;
    }

    @Override
    public @NotNull ItemStack getItem(final int index) {
        final int qty = inv.contents[ItemBitBag.INTS_PER_BIT_TYPE * index + ItemBitBag.OFFSET_QUANTITY];
        final int id = inv.contents[ItemBitBag.INTS_PER_BIT_TYPE * index + ItemBitBag.OFFSET_STATE_ID];

        if (ModUtil.notEmpty(stackSlots[index])) {
            final ItemStack which = ModUtil.nonNull(stackSlots[index]);
            ModUtil.setStackSize(which, qty);
            return which;
        }

        if (qty == 0 || id == 0) {
            return ModUtil.getEmptyStack();
        }

        return stackSlots[index] = ItemChiseledBit.createStack(id, qty, false);
    }

    @Override
    public ItemStack removeItem(final int index, int count) {
        final int qty = inv.contents[ItemBitBag.INTS_PER_BIT_TYPE * index + ItemBitBag.OFFSET_QUANTITY];
        final int id = inv.contents[ItemBitBag.INTS_PER_BIT_TYPE * index + ItemBitBag.OFFSET_STATE_ID];

        if (qty == 0 || id == 0) {
            return ModUtil.getEmptyStack();
        }

        if (count > qty) {
            count = qty;
        }

        inv.contents[ItemBitBag.INTS_PER_BIT_TYPE * index + ItemBitBag.OFFSET_QUANTITY] -= count;
        inv.onChange();

        if (ModUtil.notEmpty(stackSlots[index])) {
            ModUtil.adjustStackSize(ModUtil.nonNull(stackSlots[index]), -count);
        }

        return ItemChiseledBit.createStack(id, count, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(final int index) {
        return ModUtil.getEmptyStack();
    }

    @Override
    public void setItem(final int index, final ItemStack stack) {
        stackSlots[index] = ModUtil.getEmptyStack();

        if (stack != null && stack.getItem() instanceof ItemChiseledBit) {
            inv.contents[ItemBitBag.INTS_PER_BIT_TYPE * index + ItemBitBag.OFFSET_QUANTITY] =
                    ModUtil.getStackSize(stack);
            inv.contents[ItemBitBag.INTS_PER_BIT_TYPE * index + ItemBitBag.OFFSET_STATE_ID] =
                    ItemChiseledBit.getStackState(stack);
        } else {
            inv.contents[ItemBitBag.INTS_PER_BIT_TYPE * index + ItemBitBag.OFFSET_QUANTITY] = 0;
            inv.contents[ItemBitBag.INTS_PER_BIT_TYPE * index + ItemBitBag.OFFSET_STATE_ID] = 0;
        }

        inv.onChange();
    }

    @Override
    public int getMaxStackSize() {
        return ChiselsAndBits.getConfig().getServer().bagStackSize.get();
    }

    @Override
    public void setChanged() {
        for (int x = 0; x < getContainerSize(); x++) {
            if (ModUtil.notEmpty(stackSlots[x])) {
                inv.contents[ItemBitBag.INTS_PER_BIT_TYPE * x + ItemBitBag.OFFSET_QUANTITY] =
                        ModUtil.getStackSize(stackSlots[x]);
                stackSlots[x] = ModUtil.getEmptyStack();
                inv.onChange();
            }
        }
    }

    @Override
    public boolean stillValid(final Player player) {
        return true;
    }

    @Override
    public void startOpen(final Player player) {}

    @Override
    public void stopOpen(final Player player) {}

    @Override
    public boolean canPlaceItem(final int index, final ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemChiseledBit;
    }

    public void sort() {
        List<StateQtyPair> stacks = new ArrayList<StateQtyPair>();

        for (int x = 0; x < stackSlots.length; ++x) {
            int state = inv.contents[x * ItemBitBag.INTS_PER_BIT_TYPE + ItemBitBag.OFFSET_STATE_ID];
            int qty = inv.contents[x * ItemBitBag.INTS_PER_BIT_TYPE + ItemBitBag.OFFSET_QUANTITY];

            if (state > 0 && qty > 0) {
                stacks.add(new StateQtyPair(state, qty));
            }
        }

        stacks.sort(new Comparator<StateQtyPair>() {

            @Override
            public int compare(StateQtyPair o1, StateQtyPair o2) {
                if (o1.state < o2.state) {
                    return 1;
                }

                if (o1.state > o2.state) {
                    return -1;
                }

                if (o1.qty < o2.qty) {
                    return 1;
                }

                if (o1.qty > o2.qty) {
                    return -1;
                }

                return 0;
            }
        });

        for (int x = 0; x < stacks.size() - 1; x++) {
            StateQtyPair a = stacks.get(x);
            StateQtyPair b = stacks.get(x + 1);

            if (a.state == b.state) {
                if (a.qty < getMaxStackSize()) {
                    int shiftSize = getMaxStackSize() - a.qty;
                    shiftSize = Math.min(shiftSize, b.qty);

                    a.qty += shiftSize;
                    b.qty -= shiftSize;

                    if (b.qty <= 0) {
                        stacks.remove(x + 1);
                    }

                    --x;
                }
            }
        }

        for (int x = 0; x < stackSlots.length; ++x) {
            int state = 0;
            int qty = 0;

            if (stacks.size() > x) {
                state = stacks.get(x).state;
                qty = stacks.get(x).qty;
            }

            inv.contents[x * ItemBitBag.INTS_PER_BIT_TYPE + ItemBitBag.OFFSET_STATE_ID] = state;
            inv.contents[x * ItemBitBag.INTS_PER_BIT_TYPE + ItemBitBag.OFFSET_QUANTITY] = qty;

            stackSlots[x] = ModUtil.getEmptyStack();
        }

        inv.onChange();
    }

    public void clear(final ItemStack stack) {
        for (int x = 0; x < stackSlots.length; ++x) {
            if (matches(stack, stackSlots[x])) {
                stackSlots[x] = ModUtil.getEmptyStack();
                inv.contents[x * ItemBitBag.INTS_PER_BIT_TYPE + ItemBitBag.OFFSET_STATE_ID] = 0;
                inv.contents[x * ItemBitBag.INTS_PER_BIT_TYPE + ItemBitBag.OFFSET_QUANTITY] = 0;
            }
        }

        inv.onChange();
    }

    public boolean matches(final ItemStack cmpStack, final ItemStack invStack) {
        if (ModUtil.isEmpty(cmpStack) || invStack == null) {
            return true;
        }

        return cmpStack.getItem() == invStack.getItem() && ItemStack.isSameItemSameTags(cmpStack, invStack);
    }

    public ItemStack restockItem(final ItemStack target, final ItemStack targetType) {
        int outSize = ModUtil.getStackSize(target);

        for (int x = getContainerSize() - 1; x >= 0; x--) {
            if (ItemChiseledBit.sameBit(targetType, getStateInSlot(x))) {
                final ItemStack is = getItem(x);

                outSize += ModUtil.getStackSize(is);
                final int total = outSize;
                outSize = Math.min(is.getMaxStackSize(), outSize);
                final int overage = total - outSize;

                if (overage > 0) {
                    ModUtil.setStackSize(is, overage);
                } else {
                    setItem(x, ModUtil.getEmptyStack());
                }

                setChanged();

                if (outSize == is.getMaxStackSize()) {
                    // done!
                    break;
                }
            }
        }

        final ItemStack out = ModUtil.copy(targetType);
        ModUtil.setStackSize(out, outSize);
        return out;
    }

    public @NotNull ItemStack insertItem(final @NotNull ItemStack which) {
        for (int x = 0; x < getContainerSize(); x++) {
            final ItemStack is = getItem(x);
            if (!ModUtil.isEmpty(is) && ItemChiseledBit.getStackState(which) == ItemChiseledBit.getStackState(is)) {
                ModUtil.adjustStackSize(is, ModUtil.getStackSize(which));
                final int total = ModUtil.getStackSize(is);
                ModUtil.setStackSize(is, Math.min(getMaxStackSize(), ModUtil.getStackSize(is)));
                final int overage = total - ModUtil.getStackSize(is);
                if (overage > 0) {
                    ModUtil.setStackSize(which, overage);
                    setChanged();
                } else {
                    setChanged();
                    return ModUtil.getEmptyStack();
                }
            } else if (ModUtil.isEmpty(is)) {
                setItem(x, which);
                setChanged();
                return ModUtil.getEmptyStack();
            }
        }

        return which;
    }

    public int extractBit(final int bitMeta, int total) {
        int used = 0;

        for (int index = stackSlots.length - 1; index >= 0; index--) {
            final int qty_idx = ItemBitBag.INTS_PER_BIT_TYPE * index + ItemBitBag.OFFSET_QUANTITY;

            final int qty = inv.contents[qty_idx];
            final int id = inv.contents[ItemBitBag.INTS_PER_BIT_TYPE * index + ItemBitBag.OFFSET_STATE_ID];

            if (id == bitMeta && qty > 0) {
                inv.contents[qty_idx] -= total;

                if (inv.contents[qty_idx] < 0) {
                    inv.contents[qty_idx] = 0;
                }

                inv.onChange();

                final int diff = qty - inv.contents[qty_idx];
                used += diff;
                total -= diff;

                if (0 == total) {
                    return used;
                }
            }
        }

        return used;
    }

    @Environment(EnvType.CLIENT)
    public List<Component> listContents(final List<Component> details) {
        final TreeMap<String, Integer> contents = new TreeMap<>();

        for (int x = 0; x < getContainerSize(); x++) {
            final ItemStack is = getItem(x);
            if (!ModUtil.isEmpty(is)) {
                final BlockState state = ModUtil.getStateById(ItemChiseledBit.getStackState(is));
                if (state == null) {
                    continue;
                }

                final Component name = ItemChiseledBit.getBitStateName(state);

                if (name != null) {
                    Integer count = contents.get(name.getString());
                    if (count == null) {
                        count = ModUtil.getStackSize(is);
                    } else {
                        count += ModUtil.getStackSize(is);
                    }

                    contents.put(name.getString(), count);
                }
            }
        }

        if (contents.isEmpty()) {
            details.add(Component.literal(LocalStrings.Empty.getLocal()));
        }

        final List<Entry<String, Integer>> list = new ArrayList<>();
        list.addAll(contents.entrySet());

        Collections.sort(list, (o1, o2) -> {
            final int y = o1.getValue();
            final int x = o2.getValue();

            return Integer.compare(x, y);
        });

        for (final Entry<String, Integer> e : list) {
            details.add(Component.literal(e.getValue().toString()).append(" ").append(e.getKey()));
        }

        return details;
    }

    @Override
    public void clearContent() {
        clear(null);
    }

    @Override
    public boolean isEmpty() {
        for (final ItemStack itemstack : stackSlots) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static class StateQtyPair {
        int qty;
        int state;

        public StateQtyPair(int state, int qty) {
            this.qty = qty;
            this.state = state;
        }
    }
}
