package mod.chiselsandbits.items;

import java.util.ArrayList;
import java.util.List;
import mod.chiselsandbits.api.IBitAccess;
import mod.chiselsandbits.api.VoxelStats;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.ActingPlayer;
import mod.chiselsandbits.helpers.BitInventoryFeeder;
import mod.chiselsandbits.helpers.ContinousChisels;
import mod.chiselsandbits.helpers.IContinuousInventory;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.interfaces.IItemScrollWheel;
import mod.chiselsandbits.interfaces.IPatternItem;
import mod.chiselsandbits.interfaces.IVoxelBlobItem;
import mod.chiselsandbits.network.packets.PacketRotateVoxelBlob;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.render.helpers.SimpleInstanceCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ItemNegativePrint extends Item implements IVoxelBlobItem, IItemScrollWheel, IPatternItem {

    // add info cached info
    SimpleInstanceCache<ItemStack, List<Component>> toolTipCache = new SimpleInstanceCache<>(null, new ArrayList<>());

    public ItemNegativePrint(Item.Properties properties) {
        super(properties);
    }

    @Environment(EnvType.CLIENT)
    protected void defaultAddInfo(
            final ItemStack stack, final Level worldIn, final List<Component> tooltip, final TooltipFlag advanced) {
        super.appendHoverText(stack, worldIn, tooltip, advanced);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(
            final ItemStack stack, final Level worldIn, final List<Component> tooltip, final TooltipFlag advanced) {
        defaultAddInfo(stack, worldIn, tooltip, advanced);
        ChiselsAndBits.getConfig()
                .getCommon()
                .helpText(
                        LocalStrings.HelpNegativePrint,
                        tooltip,
                        ClientSide.instance.getKeyName(Minecraft.getInstance().options.keyUse),
                        ClientSide.instance.getKeyName(Minecraft.getInstance().options.keyUse));

        if (isWritten(stack)) {
            if (ClientSide.instance.holdingShift()) {
                final List<Component> details = toolTipCache.getCached();

                if (toolTipCache.needsUpdate(stack)) {
                    details.clear();

                    final VoxelBlob blob = ModUtil.getBlobFromStack(stack, null);

                    final int solid = blob.filled();
                    final int air = blob.air();

                    if (solid > 0) {
                        details.add(Component.literal(Integer.valueOf(solid).toString())
                                .append(" ")
                                .append(Component.literal(LocalStrings.Filled.getLocal())));
                    }

                    if (air > 0) {
                        details.add(Component.literal(Integer.valueOf(air).toString())
                                .append(" ")
                                .append(Component.literal(LocalStrings.Empty.getLocal())));
                    }
                }

                tooltip.addAll(details);
            } else {
                tooltip.add(Component.literal(LocalStrings.ShiftDetails.getLocal()));
            }
        }
    }

    @Override
    public boolean isWritten(final ItemStack stack) {
        if (stack.getItem() != getWrittenItem()) {
            return false;
        }

        if (stack.hasTag()) {
            final boolean a = ModUtil.getSubCompound(stack, ModUtil.NBT_BLOCKENTITYTAG, false)
                            .size()
                    != 0;
            final boolean b = ModUtil.getTagCompound(stack).contains(NBTBlobConverter.NBT_LEGACY_VOXEL);
            final boolean c = ModUtil.getTagCompound(stack).contains(NBTBlobConverter.NBT_VERSIONED_VOXEL);
            return a || b || c;
        }
        return false;
    }

    protected Item getWrittenItem() {
        return ModItems.ITEM_NEGATIVE_PRINT_WRITTEN.get();
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        final InteractionHand hand = context.getHand();
        final Level world = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final Direction side = context.getClickedFace();

        final ItemStack stack = player.getItemInHand(hand);
        final BlockState blkstate = world.getBlockState(pos);

        if (ItemChiseledBit.checkRequiredSpace(player, blkstate)) {
            return InteractionResult.FAIL;
        }

        if (!player.mayUseItemAt(pos, side, stack) || !world.mayInteract(player, pos)) {
            return InteractionResult.FAIL;
        }

        if (!isWritten(stack)) {
            final CompoundTag comp = getCompoundFromBlock(world, pos, player);
            if (comp != null) {
                final int count = stack.getCount();
                stack.shrink(count);

                final ItemStack newStack = new ItemStack(this::getWrittenItem, count);
                newStack.setTag(comp);

                ItemEntity itementity = player.drop(newStack, false);
                if (itementity != null) {
                    itementity.setNoPickUpDelay();
                    itementity.setThrower(player);
                }
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.FAIL;
        }

        final TileEntityBlockChiseled te = ModUtil.getChiseledTileEntity(world, pos, false);
        if (te != null) {
            // we can do this!
        } else if (!BlockChiseled.replaceWithChiseled(world, pos, blkstate, true)) {
            return InteractionResult.FAIL;
        }

        final TileEntityBlockChiseled tec = ModUtil.getChiseledTileEntity(world, pos, true);
        if (tec != null) {
            final VoxelBlob vb = tec.getBlob();

            final VoxelBlob pattern = ModUtil.getBlobFromStack(stack, player);

            applyPrint(stack, world, pos, side, vb, pattern, player, hand);

            tec.completeEditOperation(vb);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    protected boolean convertToStone() {
        return true;
    }

    protected CompoundTag getCompoundFromBlock(final Level world, final BlockPos pos, final Player player) {

        final TileEntityBlockChiseled te = ModUtil.getChiseledTileEntity(world, pos, false);
        if (te != null) {
            final CompoundTag comp = new CompoundTag();
            te.writeChiselData(comp);

            if (convertToStone()) {
                final TileEntityBlockChiseled tmp = new TileEntityBlockChiseled(pos, world.getBlockState(pos));
                tmp.readChiselData(comp);

                final VoxelBlob bestBlob = tmp.getBlob();
                bestBlob.binaryReplacement(0, ModUtil.getStateId(Blocks.STONE.defaultBlockState()));

                tmp.setBlob(bestBlob);
                tmp.writeChiselData(comp);
            }

            comp.putByte(ModUtil.NBT_SIDE, (byte) ModUtil.getPlaceFace(player).ordinal());
            return comp;
        }

        return null;
    }

    @Override
    public ItemStack getPatternedItem(final ItemStack stack, final boolean craftingBlocks) {
        if (!isWritten(stack)) {
            return null;
        }

        final CompoundTag tag = ModUtil.getTagCompound(stack);

        // Detect and provide full blocks if pattern solid full and solid.
        final NBTBlobConverter conv = new NBTBlobConverter();
        conv.readChisleData(tag, VoxelBlob.VERSION_ANY);

        if (craftingBlocks
                && ChiselsAndBits.getConfig().getServer().fullBlockCrafting.get()) {
            final VoxelStats stats = conv.getBlob().getVoxelStats();
            if (stats.isFullBlock) {
                final BlockState state = ModUtil.getStateById(stats.mostCommonState);
                final ItemStack is = ModUtil.getItemStackFromBlockState(state);

                if (!ModUtil.isEmpty(is)) {
                    return is;
                }
            }
        }

        final BlockState state = conv.getPrimaryBlockState();
        final ItemStack itemstack = new ItemStack(ModBlocks.getChiseledBlock(), 1);

        itemstack.addTagElement(ModUtil.NBT_BLOCKENTITYTAG, tag);
        return itemstack;
    }

    protected void applyPrint(
            @NotNull final ItemStack stack,
            @NotNull final Level world,
            @NotNull final BlockPos pos,
            @NotNull final Direction side,
            @NotNull final VoxelBlob vb,
            @NotNull final VoxelBlob pattern,
            @NotNull final Player who,
            @NotNull final InteractionHand hand) {
        // snag a tool...
        final ActingPlayer player = ActingPlayer.actingAs(who, hand);
        final IContinuousInventory selected = new ContinousChisels(player, pos, side);
        ItemStack spawnedItem = null;

        final List<ItemEntity> spawnlist = new ArrayList<ItemEntity>();

        for (int z = 0; z < vb.detail && selected.isValid(); z++) {
            for (int y = 0; y < vb.detail && selected.isValid(); y++) {
                for (int x = 0; x < vb.detail && selected.isValid(); x++) {
                    final int blkID = vb.get(x, y, z);
                    if (blkID != 0 && pattern.get(x, y, z) == 0) {
                        spawnedItem = ItemChisel.chiselBlock(
                                selected, player, vb, world, pos, side, x, y, z, spawnedItem, spawnlist);
                    }
                }
            }
        }

        BitInventoryFeeder feeder = new BitInventoryFeeder(who, world);
        for (final ItemEntity ei : spawnlist) {
            feeder.addItem(ei);
        }
    }

    @Override
    public void scroll(final Player player, final ItemStack stack, final int dwheel) {
        final PacketRotateVoxelBlob p =
                new PacketRotateVoxelBlob(Axis.Y, dwheel > 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90);
        ChiselsAndBits.getNetworkChannel().sendToServer(p);
    }

    @Override
    public void rotate(final ItemStack stack, final Direction.Axis axis, final Rotation rotation) {
        Direction side = ModUtil.getSide(stack);

        if (axis == Axis.Y) {
            if (side.getAxis() == Axis.Y) {
                side = Direction.NORTH;
            }

            switch (rotation) {
                case CLOCKWISE_180:
                    side = side.getClockWise();
                case CLOCKWISE_90:
                    side = side.getClockWise();
                    break;
                case COUNTERCLOCKWISE_90:
                    side = side.getCounterClockWise();
                    break;
                default:
                case NONE:
                    break;
            }
        } else {
            IBitAccess ba = ChiselsAndBits.getApi().createBitItem(stack);
            ba.rotate(axis, rotation);
            stack.setTag(ba.getBitsAsItem(side, ChiselsAndBits.getApi().getItemType(stack), false)
                    .getTag());
        }

        ModUtil.setSide(stack, side);
    }
}
