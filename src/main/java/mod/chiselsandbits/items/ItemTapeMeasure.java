package mod.chiselsandbits.items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mod.chiselsandbits.chiseledblock.data.BitLocation;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.core.ReflectionWrapper;
import mod.chiselsandbits.helpers.BitOperation;
import mod.chiselsandbits.helpers.ChiselToolType;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.interfaces.IChiselModeItem;
import mod.chiselsandbits.interfaces.IItemScrollWheel;
import mod.chiselsandbits.network.packets.PacketSetColor;
import mod.chiselsandbits.registry.ModDataComponents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

public class ItemTapeMeasure extends Item implements IChiselModeItem, IItemScrollWheel {
    public ItemTapeMeasure(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    @Environment(EnvType.CLIENT)
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
                        LocalStrings.HelpTapeMeasure,
                        helpText,
                        ClientSide.instance.getKeyName(Minecraft.getInstance().options.keyUse),
                        ClientSide.instance.getKeyName(Minecraft.getInstance().options.keyUse),
                        ClientSide.instance.getKeyName(Minecraft.getInstance().options.keyShift),
                        ClientSide.instance.getModeKey());
        helpText.forEach(tooltip);
    }

    @Override
    public InteractionResult use(final Level worldIn, final Player playerIn, final InteractionHand hand) {
        if (playerIn.isShiftKeyDown() && playerIn.level().isClientSide()) {
            ClientSide.instance.tapeMeasures.clear();
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            if (context.getPlayer().isShiftKeyDown()) {
                ClientSide.instance.tapeMeasures.clear();
                return InteractionResult.SUCCESS;
            }

            final Pair<Vec3, Vec3> PlayerRay = ModUtil.getPlayerRay(context.getPlayer());
            final Vec3 ray_from = PlayerRay.getLeft();
            final Vec3 ray_to = PlayerRay.getRight();

            final ClipContext rayTraceContext = new ClipContext(
                    ray_from, ray_to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context.getPlayer());

            final BlockHitResult mop = context.getPlayer().level().clip(rayTraceContext);
            if (mop.getType() == HitResult.Type.BLOCK) {
                final BitLocation loc = new BitLocation(mop, BitOperation.CHISEL);
                ClientSide.instance.pointAt(ChiselToolType.TAPEMEASURE, loc, context.getHand());
            } else {
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.SUCCESS;
    }

    //    @Override
    //    public Component getHighlightTip(final ItemStack item, final Component displayName)
    //    {
    //        if (EffectiveSide.get().isClient() && displayName instanceof MutableComponent &&
    // ChiselsAndBits.getConfig().getClient().itemNameModeDisplay.get() )
    //        {
    //            final MutableComponent formattableTextComponent = (MutableComponent) displayName;
    //            return formattableTextComponent.append(" - ").append(TapeMeasureModes.getMode( item
    // ).string.getLocal()).append(" - ").append(DeprecationHelper.translateToLocal( "chiselsandbits.color." +
    // getTapeColor( item ).getName()) );
    //        }
    //
    //        return displayName;
    //    }

    public DyeColor getTapeColor(final ItemStack item) {
        final DyeColor component = item.get(ModDataComponents.COLOR);
        if (component != null) {
            return component;
        }
        final CompoundTag compound = ModUtil.getTagCompound(item);
        if (compound.contains("color")) {
            try {
                final DyeColor legacy = DyeColor.valueOf(compound.getStringOr("color", DyeColor.WHITE.name()));
                item.set(ModDataComponents.COLOR, legacy);
                return legacy;
            } catch (final IllegalArgumentException iae) {
                // nope!
            }
        }

        return DyeColor.WHITE;
    }

    @Override
    public void scroll(final Player player, final ItemStack stack, final int dwheel) {
        final DyeColor color = getTapeColor(stack);
        int next = color.ordinal() + (dwheel < 0 ? -1 : 1);

        if (next < 0) {
            next = DyeColor.values().length - 1;
        }

        if (next >= DyeColor.values().length) {
            next = 0;
        }

        final DyeColor col = DyeColor.values()[next];
        setTapeColor(stack, col);

        final PacketSetColor setColor = new PacketSetColor(
                col,
                ChiselToolType.TAPEMEASURE,
                ChiselsAndBits.getConfig().getClient().chatModeNotification.get());

        ChiselsAndBits.getNetworkChannel().sendToServer(setColor);
        ReflectionWrapper.instance.clearHighlightedStack();
    }

    public void setTapeColor(final ItemStack stack, final DyeColor color) {
        stack.set(ModDataComponents.COLOR, color);
    }
}
