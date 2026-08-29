package mod.chiselsandbits.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.events.extra.EntityItemPickupEvent;
import mod.chiselsandbits.items.ItemBitBag;
import mod.chiselsandbits.items.ItemBitBag.BagPos;
import mod.chiselsandbits.items.ItemChiseledBit;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BitInventoryFeeder {
    private static final Random itemRand = new Random();
    final Player player;
    final Level world;
    ArrayList<Integer> seenBits = new ArrayList<>();
    boolean hasSentMessage = false;

    public BitInventoryFeeder(final Player p, final Level w) {
        player = p;
        world = w;
    }

    private static void spawnItem(Level world, ItemEntity ei) {
        if (world.isClientSide()) // no spawning items on the client.
        {
            return;
        }

        world.addFreshEntity(ei);
    }

    public void addItem(final ItemEntity ei) {
        ItemStack is = ModUtil.nonNull(ei.getItem());

        final List<BagPos> bags = ItemBitBag.getBags(player.getInventory());

        if (!ModUtil.containsAtLeastOneOf(player.getInventory(), is)) {
            final ItemStack minSize = is.copy();

            if (ModUtil.getStackSize(minSize) > minSize.getMaxStackSize()) {
                ModUtil.setStackSize(minSize, minSize.getMaxStackSize());
            }

            ModUtil.adjustStackSize(is, -ModUtil.getStackSize(minSize));
            player.getInventory().add(minSize);
            ModUtil.adjustStackSize(is, ModUtil.getStackSize(minSize));
        }

        for (final BagPos bp : bags) {
            is = bp.inv.insertItem(is);
        }

        if (ModUtil.isEmpty(is)) {
            return;
        }

        ei.setItem(is);
        boolean result = EntityItemPickupEvent.EVENT.invoker().handle(ei, player);
        if (result) {
            // cancelled...
            spawnItem(world, ei);
        } else {

            is = ei.getItem();

            if (is != null && !player.getInventory().add(is)) {
                ei.setItem(is);
                // Never spawn the items for dropped excess items if setting is enabled.
                if (!ChiselsAndBits.getConfig().getServer().voidExcessBits.get()) {
                    spawnItem(world, ei);
                }
            } else {
                if (!ei.isSilent()) {
                    ei.level()
                            .playSound(
                                    null,
                                    ei.getX(),
                                    ei.getY(),
                                    ei.getZ(),
                                    SoundEvents.ITEM_PICKUP,
                                    SoundSource.PLAYERS,
                                    0.2F,
                                    ((itemRand.nextFloat() - itemRand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                }
            }

            player.getInventory().setChanged();

            if (player.inventoryMenu != null) {
                player.inventoryMenu.broadcastChanges();
            }
        }

        final int blk = ItemChiseledBit.getStackState(is);
        if (ChiselsAndBits.getConfig().getServer().voidExcessBits.get() && !seenBits.contains(blk) && !hasSentMessage) {
            if (!ItemChiseledBit.hasBitSpace(player, blk)) {
                player.sendSystemMessage(Component.translatable("mod.chiselsandbits.result.void_excess"));
                hasSentMessage = true;
            }
            if (!seenBits.contains(blk)) {
                seenBits.add(blk);
            }
        }
    }
}
