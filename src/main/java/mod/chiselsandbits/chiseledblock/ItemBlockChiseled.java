package mod.chiselsandbits.chiseledblock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mod.chiselsandbits.api.ChiselsAndBitsEvents;
import mod.chiselsandbits.api.EventBlockBitModification;
import mod.chiselsandbits.api.IBitAccess;
import mod.chiselsandbits.chiseledblock.BlockChiseled.ReplaceWithChiseledValue;
import mod.chiselsandbits.chiseledblock.data.BitLocation;
import mod.chiselsandbits.chiseledblock.data.IntegerBox;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.UndoTracker;
import mod.chiselsandbits.components.ChiseledData;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.BitOperation;
import mod.chiselsandbits.helpers.DeprecationHelper;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.interfaces.IItemScrollWheel;
import mod.chiselsandbits.interfaces.IVoxelBlobItem;
import mod.chiselsandbits.items.ItemChiseledBit;
import mod.chiselsandbits.network.packets.PacketAccurateSneakPlace;
import mod.chiselsandbits.network.packets.PacketAccurateSneakPlace.IItemBlockAccurate;
import mod.chiselsandbits.network.packets.PacketRotateVoxelBlob;
import mod.chiselsandbits.registry.ModDataComponents;
import mod.chiselsandbits.render.helpers.SimpleInstanceCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemBlockChiseled extends BlockItem implements IVoxelBlobItem, IItemScrollWheel, IItemBlockAccurate {

    private static final Logger log = LoggerFactory.getLogger(ItemBlockChiseled.class);
    SimpleInstanceCache<ItemStack, List<Component>> tooltipCache =
            new SimpleInstanceCache<ItemStack, List<Component>>(null, new ArrayList<Component>());

    public ItemBlockChiseled(final Block block, Item.Properties builder) {
        super(block, builder);
    }

    public static boolean tryPlaceBlockAt(
            final @NotNull Block block,
            final @NotNull ItemStack stack,
            final @NotNull Player player,
            final @NotNull Level world,
            @NotNull BlockPos pos,
            final @NotNull Direction side,
            final @NotNull InteractionHand hand,
            final double hitX,
            final double hitY,
            final double hitZ,
            final BlockPos partial,
            final boolean modulateWorld) {
        final VoxelBlob[][][] blobs = new VoxelBlob[2][2][2];

        // you can't place empty blocks...
        if (!ModUtil.hasChiseledData(stack)) {
            return false;
        }

        final VoxelBlob source = ModUtil.getBlobFromStack(stack, player);

        final IntegerBox modelBounds = source.getBounds();
        BlockPos offset = partial == null || modelBounds == null
                ? new BlockPos(0, 0, 0)
                : ModUtil.getPartialOffset(side, partial, modelBounds);
        if (offset.getX() < 0) {
            pos = pos.offset(-1, 0, 0);
            offset = offset.offset(VoxelBlob.dim, 0, 0);
        }

        if (offset.getY() < 0) {
            pos = pos.offset(0, -1, 0);
            offset = offset.offset(0, VoxelBlob.dim, 0);
        }

        if (offset.getZ() < 0) {
            pos = pos.offset(0, 0, -1);
            offset = offset.offset(0, 0, VoxelBlob.dim);
        }

        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    blobs[x][y][z] = source.offset(
                            offset.getX() - source.detail * x,
                            offset.getY() - source.detail * y,
                            offset.getZ() - source.detail * z);
                    final int solids = blobs[x][y][z].filled();
                    if (solids > 0) {
                        final BlockPos bp = pos.offset(x, y, z);

                        final EventBlockBitModification bmm =
                                new EventBlockBitModification(world, bp, player, hand, stack, true);
                        ChiselsAndBitsEvents.BLOCK_BIT_MODIFICATION.invoker().handle(bmm);
                        // test permissions.
                        if (!world.mayInteract(player, bp) || bmm.isCancelled()) {
                            return false;
                        }

                        if (world.isEmptyBlock(bp)
                                || world.getBlockState(bp)
                                        .canBeReplaced(new BlockPlaceContext(
                                                player,
                                                hand,
                                                stack,
                                                new BlockHitResult(new Vec3(hitX, hitY, hitZ), side, pos, false)))) {
                            continue;
                        }

                        final TileEntityBlockChiseled target = ModUtil.getChiseledTileEntity(world, bp, true);
                        if (target != null) {
                            if (!target.canMerge(blobs[x][y][z])) {
                                return false;
                            }

                            blobs[x][y][z] = blobs[x][y][z].merge(target.getBlob());
                            continue;
                        }

                        return false;
                    }
                }
            }
        }

        if (modulateWorld) {
            UndoTracker.getInstance().beginGroup(player);
            try {
                for (int x = 0; x < 2; x++) {
                    for (int y = 0; y < 2; y++) {
                        for (int z = 0; z < 2; z++) {
                            if (blobs[x][y][z].filled() > 0) {
                                final BlockPos bp = pos.offset(x, y, z);
                                final BlockState state = world.getBlockState(bp);

                                if (world.getBlockState(bp)
                                        .canBeReplaced(new BlockPlaceContext(
                                                player,
                                                hand,
                                                stack,
                                                new BlockHitResult(
                                                        new Vec3(hitX, hitY, hitZ),
                                                        side,
                                                        bp,
                                                        false) // TODO: Figure is a recalc of the hit vector is needed
                                                // here.
                                                ))) {
                                    // clear it...

                                    world.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
                                }

                                if (world.isEmptyBlock(bp)) {
                                    final int commonBlock = blobs[x][y][z].getVoxelStats().mostCommonState;
                                    ReplaceWithChiseledValue rv =
                                            BlockChiseled.replaceWithChiseled(world, bp, state, commonBlock, true);
                                    if (rv.success && rv.te != null) {
                                        rv.te.completeEditOperation(blobs[x][y][z]);
                                    }

                                    continue;
                                }

                                final TileEntityBlockChiseled target = ModUtil.getChiseledTileEntity(world, bp, true);
                                if (target != null) {
                                    // Here
                                    target.completeEditOperation(blobs[x][y][z]);
                                    continue;
                                }

                                return false;
                            }
                        }
                    }
                }
            } finally {
                UndoTracker.getInstance().endGroup(player);
            }
        }

        return true;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final TooltipDisplay display,
            final Consumer<Component> tooltip,
            final TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        final List<Component> tooltipLines = new ArrayList<>();
        ChiselsAndBits.getConfig()
                .getCommon()
                .helpText(
                        LocalStrings.HelpChiseledBlock,
                        tooltipLines,
                        ClientSide.instance.getKeyName(Minecraft.getInstance().options.keyUse),
                        ClientSide.instance.getKeyName(ClientSide.getOffGridPlacementKey()));

        if (ModUtil.hasChiseledData(stack)) {
            if (ClientSide.instance.holdingShift()) {
                if (tooltipCache.needsUpdate(stack)) {
                    final VoxelBlob blob = ModUtil.getBlobFromStack(stack, null);
                    tooltipCache.updateCachedValue(blob.listContents(new ArrayList<>()));
                }

                tooltipLines.addAll(tooltipCache.getCached());
            } else {
                tooltipLines.add(Component.literal(LocalStrings.ShiftDetails.getLocal()));
            }
        }
        tooltipLines.forEach(tooltip);
    }

    @Override
    protected boolean canPlace(final BlockPlaceContext p_195944_1_, final BlockState p_195944_2_) {
        // TODO: Check for offgrid logic.
        return canPlaceBlockHere(
                p_195944_1_.getLevel(),
                p_195944_1_.getClickedPos(),
                p_195944_1_.getClickedFace(),
                p_195944_1_.getPlayer(),
                p_195944_1_.getHand(),
                p_195944_1_.getItemInHand(),
                p_195944_1_.getClickLocation().x,
                p_195944_1_.getClickLocation().y,
                p_195944_1_.getClickLocation().z,
                false);
    }

    public boolean vanillaStylePlacementTest(
            final @NotNull Level worldIn,
            @NotNull BlockPos pos,
            @NotNull Direction side,
            final Player player,
            final InteractionHand hand,
            final ItemStack stack) {
        final Block block = worldIn.getBlockState(pos).getBlock();

        if (block == Blocks.SNOW) {
            side = Direction.UP;
        } else if (!worldIn.getBlockState(pos)
                .canBeReplaced(new BlockPlaceContext(
                        player, hand, stack, new BlockHitResult(new Vec3(0.5, 0.5, 0.5), side, pos, false)))) {
            pos = pos.relative(side);
        }

        return true;
    }

    public boolean canPlaceBlockHere(
            final @NotNull Level worldIn,
            final @NotNull BlockPos pos,
            final @NotNull Direction side,
            final Player player,
            final InteractionHand hand,
            final ItemStack stack,
            final double hitX,
            final double hitY,
            final double hitZ,
            boolean offgrid) {
        if (vanillaStylePlacementTest(worldIn, pos, side, player, hand, stack)) {
            return true;
        }

        if (offgrid) {
            return true;
        }

        if (tryPlaceBlockAt(
                getBlock(),
                stack,
                player,
                worldIn,
                pos,
                side,
                InteractionHand.MAIN_HAND,
                hitX,
                hitY,
                hitZ,
                null,
                false)) {
            return true;
        }

        return tryPlaceBlockAt(
                getBlock(),
                stack,
                player,
                worldIn,
                pos.relative(side),
                side,
                InteractionHand.MAIN_HAND,
                hitX,
                hitY,
                hitZ,
                null,
                false);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final ItemStack stack = context.getPlayer().getItemInHand(context.getHand());

        if (!context.getLevel().isClientSide() && !(context.getPlayer() instanceof FakePlayer)) {
            // Say it "worked", Don't do anything we'll get a better packet.
            return InteractionResult.SUCCESS;
        }

        // send accurate packet.
        final PacketAccurateSneakPlace pasp = new PacketAccurateSneakPlace(
                context.getItemInHand(),
                context.getClickedPos(),
                context.getHand(),
                context.getClickedFace(),
                context.getClickLocation().x,
                context.getClickLocation().y,
                context.getClickLocation().z,
                ClientSide.offGridPlacement(context.getPlayer()) // TODO: Figure out the placement logic.
                );

        ChiselsAndBits.getNetworkChannel().sendToServer(pasp);
        // constructor of BlockPlaceContext set its block pos to relative block pos of original, this cause placed
        // chiseled block over 1 bit
        return tryPlace(new BlockPlaceContext(context), ClientSide.offGridPlacement(context.getPlayer()));
    }

    @Override
    public InteractionResult place(final BlockPlaceContext context) {
        return tryPlace(context, false);
    }

    @Override
    protected boolean placeBlock(final BlockPlaceContext context, final BlockState state) {
        return placeBitBlock(
                context.getItemInHand(),
                context.getPlayer(),
                context.getLevel(),
                context.getClickedPos(),
                context.getClickedFace(),
                context.getClickLocation().x,
                context.getClickLocation().y,
                context.getClickLocation().z,
                state,
                false);
    }

    public boolean placeBitBlock(
            final ItemStack stack,
            final Player player,
            final Level world,
            final BlockPos pos,
            final Direction side,
            final double hitX,
            final double hitY,
            final double hitZ,
            final BlockState newState,
            boolean offgrid) {
        if (offgrid) {

            final BitLocation bl = new BitLocation(
                    new BlockHitResult(new Vec3(hitX, hitY, hitZ), side, pos.relative(side.getOpposite()), false),
                    BitOperation.PLACE);
            return tryPlaceBlockAt(
                    getBlock(),
                    stack,
                    player,
                    world,
                    bl.blockPos,
                    side,
                    InteractionHand.MAIN_HAND,
                    hitX,
                    hitY,
                    hitZ,
                    new BlockPos(bl.bitX, bl.bitY, bl.bitZ),
                    true);
        } else {
            return tryPlaceBlockAt(
                    getBlock(),
                    stack,
                    player,
                    world,
                    pos,
                    side,
                    InteractionHand.MAIN_HAND,
                    hitX,
                    hitY,
                    hitZ,
                    null,
                    true);
        }
    }

    @Override
    public Component getName(final ItemStack stack) {
        final ChiseledData data = NBTBlobConverter.getComponent(stack);
        if (data != null) {
            final NBTBlobConverter converter = new NBTBlobConverter();
            converter.readChisleData(data, VoxelBlob.VERSION_ANY);

            final BlockState state = converter.getPrimaryBlockState();
            final Component name = ItemChiseledBit.getBitStateName(state);

            if (name != null) {
                final Component parent = super.getName(stack);
                if (!(parent instanceof MutableComponent formattedParent)) {
                    return parent;
                }

                return formattedParent.append(" - ").append(name);
            }
        }

        return super.getName(stack);
    }

    @Override
    public void scroll(final Player player, final ItemStack stack, final int dwheel) {
        final PacketRotateVoxelBlob p = new PacketRotateVoxelBlob(
                Direction.Axis.Y, dwheel > 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90);
        ChiselsAndBits.getNetworkChannel().sendToServer(p);
    }

    @Override
    public void rotate(final ItemStack stack, final Direction.Axis axis, final Rotation rotation) {
        Direction side = ModUtil.getSide(stack);

        if (axis == Axis.Y) {
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
            final ItemStack rotated =
                    ba.getBitsAsItem(side, ChiselsAndBits.getApi().getItemType(stack), false);
            final ChiseledData rotatedData = NBTBlobConverter.getComponent(rotated);
            if (rotatedData == null) {
                stack.remove(ModDataComponents.CHISELED_DATA);
            } else {
                stack.set(ModDataComponents.CHISELED_DATA, rotatedData);
            }
        }

        ModUtil.setSide(stack, side);
    }

    @Override
    public InteractionResult tryPlace(final UseOnContext context, final boolean offgrid) {
        final BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        final Block block = state.getBlock();

        Direction side = context.getClickedFace();
        BlockPos pos = context.getClickedPos();

        if (block == Blocks.SNOW && state.getValue(SnowLayerBlock.LAYERS).intValue() < 1) {
            side = Direction.UP;
        } else {
            boolean canMerge = false;
            if (ModUtil.hasChiseledData(context.getItemInHand())) {
                final TileEntityBlockChiseled tebc =
                        ModUtil.getChiseledTileEntity(context.getLevel(), context.getClickedPos(), true);

                if (tebc != null) {
                    final VoxelBlob blob = ModUtil.getBlobFromStack(context.getItemInHand(), context.getPlayer());
                    canMerge = tebc.canMerge(blob);
                }
            }

            BlockPlaceContext replacementCheckContext =
                    context instanceof BlockPlaceContext ? (BlockPlaceContext) context : new BlockPlaceContext(context);
            if (context.getPlayer()
                            .level()
                            .getBlockState(context.getClickedPos())
                            .getBlock()
                    instanceof BlockChiseled) {
                replacementCheckContext = new DirectionalPlaceContext(
                        context.getLevel(), pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP);
            }

            if (!canMerge && !offgrid && !state.canBeReplaced(replacementCheckContext)) {
                pos = pos.relative(side);
            }
        }

        if (ModUtil.isEmpty(context.getItemInHand())) {
            return InteractionResult.FAIL;
        } else if (!context.getPlayer().mayUseItemAt(pos, side, context.getItemInHand())) {
            return InteractionResult.FAIL;
        } else if (pos.getY() == 255
                && DeprecationHelper.getStateFromItem(context.getItemInHand()).isSolid()) {
            return InteractionResult.FAIL;
        } else if (context instanceof BlockPlaceContext
                && canPlaceBlockHere(
                        context.getLevel(),
                        pos,
                        side,
                        context.getPlayer(),
                        context.getHand(),
                        context.getItemInHand(),
                        context.getClickLocation().x,
                        context.getClickLocation().y,
                        context.getClickLocation().z,
                        offgrid)) {
            final BlockState BlockState1 = getPlacementState((BlockPlaceContext) context);

            if (placeBitBlock(
                    context.getItemInHand(),
                    context.getPlayer(),
                    context.getLevel(),
                    pos,
                    side,
                    context.getClickLocation().x,
                    context.getClickLocation().y,
                    context.getClickLocation().z,
                    BlockState1,
                    offgrid)) {
                context.getLevel()
                        .playLocalSound(
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5,
                                DeprecationHelper.getSoundType(this.getBlock()).getPlaceSound(),
                                SoundSource.BLOCKS,
                                (DeprecationHelper.getSoundType(this.getBlock()).getVolume() + 1.0F) / 2.0F,
                                DeprecationHelper.getSoundType(this.getBlock()).getPitch() * 0.8F,
                                false);

                if (!context.getPlayer().isCreative()
                        && context.getItemInHand().getItem() instanceof ItemBlockChiseled) {
                    ModUtil.adjustStackSize(context.getItemInHand(), -1);
                }

                return InteractionResult.SUCCESS;
            }

            return InteractionResult.FAIL;
        } else {
            return InteractionResult.FAIL;
        }
    }
}
