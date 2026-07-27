package mod.chiselsandbits.items;

import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import mod.chiselsandbits.api.ReplacementStateHandler;
import mod.chiselsandbits.bitstorage.BlockBitStorage;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.BlockChiseled.ReplaceWithChiseledValue;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.BitLocation;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.core.Log;
import mod.chiselsandbits.events.TickHandler;
import mod.chiselsandbits.helpers.ActingPlayer;
import mod.chiselsandbits.helpers.BitOperation;
import mod.chiselsandbits.helpers.ChiselModeManager;
import mod.chiselsandbits.helpers.ChiselToolType;
import mod.chiselsandbits.helpers.DeprecationHelper;
import mod.chiselsandbits.helpers.IContinuousInventory;
import mod.chiselsandbits.helpers.IItemInInventory;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.interfaces.ICacheClearable;
import mod.chiselsandbits.interfaces.IChiselModeItem;
import mod.chiselsandbits.interfaces.IItemScrollWheel;
import mod.chiselsandbits.items.ItemBitBag.BagPos;
import mod.chiselsandbits.modes.ChiselMode;
import mod.chiselsandbits.modes.IToolMode;
import mod.chiselsandbits.network.packets.PacketChisel;
import mod.chiselsandbits.registry.ModDataComponents;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.utils.FluidUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class ItemChiseledBit extends Item implements IItemScrollWheel, IChiselModeItem, ICacheClearable {

    private static final NonNullList<ItemStack> alternativeStacks = NonNullList.create();
    public static boolean bitBagStackLimitHack;
    private static Stopwatch timer;
    private ArrayList<ItemStack> bits;

    public ItemChiseledBit(Item.Properties properties) {
        super(properties);
        ChiselsAndBits.getInstance().addClearable(this);
    }

    public static Component getBitStateName(final BlockState state) {
        ItemStack target = null;
        Block blk = null;

        if (state == null) {
            return Component.literal("Null");
        }

        try {
            // for an unknown reason its possible to generate mod blocks without
            // proper state here...
            blk = state.getBlock();

            final Item item = Item.byBlock(blk);
            if (ModUtil.isEmpty(item)) {
                final Fluid f = BlockBitInfo.getFluidFromBlock(blk);
                if (f != null) {
                    return Component.translatable(FluidUtil.getTranslationKey(f));
                }
            } else {
                target = ChiselsAndBits.getApi().getItemStackForState(state);
            }
        } catch (final IllegalArgumentException e) {
            Log.logError("Unable to get Item Details for Bit.", e);
        }

        if (target == null || target.getItem() == null) {
            return null;
        }

        try {
            final Component myName = target.getHoverName();
            if (!(myName instanceof MutableComponent formattableName)) {
                return myName;
            }

            final Set<String> extra = new HashSet<String>();
            if (blk != null && state != null) {
                for (final Property<?> p : state.getProperties()) {
                    if (p.getName().equals("axis") || p.getName().equals("facing")) {
                        extra.add(DeprecationHelper.translateToLocal(
                                "mod.chiselsandbits.pretty." + p.getName() + "-" + state.getValue(p)));
                    }
                }
            }

            if (extra.isEmpty()) {
                return myName;
            }

            for (final String x : extra) {
                formattableName.append(" ").append(x);
            }

            return formattableName;
        } catch (final Exception e) {
            return Component.literal("Error");
        }
    }

    public static Component getBitTypeName(final ItemStack stack) {
        int stateID = ItemChiseledBit.getStackState(stack);
        if (stateID == 0) {
            // We are running an empty bit, for display purposes.
            // Lets loop:
            if (alternativeStacks.isEmpty()) {
                ModItems.ITEM_BLOCK_BIT.get().fillItemCategory(alternativeStacks);
            }

            stateID = ItemChiseledBit.getStackState(alternativeStacks.get(
                    (int) (TickHandler.getClientTicks() % ((alternativeStacks.size() * 20L)) / 20L)));

            if (stateID == 0) {
                alternativeStacks.clear();
            }
        }

        return getBitStateName(ModUtil.getStateById(stateID));
    }

    public static BitOperation getBitOperation(final Player player, final InteractionHand hand, final ItemStack stack) {
        return ReplacementStateHandler.getInstance().isReplacing() ? BitOperation.REPLACE : BitOperation.PLACE;
    }

    public static boolean sameBit(final ItemStack output, final int blk) {
        return output.has(ModDataComponents.BIT_STATE) && getStackState(output) == blk;
    }

    public static @NotNull ItemStack createStack(final int id, final int count, final boolean RequireStack) {
        final ItemStack out = new ItemStack(ModItems.ITEM_BLOCK_BIT.get(), count);
        out.set(ModDataComponents.BIT_STATE, id);
        return out;
    }

    public static int getStackState(final ItemStack inHand) {
        if (inHand == null) {
            return 0;
        }

        final Integer component = inHand.get(ModDataComponents.BIT_STATE);
        if (component != null) {
            return component;
        }

        final int legacy = ModUtil.getTagCompound(inHand).getIntOr("id", 0);
        if (legacy != 0) {
            inHand.set(ModDataComponents.BIT_STATE, legacy);
        }
        return legacy;
    }

    public static boolean placeBit(
            final IContinuousInventory bits,
            final ActingPlayer player,
            final VoxelBlob vb,
            final int x,
            final int y,
            final int z) {
        if (vb.get(x, y, z) == 0) {
            final IItemInInventory slot = bits.getItem(0);
            final int stateID = ItemChiseledBit.getStackState(slot.getStack());

            if (slot.isValid()) {
                if (!player.isCreative()) {
                    if (bits.useItem(stateID)) {
                        vb.set(x, y, z, stateID);
                    }
                } else {
                    vb.set(x, y, z, stateID);
                }
            }

            return true;
        }

        return false;
    }

    public static boolean hasBitSpace(final Player player, final int blk) {
        final List<BagPos> bags = ItemBitBag.getBags(player.getInventory());
        for (final BagPos bp : bags) {
            for (int x = 0; x < bp.inv.getContainerSize(); x++) {
                final ItemStack is = bp.inv.getItem(x);
                if ((ItemChiseledBit.sameBit(is, blk) && ModUtil.getStackSize(is) < bp.inv.getMaxStackSize())
                        || ModUtil.isEmpty(is)) {
                    return true;
                }
            }
        }
        for (int x = 0; x < 36; x++) {
            final ItemStack is = player.getInventory().getItem(x);
            if ((ItemChiseledBit.sameBit(is, blk) && ModUtil.getStackSize(is) < is.getMaxStackSize())
                    || ModUtil.isEmpty(is)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkRequiredSpace(final Player player, final BlockState blkstate) {
        if (ChiselsAndBits.getConfig().getServer().requireBagSpace.get() && !player.isCreative()) {
            // Cycle every item in any bag, if the player can't store the clicked block then
            // send them a message.
            final int stateId = ModUtil.getStateId(blkstate);
            if (!ItemChiseledBit.hasBitSpace(player, stateId)) {
                if (player.level().isClientSide() && (timer == null || timer.elapsed(TimeUnit.MILLISECONDS) > 1000)) {
                    // Timer is client-sided so it doesn't have to be made player-specific
                    timer = Stopwatch.createStarted();
                    // Only client should handle messaging.
                    player.sendSystemMessage(Component.translatable("mod.chiselsandbits.result.require_bag"));
                }
                return true;
            }
        }
        return false;
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
                        LocalStrings.HelpBit,
                        tooltipLines,
                        ClientSide.instance.getKeyName(Minecraft.getInstance().options.keyAttack),
                        ClientSide.instance.getKeyName(Minecraft.getInstance().options.keyUse),
                        ClientSide.instance.getModeKey());

        final int stateId = ItemChiseledBit.getStackState(stack);
        if (stateId == 0) {
            tooltipLines.add(Component.literal(ChatFormatting.RED.toString()
                    + ChatFormatting.ITALIC
                    + LocalStrings.AnyHelpBit.getLocal()
                    + ChatFormatting.RESET));
        }
        tooltipLines.forEach(tooltip);
    }

    @Override
    public Component getName(final ItemStack stack) {
        final Component typeName = getBitTypeName(stack);

        if (typeName == null) {
            return super.getName(stack);
        }

        final MutableComponent strComponent = Component.literal("");
        return strComponent.append(super.getName(stack)).append(" - ").append(typeName);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        if (!context.getLevel().isClientSide()) {
            return InteractionResult.PASS;
        }

        final Pair<Vec3, Vec3> PlayerRay = ModUtil.getPlayerRay(context.getPlayer());
        final Vec3 ray_from = PlayerRay.getLeft();
        final Vec3 ray_to = PlayerRay.getRight();
        final ClipContext rtc = new ClipContext(
                ray_from, ray_to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context.getPlayer());

        final HitResult mop = context.getLevel().clip(rtc);
        if (mop != null) {
            final BlockHitResult rayTraceResult = (BlockHitResult) mop;
            return onItemUseInternal(
                    context.getPlayer(),
                    context.getLevel(),
                    context.getClickedPos(),
                    context.getHand(),
                    rayTraceResult);
        }

        return InteractionResult.FAIL;
    }

    public InteractionResult onItemUseInternal(
            final @NotNull Player player,
            final @NotNull Level world,
            final @NotNull BlockPos usedBlock,
            final @NotNull InteractionHand hand,
            final @NotNull BlockHitResult rayTraceResult) {
        final ItemStack stack = player.getItemInHand(hand);

        if (!player.mayUseItemAt(usedBlock, rayTraceResult.getDirection(), stack)) {
            return InteractionResult.FAIL;
        }

        // forward interactions to tank...
        final BlockState usedState = world.getBlockState(usedBlock);
        final Block blk = usedState.getBlock();
        if (blk instanceof BlockBitStorage) {
            if (usedState.useItemOn(stack, world, player, hand, rayTraceResult) == InteractionResult.SUCCESS) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }

        if (world.isClientSide()) {
            final IToolMode mode =
                    ChiselModeManager.getChiselMode(player, ClientSide.instance.getHeldToolType(hand), hand);
            final BitLocation bitLocation = new BitLocation(rayTraceResult, getBitOperation(player, hand, stack));

            BlockState blkstate = world.getBlockState(bitLocation.blockPos);
            TileEntityBlockChiseled tebc = ModUtil.getChiseledTileEntity(world, bitLocation.blockPos, true);
            ReplaceWithChiseledValue rv = null;
            if (tebc == null
                    && (rv = BlockChiseled.replaceWithChiseled(
                                    world, bitLocation.blockPos, blkstate, ItemChiseledBit.getStackState(stack), true))
                            .success) {
                blkstate = world.getBlockState(bitLocation.blockPos);
                tebc = rv.te;
            }

            if (tebc != null) {
                PacketChisel pc = null;
                if (mode == ChiselMode.DRAWN_REGION) {
                    if (world.isClientSide()) {
                        ClientSide.instance.pointAt(
                                getBitOperation(player, hand, stack).getToolType(), bitLocation, hand);
                    }
                    return InteractionResult.FAIL;
                } else {

                    pc = new PacketChisel(
                            getBitOperation(player, hand, stack),
                            bitLocation,
                            rayTraceResult.getDirection(),
                            ChiselMode.castMode(mode),
                            hand);
                }

                final int result = pc.doAction(player);

                if (result > 0) {
                    ClientSide.instance.setLastTool(ChiselToolType.BIT);
                    ChiselsAndBits.getNetworkChannel().sendToServer(pc);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canDestroyBlock(
            ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return super.canDestroyBlock(itemStack, blockState, level, blockPos, entity);
        }

        return ItemChisel.fromBreakToChisel(
                ChiselMode.castMode(
                        ChiselModeManager.getChiselMode(player, ChiselToolType.BIT, InteractionHand.MAIN_HAND)),
                itemStack,
                blockPos,
                player,
                InteractionHand.MAIN_HAND);
    }

    @Override
    public boolean isCorrectToolForDrops(final ItemStack stack, final BlockState blk) {
        return blk.getBlock() instanceof BlockChiseled || super.isCorrectToolForDrops(stack, blk);
    }

    @Override
    public void clearCache() {
        bits = null;
    }

    public void fillItemCategory(NonNullList<ItemStack> output) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof BlockChiseled) {
                continue;
            }

            for (BlockState possibleState : block.getStateDefinition().getPossibleStates()) {
                if (BlockBitInfo.canChisel(possibleState)) {
                    ItemStack itemStack = ItemChiseledBit.createStack(ModUtil.getStateId(possibleState), 1, true);
                    output.add(itemStack);
                }
            }

            BlockState blockState = block.defaultBlockState();

            if (block instanceof LiquidBlock liquidBlock) {
                Fluid fluid = blockState.getFluidState().getType();
                if (fluid instanceof FlowingFluid flowingFluid) {
                    blockState = flowingFluid.getSource().defaultFluidState().createLegacyBlock();
                }
            }

            if (BlockBitInfo.canChisel(blockState)) {
                ItemStack itemStack = ItemChiseledBit.createStack(ModUtil.getStateId(blockState), 1, true);
                output.add(itemStack);
            }
        }
    }

    @Override
    public void scroll(final Player player, final ItemStack stack, final int dwheel) {
        final IToolMode mode = ChiselModeManager.getChiselMode(player, ChiselToolType.BIT, InteractionHand.MAIN_HAND);
        ChiselModeManager.scrollOption(ChiselToolType.BIT, mode, mode, dwheel);
    }
}
