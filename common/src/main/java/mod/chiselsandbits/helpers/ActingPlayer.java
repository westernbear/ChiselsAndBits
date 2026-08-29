package mod.chiselsandbits.helpers;

import mod.chiselsandbits.api.EventBlockBitModification;
import mod.chiselsandbits.api.events.BitModificationEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ActingPlayer {
    private final Container storage;

    // used to test permission and stuff...
    private final Player innerPlayer;
    private final boolean realPlayer; // are we a real player?
    private final InteractionHand hand;
    // permission check cache.
    BlockPos lastPos = null;
    Boolean lastPlacement = null;
    ItemStack lastPermissionBit = null;
    Boolean permissionResult = null;

    private ActingPlayer(final Player player, final boolean realPlayer, final InteractionHand hand) {
        innerPlayer = player;
        this.hand = hand;
        this.realPlayer = realPlayer;
        storage = realPlayer ? player.getInventory() : new PlayerCopiedInventory(player.getInventory());
    }

    @NotNull
    public static ActingPlayer actingAs(final Player player, final InteractionHand hand) {
        return new ActingPlayer(player, true, hand);
    }

    @NotNull
    public static ActingPlayer testingAs(final Player player, final InteractionHand hand) {
        return new ActingPlayer(player, false, hand);
    }

    public Container getInventory() {
        return storage;
    }

    public int getCurrentItem() {
        return innerPlayer.getInventory().getSelectedSlot();
    }

    public boolean isCreative() {
        return innerPlayer.isCreative();
    }

    public ItemStack getCurrentEquippedItem() {
        return storage.getItem(getCurrentItem());
    }

    public boolean canPlayerManipulate(
            final @NotNull BlockPos pos,
            final @NotNull Direction side,
            final @NotNull ItemStack is,
            final boolean placement) {
        // only re-test if something changes.
        if (permissionResult == null || lastPermissionBit != is || lastPos != pos || placement != lastPlacement) {
            lastPos = pos;
            lastPlacement = placement;
            lastPermissionBit = is;

            if (innerPlayer.mayUseItemAt(pos, side, is) && innerPlayer.level().mayInteract(innerPlayer, pos)) {
                final EventBlockBitModification event =
                        new EventBlockBitModification(innerPlayer.level(), pos, innerPlayer, hand, is, placement);
                BitModificationEvents.BLOCK_BIT_MODIFICATION.invoker().handle(event);
                permissionResult = !event.isCancelled();
            } else {
                permissionResult = false;
            }
        }

        return permissionResult;
    }

    public void damageItem(final ItemStack stack, final int amount) {
        if (realPlayer) {
            stack.hurtAndBreak(amount, innerPlayer, hand);
        } else {
            stack.setDamageValue(stack.getDamageValue() + amount);
        }
    }

    public void playerDestroyItem(final @NotNull ItemStack stack, final InteractionHand hand) {
        if (realPlayer) {
            //			net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem( innerPlayer, stack, hand );
        }
    }

    public Level getWorld() {
        return innerPlayer.level();
    }

    /**
     * only call this is you require a player, and only as a last resort.
     */
    public Player getPlayer() {
        return innerPlayer;
    }

    public boolean isReal() {
        return realPlayer;
    }

    /**
     * @return the hand
     */
    public InteractionHand getHand() {
        return hand;
    }
}
