package mod.chiselsandbits.items;

import dev.architectury.injectables.annotations.Environment;
import dev.architectury.utils.Env;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import mod.chiselsandbits.bitbag.BagInventory;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.ItemBlockChiseled;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.components.ChiseledData;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.ActingPlayer;
import mod.chiselsandbits.helpers.BitInventoryFeeder;
import mod.chiselsandbits.helpers.ContinousChisels;
import mod.chiselsandbits.helpers.IContinuousInventory;
import mod.chiselsandbits.helpers.IItemInInventory;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.interfaces.IChiselModeItem;
import mod.chiselsandbits.modes.PositivePatternMode;
import mod.chiselsandbits.network.packets.PacketAccurateSneakPlace;
import mod.chiselsandbits.network.packets.PacketAccurateSneakPlace.IItemBlockAccurate;
import mod.chiselsandbits.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ItemPositivePrint extends ItemNegativePrint implements IChiselModeItem, IItemBlockAccurate {

    public ItemPositivePrint(final Properties properties) {
        super(properties);
    }

    @Override
    protected Item getWrittenItem() {
        return ModItems.ITEM_POSITIVE_PRINT_WRITTEN.get();
    }

    @Override
    @Environment(Env.CLIENT)
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final TooltipDisplay display,
            final Consumer<Component> tooltip,
            final TooltipFlag advanced) {
        defaultAddInfo(stack, context, display, tooltip, advanced);
        final List<Component> tooltipLines = new ArrayList<>();
        ChiselsAndBits.getConfig()
                .getCommon()
                .helpText(
                        LocalStrings.HelpPositivePrint,
                        tooltipLines,
                        ClientSide.instance.getKeyName(Minecraft.getInstance().options.keyUse),
                        ClientSide.instance.getKeyName(Minecraft.getInstance().options.keyUse),
                        ClientSide.instance.getModeKey());

        if (ModUtil.hasChiseledData(stack)) {
            if (ClientSide.instance.holdingShift()) {
                if (toolTipCache.needsUpdate(stack)) {
                    final VoxelBlob blob = ModUtil.getBlobFromStack(stack, null);
                    toolTipCache.updateCachedValue(blob.listContents(new ArrayList<Component>()));
                }

                tooltipLines.addAll(toolTipCache.getCached());
            } else {
                tooltipLines.add(Component.literal(LocalStrings.ShiftDetails.getLocal()));
            }
        }
        tooltipLines.forEach(tooltip);
    }

    @Override
    protected ChiseledData getChiseledDataFromBlock(final Level world, final BlockPos pos, final Player player) {
        final BlockState state = world.getBlockState(pos);
        final Block blkObj = state.getBlock();

        if (!(blkObj instanceof BlockChiseled) && BlockBitInfo.canChisel(state)) {
            final NBTBlobConverter tmp = new NBTBlobConverter();

            tmp.fillWith(state);
            return tmp.toComponent(false);
        }

        return super.getChiseledDataFromBlock(world, pos, player);
    }

    @Override
    protected boolean convertToStone() {
        return false;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        InteractionHand hand = context.getHand();
        BlockPos pos = context.getClickedPos();

        final ItemStack stack = player.getItemInHand(hand);
        final BlockState blkstate = world.getBlockState(pos);

        if (ItemChiseledBit.checkRequiredSpace(player, blkstate)) {
            return InteractionResult.FAIL;
        }

        boolean offgrid = false;

        if (PositivePatternMode.getMode(stack) == PositivePatternMode.PLACEMENT) {
            if (!world.isClientSide()) {
                // Say it "worked", Don't do anything we'll get a better
                // packet.
                return InteractionResult.SUCCESS;
            }

            // send accurate packet.
            final PacketAccurateSneakPlace pasp = new PacketAccurateSneakPlace(
                    context.getItemInHand(),
                    pos,
                    hand,
                    context.getClickedFace(),
                    context.getClickLocation().x,
                    context.getClickLocation().y,
                    context.getClickLocation().z,
                    false);

            ChiselsAndBits.getNetworkChannel().sendToServer(pasp);

            return placeItem(new BlockPlaceContext(context), offgrid);
        }

        return placeItem(context, offgrid);
    }

    public final InteractionResult placeItem(final UseOnContext context, boolean offgrid) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();
        BlockPos pos = context.getClickedPos();

        if (PositivePatternMode.getMode(stack) == PositivePatternMode.PLACEMENT) {
            final ItemStack output = getPatternedItem(stack, false);
            if (output != null) {
                final VoxelBlob pattern = ModUtil.getBlobFromStack(stack, player);
                final Map<Integer, Integer> stats = pattern.getBlockSums();

                if (consumeEntirePattern(pattern, stats, pos, ActingPlayer.testingAs(player, hand))
                        && output.getItem() instanceof ItemBlockChiseled ibc) {
                    final InteractionResult res = ibc.tryPlace(context, offgrid);
                    if (res == InteractionResult.SUCCESS) {
                        consumeEntirePattern(pattern, stats, pos, ActingPlayer.actingAs(player, hand));
                    }

                    return res;
                }

                return InteractionResult.FAIL;
            }
        }

        return super.useOn(context);
    }

    private boolean consumeEntirePattern(
            final VoxelBlob pattern, final Map<Integer, Integer> stats, final BlockPos pos, final ActingPlayer player) {
        final List<BagInventory> bags = ModUtil.getBags(player);

        for (final Entry<Integer, Integer> type : stats.entrySet()) {
            final int inPattern = type.getKey();

            if (type.getKey() == 0) {
                continue;
            }

            IItemInInventory bit = ModUtil.findBit(player, pos, inPattern);
            int stillNeeded = type.getValue() - ModUtil.consumeBagBit(bags, inPattern, type.getValue());
            if (stillNeeded != 0) {
                for (int x = stillNeeded; x > 0 && bit.isValid(); --x) {
                    if (bit.consume()) {
                        stillNeeded--;
                        bit = ModUtil.findBit(player, pos, inPattern);
                    }
                }

                if (stillNeeded != 0) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    protected void applyPrint(
            final ItemStack stack,
            final Level world,
            final BlockPos pos,
            final Direction side,
            final VoxelBlob vb,
            final VoxelBlob pattern,
            final Player who,
            final InteractionHand hand) {
        // snag a tool...
        final ActingPlayer player = ActingPlayer.actingAs(who, hand);
        final IContinuousInventory selected = new ContinousChisels(player, pos, side);
        ItemStack spawnedItem = null;

        final VoxelBlob filled = new VoxelBlob();

        final List<BagInventory> bags = ModUtil.getBags(player);
        final List<ItemEntity> spawnlist = new ArrayList<>();

        final PositivePatternMode chiselMode = PositivePatternMode.getMode(stack);
        final boolean chisel_bits =
                chiselMode == PositivePatternMode.IMPOSE || chiselMode == PositivePatternMode.REPLACE;
        final boolean chisel_to_air = chiselMode == PositivePatternMode.REPLACE;

        for (int y = 0; y < vb.detail; y++) {
            for (int z = 0; z < vb.detail; z++) {
                for (int x = 0; x < vb.detail; x++) {
                    int inPlace = vb.get(x, y, z);
                    final int inPattern = pattern.get(x, y, z);
                    if (inPlace != inPattern) {
                        if (inPlace != 0 && chisel_bits && selected.isValid()) {
                            if (chisel_to_air || inPattern != 0) {
                                spawnedItem = ItemChisel.chiselBlock(
                                        selected, player, vb, world, pos, side, x, y, z, spawnedItem, spawnlist);

                                if (spawnedItem != null) {
                                    inPlace = 0;
                                }
                            }
                        }

                        if (inPlace == 0 && inPattern != 0 && filled.get(x, y, z) == 0) {
                            final IItemInInventory bit = ModUtil.findBit(player, pos, inPattern);
                            if (ModUtil.consumeBagBit(bags, inPattern, 1) == 1) {
                                vb.set(x, y, z, inPattern);
                            } else if (bit.isValid()) {
                                if (!player.isCreative()) {
                                    if (bit.consume()) {
                                        vb.set(x, y, z, inPattern);
                                    }
                                } else {
                                    vb.set(x, y, z, inPattern);
                                }
                            }
                        }
                    }
                }
            }
        }

        BitInventoryFeeder feeder = new BitInventoryFeeder(who, world);
        for (final ItemEntity ei : spawnlist) {
            feeder.addItem(ei);
            ItemBitBag.cleanupInventory(who, ei.getItem());
        }
    }

    //    @Override
    //    public Component getHighlightTip(final ItemStack item, final Component displayName)
    //    {
    //        if (EffectiveSide.get().isClient() && displayName instanceof MutableComponent &&
    // ChiselsAndBits.getConfig().getClient().itemNameModeDisplay.get() )
    //        {
    //            MutableComponent formattableTextComponent = (MutableComponent) displayName;
    //            return formattableTextComponent.append(" - ").append(PositivePatternMode.getMode( item
    // ).string.getLocal());
    //        }
    //
    //        return displayName;
    //    }

    @Override
    public InteractionResult tryPlace(final UseOnContext context, final boolean offGrid) {
        if (PositivePatternMode.getMode(context.getItemInHand()) == PositivePatternMode.PLACEMENT) {
            final ItemStack output = getPatternedItem(context.getItemInHand(), false);
            if (output != null) {
                final VoxelBlob pattern = ModUtil.getBlobFromStack(context.getItemInHand(), context.getPlayer());
                final Map<Integer, Integer> stats = pattern.getBlockSums();

                if (consumeEntirePattern(
                                pattern,
                                stats,
                                context.getClickedPos(),
                                ActingPlayer.testingAs(context.getPlayer(), context.getHand()))
                        && output.getItem() instanceof ItemBlockChiseled ibc) {
                    final InteractionResult res = ibc.tryPlace(new BlockPlaceContext(context), offGrid);

                    if (res == InteractionResult.SUCCESS) {
                        consumeEntirePattern(
                                pattern,
                                stats,
                                context.getClickedPos(),
                                ActingPlayer.actingAs(context.getPlayer(), context.getHand()));
                    }

                    return res;
                }

                return InteractionResult.FAIL;
            }
        }

        return super.useOn(context);
    }
}
