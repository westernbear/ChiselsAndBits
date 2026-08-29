package mod.chiselsandbits.items;

import dev.architectury.injectables.annotations.Environment;
import dev.architectury.utils.Env;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.extensions.BlockExtension;
import mod.chiselsandbits.helpers.LocalStrings;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public class ItemWrench extends Item {

    public ItemWrench(Item.Properties properties) {
        super(properties.stacksTo(1).durability(1));
    }

    @Override
    @Environment(Env.CLIENT)
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final TooltipDisplay display,
            final Consumer<Component> tooltip,
            final TooltipFlag advanced) {
        super.appendHoverText(stack, context, display, tooltip, advanced);
        final List<Component> helpText = new ArrayList<>();
        ChiselsAndBits.getConfig()
                .getCommon()
                .helpText(
                        LocalStrings.HelpWrench,
                        helpText,
                        ClientSide.instance.getKeyName(Minecraft.getInstance().options.keyUse));
        helpText.forEach(tooltip);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        final BlockPos pos = context.getClickedPos();
        final Direction side = context.getClickedFace();
        final Level world = context.getLevel();
        final ItemStack stack = context.getItemInHand();
        final InteractionHand hand = context.getHand();

        if (!player.mayUseItemAt(pos, side, stack) || !world.mayInteract(player, pos)) {
            return InteractionResult.FAIL;
        }

        final BlockState b = world.getBlockState(pos);
        if (b != null && !player.isShiftKeyDown()) {
            BlockState nb;

            if (b.getBlock() instanceof BlockExtension ext) {
                nb = ext.rotate(b, world, pos, side, Rotation.CLOCKWISE_90);
            } else {
                nb = b.rotate(Rotation.CLOCKWISE_90);
            }

            if (nb != b) {
                world.setBlockAndUpdate(pos, nb);
                stack.hurtAndBreak(1, player, hand);
                world.updateNeighborsAt(pos, b.getBlock());
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }
}
