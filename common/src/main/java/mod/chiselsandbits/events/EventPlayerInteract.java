package mod.chiselsandbits.events;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import java.util.WeakHashMap;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.items.ItemChisel;
import mod.chiselsandbits.items.ItemChiseledBit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class EventPlayerInteract {

    private static final WeakHashMap<Player, Boolean> serverSuppressEvent = new WeakHashMap<>();

    private EventPlayerInteract() {}

    public static void register() {
        InteractionEvent.LEFT_CLICK_BLOCK.register(EventPlayerInteract::leftClickBlock);
        InteractionEvent.RIGHT_CLICK_BLOCK.register(EventPlayerInteract::rightClickBlock);
    }

    public static void setPlayerSuppressionState(final Player player, final boolean state) {
        if (state) {
            serverSuppressEvent.put(player, state);
        } else {
            serverSuppressEvent.remove(player);
        }
    }

    private static EventResult leftClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction direction) {
        final InteractionResult result = interaction(player, player.level(), hand, pos, direction);
        return toEventResult(result);
    }

    private static EventResult rightClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction direction) {
        final BlockHitResult hitResult = new BlockHitResult(player.getEyePosition(1.0F), direction, pos, false);
        final InteractionResult result = testInteractionSupression(player, player.level(), hand, hitResult);
        return toEventResult(result);
    }

    private static EventResult toEventResult(InteractionResult result) {
        if (result.consumesAction()) {
            return EventResult.interruptTrue();
        }
        if (result == InteractionResult.FAIL) {
            return EventResult.interruptFalse();
        }
        return EventResult.pass();
    }

    private static InteractionResult interaction(
            Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        final ItemStack stack = player.getItemInHand(hand);
        final boolean validEvent = pos != null && world != null;
        if ((stack.getItem() instanceof ItemChisel || stack.getItem() instanceof ItemChiseledBit) && validEvent) {
            final BlockState state = world.getBlockState(pos);
            if (BlockBitInfo.canChisel(state)) {
                if (world.isClientSide()) {
                    stack.canDestroyBlock(state, world, pos, player);
                }
                return InteractionResult.FAIL;
            }
        }

        return testInteractionSupression(world, player);
    }

    private static InteractionResult testInteractionSupression(
            Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (world.isClientSide() && ClientSide.instance != null) {
            if (ClientSide.instance.getStartPos() != null) {
                return InteractionResult.FAIL;
            }
        }
        final ItemStack itemEntity = player.getItemInHand(hand);
        if (!world.isClientSide() && itemEntity != null) {
            if (serverSuppressEvent.containsKey(player)) {
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult testInteractionSupression(Level level, Player player) {
        if (level.isClientSide() && ClientSide.instance != null) {
            if (ClientSide.instance.getStartPos() != null) {
                return InteractionResult.FAIL;
            }
        }

        if (!level.isClientSide()) {
            if (serverSuppressEvent.containsKey(player)) {
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }
}
