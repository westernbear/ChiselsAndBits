package mod.chiselsandbits.events;

import java.util.WeakHashMap;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.items.ItemChisel;
import mod.chiselsandbits.items.ItemChiseledBit;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Disable breaking blocks when using a chisel / bit, some items break too fast
 * for the other code to prevent which is where this comes in.
 * <p>
 * This manages survival chisel actions, creative some how skips this and calls
 * onBlockStartBreak on its own, but when in creative this is called on the
 * server... which still needs to be canceled or it will break the block.
 * <p>
 * The whole things, is very strange.
 */
public class EventPlayerInteract {

    private static final WeakHashMap<Player, Boolean> serverSuppressEvent = new WeakHashMap<Player, Boolean>();

    public static void register() {
        AttackBlockCallback.EVENT.register(EventPlayerInteract::interaction);
        UseBlockCallback.EVENT.register(EventPlayerInteract::interaction);
    }

    public static void setPlayerSuppressionState(final Player player, final boolean state) {
        if (state) {
            serverSuppressEvent.put(player, state);
        } else {
            serverSuppressEvent.remove(player);
        }
    }

    private static InteractionResult interaction(
            Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        final ItemStack is = player.getItemInHand(hand);
        final boolean validEvent = pos != null && world != null;
        if ((is.getItem() instanceof ItemChisel || is.getItem() instanceof ItemChiseledBit) && validEvent) {
            final BlockState state = world.getBlockState(pos);
            if (BlockBitInfo.canChisel(state)) {
                // cancel interactions vs chiseable blocks, creative is
                // magic.
                return InteractionResult.FAIL;
            }
        }

        return testInteractionSupression(world, player);
    }

    private static InteractionResult interaction(
            Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {

        return testInteractionSupression(player, world, hand, hitResult);
    }

    private static InteractionResult testInteractionSupression(
            Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        // client is dragging...
        if (world.isClientSide()) {
            if (ClientSide.instance.getStartPos() != null) {
                return InteractionResult.FAIL;
            }
        }
        ItemStack itemEntity = player.getItemInHand(hand);
        // server is supressed.
        if (!world.isClientSide() && itemEntity != null) {
            if (serverSuppressEvent.containsKey(player)) {
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult testInteractionSupression(Level level, Player player) {
        // client is dragging...
        if (level.isClientSide()) {
            if (ClientSide.instance.getStartPos() != null) {
                return InteractionResult.FAIL;
            }
        }

        // server is supressed.
        if (!level.isClientSide()) {
            if (serverSuppressEvent.containsKey(player)) {
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }
}
