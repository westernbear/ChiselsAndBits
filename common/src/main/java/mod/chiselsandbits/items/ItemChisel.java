package mod.chiselsandbits.items;

import static net.minecraft.world.item.ToolMaterial.DIAMOND;
import static net.minecraft.world.item.ToolMaterial.GOLD;
import static net.minecraft.world.item.ToolMaterial.IRON;
import static net.minecraft.world.item.ToolMaterial.NETHERITE;
import static net.minecraft.world.item.ToolMaterial.WOOD;

import com.google.common.base.Stopwatch;
import dev.architectury.injectables.annotations.Environment;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.data.BitLocation;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.ActingPlayer;
import mod.chiselsandbits.helpers.BitOperation;
import mod.chiselsandbits.helpers.ChiselModeManager;
import mod.chiselsandbits.helpers.ChiselToolType;
import mod.chiselsandbits.helpers.IContinuousInventory;
import mod.chiselsandbits.helpers.IItemInInventory;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.interfaces.IChiselModeItem;
import mod.chiselsandbits.interfaces.IItemScrollWheel;
import mod.chiselsandbits.modes.ChiselMode;
import mod.chiselsandbits.modes.IToolMode;
import mod.chiselsandbits.network.packets.PacketChisel;
import mod.chiselsandbits.registry.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class ItemChisel extends Item implements IItemScrollWheel, IChiselModeItem {
    private static final float one_16th = 1.0f / 16.0f;
    private static Stopwatch timer;
    private static boolean testingChisel = false;
    private final ToolMaterial material;

    public ItemChisel(final ToolMaterial material, final Item.Properties properties) {
        super(properties.tool(material, ModTags.Blocks.CHISELED_BLOCK, 0.1F, -2.8F, 0.0F));
        this.material = material;
    }

    public int getMaterialLevel() {
        if (NETHERITE.equals(material)) {
            return 4;
        }
        if (DIAMOND.equals(material)) {
            return 3;
        }
        if (IRON.equals(material)) {
            return 2;
        }
        if (GOLD.equals(material) || WOOD.equals(material)) {
            return 0;
        }
        return 1;
    }

    public static void resetDelay() {
        timer = null;
    }

    public static boolean fromBreakToChisel(
            final ChiselMode mode,
            final ItemStack itemstack,
            final @NotNull BlockPos pos,
            final Player player,
            final InteractionHand hand) {
        final BlockState state = player.level().getBlockState(pos);
        if (ItemChiseledBit.checkRequiredSpace(player, state)) {
            return false;
        }
        if (BlockBitInfo.canChisel(state)) {
            if (itemstack != null && (timer == null || timer.elapsed(TimeUnit.MILLISECONDS) > 150)) {
                timer = Stopwatch.createStarted();
                if (mode == ChiselMode.DRAWN_REGION) {
                    final Pair<Vec3, Vec3> PlayerRay = ModUtil.getPlayerRay(player);
                    final Vec3 ray_from = PlayerRay.getLeft();
                    final Vec3 ray_to = PlayerRay.getRight();

                    final ClipContext context =
                            new ClipContext(ray_from, ray_to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player);

                    final HitResult mop = player.level().clip(context);
                    if (mop != null && mop instanceof BlockHitResult rayTraceResult) {
                        final BitLocation loc = new BitLocation(rayTraceResult, BitOperation.CHISEL);
                        ClientSide.instance.pointAt(ChiselToolType.CHISEL, loc, hand);
                        return true;
                    }

                    return true;
                }

                if (!player.level().isClientSide()) {
                    return true;
                }

                final Pair<Vec3, Vec3> PlayerRay = ModUtil.getPlayerRay(player);
                final Vec3 ray_from = PlayerRay.getLeft();
                final Vec3 ray_to = PlayerRay.getRight();
                final ClipContext context =
                        new ClipContext(ray_from, ray_to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);

                BlockHitResult mop = player.level().clip(context);
                if (mop.getType() != HitResult.Type.MISS) {
                    if ((Minecraft.getInstance().hitResult != null
                                    ? Minecraft.getInstance().hitResult.getType()
                                    : HitResult.Type.MISS)
                            == HitResult.Type.BLOCK) {
                        BlockHitResult minecraftResult = (BlockHitResult) Minecraft.getInstance().hitResult;
                        if (!minecraftResult
                                .getBlockPos()
                                .immutable()
                                .equals(mop.getBlockPos().immutable())) {
                            mop = minecraftResult;
                        }
                    }

                    useChisel(mode, player, player.level(), mop, hand);
                }
            }

            return true;
        }

        if (player.level().isClientSide()) {
            return ClientSide.instance.getStartPos() != null;
        }

        return false;
    }

    static void useChisel(
            final ChiselMode mode,
            final Player player,
            final Level world,
            final BlockHitResult rayTraceResult,
            final InteractionHand hand) {
        final BitLocation location = new BitLocation(rayTraceResult, BitOperation.CHISEL);

        final PacketChisel pc =
                new PacketChisel(BitOperation.CHISEL, location, rayTraceResult.getDirection(), mode, hand);

        final int extractedState = pc.doAction(player);
        if (extractedState != 0) {
            ClientSide.breakSound(world, rayTraceResult.getBlockPos(), extractedState);

            ChiselsAndBits.getNetworkChannel().sendToServer(pc);
        }
    }

    /**
     * Modifies VoxelData of TileEntityChiseled
     *
     * @param selected
     * @param player
     * @param vb
     * @param world
     * @param pos
     * @param side
     * @param x
     * @param y
     * @param z
     * @param output
     * @return
     */
    public static ItemStack chiselBlock(
            final IContinuousInventory selected,
            final ActingPlayer player,
            final VoxelBlob vb,
            final Level world,
            final BlockPos pos,
            final Direction side,
            final int x,
            final int y,
            final int z,
            ItemStack output,
            final List<ItemEntity> spawnlist) {
        final boolean isCreative = player.isCreative();

        final int blk = vb.get(x, y, z);
        if (blk == 0) {
            return output;
        }

        if (!canMine(selected, ModUtil.getStateById(blk), player.getPlayer(), world, pos)) {
            return output;
        }

        if (!selected.useItem(blk)) {
            return output;
        }

        if (!world.isClientSide() && !isCreative) {
            double hitX = x * one_16th;
            double hitY = y * one_16th;
            double hitZ = z * one_16th;

            final double offset = 0.5;
            hitX += side.getStepX() * offset;
            hitY += side.getStepY() * offset;
            hitZ += side.getStepZ() * offset;

            if (output == null || !ItemChiseledBit.sameBit(output, blk) || ModUtil.getStackSize(output) == 64) {
                output = ItemChiseledBit.createStack(blk, 1, true);

                spawnlist.add(new ItemEntity(world, pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ, output));
            } else {
                ModUtil.adjustStackSize(output, 1);
            }
        } else {
            // return value...
            output = ItemChiseledBit.createStack(blk, 1, true);
        }

        vb.clear(x, y, z);
        return output;
    }

    public static boolean canMine(
            final IContinuousInventory chiselInv,
            final BlockState state,
            final Player player,
            final Level world,
            final @NotNull BlockPos pos) {
        final int targetState = ModUtil.getStateId(state);
        IItemInInventory chiselSlot = chiselInv.getItem(targetState);
        ItemStack chisel = chiselSlot.getStack();

        if (player.isCreative()) {
            return world.mayInteract(player, pos);
        }

        if (ModUtil.isEmpty(chisel)) {
            return false;
        }

        if (ChiselsAndBits.getConfig().getServer().enableChiselToolHarvestCheck.get()) {
            // this is the earily check.
            if (state.getBlock() instanceof BlockChiseled) {
                return ((BlockChiseled) state.getBlock()).basicHarvestBlockTest(world, pos, player);
            }

            do {
                final Block blk = world.getBlockState(pos).getBlock();
                BlockChiseled.setActingAs(state);
                testingChisel = true;
                chiselSlot.swapWithWeapon();
                boolean canHarvest = blk instanceof BlockChiseled block
                        && block.canHarvestBlock(world.getBlockState(pos), world, pos, player);
                chiselSlot.swapWithWeapon();
                testingChisel = false;
                BlockChiseled.setActingAs(null);

                if (canHarvest) {
                    return true;
                }

                chiselInv.fail(targetState);

                chiselSlot = chiselInv.getItem(targetState);
                chisel = chiselSlot.getStack();
            } while (!ModUtil.isEmpty(chisel));

            return false;
        }

        return true;
    }

    @Environment(Env.CLIENT)
    @Override
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final TooltipDisplay display,
            final Consumer<Component> tooltip,
            final TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        final List<Component> helpText = new ArrayList<>();
        ChiselsAndBits.getConfig()
                .getCommon()
                .helpText(
                        LocalStrings.HelpChisel,
                        helpText,
                        ClientSide.instance.getKeyName(Minecraft.getInstance().options.keyAttack),
                        ClientSide.instance.getModeKey());
        helpText.forEach(tooltip);
    }

    @Override
    public boolean canDestroyBlock(
            ItemStack stack, BlockState blockState, Level level, BlockPos pos, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return true;
        }

        return ItemChisel.fromBreakToChisel(
                ChiselMode.castMode(
                        ChiselModeManager.getChiselMode(player, ChiselToolType.CHISEL, InteractionHand.MAIN_HAND)),
                stack,
                pos,
                player,
                InteractionHand.MAIN_HAND);
    }

    @Override
    public Component getName(ItemStack itemStack) {
        Component displayName = super.getName(itemStack);
        if (Platform.getEnvironment() == Env.CLIENT
                && ChiselsAndBits.getConfig().getClient().itemNameModeDisplay.get()
                && displayName instanceof MutableComponent formattableTextComponent) {
            if (ChiselsAndBits.getConfig().getClient().perChiselMode.get() || Platform.getEnvironment() == Env.SERVER) {
                return formattableTextComponent
                        .append(" - ")
                        .append(ChiselMode.getMode(itemStack).string.getLocal());
            } else {
                return formattableTextComponent
                        .append(" - ")
                        .append(ChiselModeManager.getChiselMode(
                                        ClientSide.instance.getPlayer(),
                                        ChiselToolType.CHISEL,
                                        InteractionHand.MAIN_HAND)
                                .getName()
                                .getLocal());
            }
        }
        return displayName;
    }

    @Override
    public InteractionResult use(final Level worldIn, final Player playerIn, final InteractionHand hand) {
        if (worldIn.isClientSide()
                && ChiselsAndBits.getConfig()
                        .getClient()
                        .enableRightClickModeChange
                        .get()) {
            final IToolMode mode = ChiselModeManager.getChiselMode(playerIn, ChiselToolType.CHISEL, hand);
            ChiselModeManager.scrollOption(ChiselToolType.CHISEL, mode, mode, playerIn.isShiftKeyDown() ? -1 : 1);
            return InteractionResult.SUCCESS;
        }

        return super.use(worldIn, playerIn, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()
                && ChiselsAndBits.getConfig()
                        .getClient()
                        .enableRightClickModeChange
                        .get()) {
            use(context.getLevel(), context.getPlayer(), context.getHand());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public boolean isCorrectToolForDrops(final ItemStack stack, final BlockState blk) {
        Item it;

        final ToolMaterial tier = material;
        if (DIAMOND.equals(tier)) {
            it = Items.DIAMOND_PICKAXE;
        } else if (GOLD.equals(tier)) {
            it = Items.GOLDEN_PICKAXE;
        } else if (IRON.equals(tier)) {
            it = Items.IRON_PICKAXE;
        } else if (WOOD.equals(tier)) {
            it = Items.WOODEN_PICKAXE;
        } else {
            it = Items.STONE_PICKAXE;
        }

        return blk.getBlock() instanceof BlockChiseled || it.isCorrectToolForDrops(new ItemStack(it), blk);
    }

    //    @Override
    //    public int getHarvestLevel(final ItemStack stack, final ToolType tool, @Nullable final Player player,
    // @Nullable final BlockState blockState)
    //    {
    //        if ( testingChisel && stack.getItem() instanceof ItemChisel )
    //        {
    //            final String pattern = "(^|,)" + Pattern.quote( tool.getName() ) + "(,|$)";
    //
    //            final Pattern p = Pattern.compile( pattern );
    //            final Matcher m = p.matcher(
    // ChiselsAndBits.getConfig().getServer().enableChiselToolHarvestCheckTools.get() );
    //
    //            if ( m.find() )
    //            {
    //                final ItemChisel ic = (ItemChisel) stack.getItem();
    //                return ic.getTier().getLevel();
    //            }
    //        }
    //
    //        return super.getHarvestLevel( stack, tool, player, blockState );
    //    }

    @Override
    public void scroll(final Player player, final ItemStack stack, final int dwheel) {
        final IToolMode mode =
                ChiselModeManager.getChiselMode(player, ChiselToolType.CHISEL, InteractionHand.MAIN_HAND);
        ChiselModeManager.scrollOption(ChiselToolType.CHISEL, mode, mode, dwheel);
    }
}
