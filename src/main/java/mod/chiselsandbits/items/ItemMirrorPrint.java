package mod.chiselsandbits.items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.components.ChiseledData;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.interfaces.IPatternItem;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModDataComponents;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.render.helpers.SimpleInstanceCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemMirrorPrint extends Item implements IPatternItem {

    SimpleInstanceCache<ItemStack, List<Component>> toolTipCache = new SimpleInstanceCache<>(null, new ArrayList<>());

    public ItemMirrorPrint(Item.Properties properties) {
        super(properties);
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
        final List<Component> tooltipLines = new ArrayList<>();
        ChiselsAndBits.getConfig()
                .getCommon()
                .helpText(
                        LocalStrings.HelpMirrorPrint,
                        tooltipLines,
                        ClientSide.instance.getKeyName(Minecraft.getInstance().options.keyUse));

        if (isWritten(stack)) {
            if (ClientSide.instance.holdingShift()) {
                if (toolTipCache.needsUpdate(stack)) {
                    final VoxelBlob blob = ModUtil.getBlobFromStack(stack, null);
                    toolTipCache.updateCachedValue(blob.listContents(new ArrayList<>()));
                }

                tooltipLines.addAll(toolTipCache.getCached());
            } else {
                tooltipLines.add(Component.literal(LocalStrings.ShiftDetails.getLocal()));
            }
        }
        tooltipLines.forEach(tooltip);
    }

    /** Compatibility overload for callers that previously supplied a stack. */
    @Deprecated
    public String getDescriptionId(final ItemStack stack) {
        return super.getDescriptionId();
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final ItemStack stack = context.getPlayer().getItemInHand(context.getHand());

        if (!context.getPlayer().mayUseItemAt(context.getClickedPos(), context.getClickedFace(), stack)) {
            return InteractionResult.SUCCESS;
        }

        if (!isWritten(stack)) {
            final ChiseledData data = getChiseledDataFromBlock(
                    context.getLevel(), context.getClickedPos(), context.getPlayer(), context.getClickedFace());
            if (data != null) {
                stack.shrink(1);

                final ItemStack newStack = new ItemStack(ModItems.ITEM_MIRROR_PRINT_WRITTEN.get(), 1);
                newStack.set(ModDataComponents.CHISELED_DATA, data);
                ModUtil.setSide(newStack, ModUtil.getPlaceFace(context.getPlayer()));

                final ItemEntity entity = context.getPlayer().drop(newStack, true);
                entity.setPickUpDelay(0);
                entity.setThrower(context.getPlayer());

                return InteractionResult.SUCCESS;
            }

            return InteractionResult.FAIL;
        }

        return InteractionResult.FAIL;
    }

    protected ChiseledData getChiseledDataFromBlock(
            final Level world, final BlockPos pos, final Player player, final Direction face) {
        final TileEntityBlockChiseled te = ModUtil.getChiseledTileEntity(world, pos, false);

        if (te != null) {
            final NBTBlobConverter converter = new NBTBlobConverter();
            converter.setBlob(te.getBlob().mirror(face.getAxis()));
            return converter.toComponent(false);
        }

        return null;
    }

    @Override
    public ItemStack getPatternedItem(final ItemStack stack, final boolean wantRealItems) {
        if (!isWritten(stack)) {
            return null;
        }

        final ChiseledData data = NBTBlobConverter.getComponent(stack);
        if (data == null) {
            return null;
        }

        // Detect and provide full blocks if pattern solid full and solid.
        final NBTBlobConverter conv = new NBTBlobConverter();
        conv.readChisleData(data, VoxelBlob.VERSION_ANY);

        final ItemStack itemstack = new ItemStack(ModBlocks.getChiseledBlock(), 1);
        itemstack.set(ModDataComponents.CHISELED_DATA, data);
        ModUtil.setSide(itemstack, ModUtil.getSide(stack));
        return itemstack;
    }

    @Override
    public boolean isWritten(final ItemStack stack) {
        return stack.getItem() == ModItems.ITEM_MIRROR_PRINT_WRITTEN.get() && ModUtil.hasChiseledData(stack);
    }
}
