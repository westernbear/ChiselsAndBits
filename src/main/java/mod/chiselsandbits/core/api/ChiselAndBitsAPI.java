package mod.chiselsandbits.core.api;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import mod.chiselsandbits.api.APIExceptions.CannotBeChiseled;
import mod.chiselsandbits.api.APIExceptions.InvalidBitItem;
import mod.chiselsandbits.api.BlockProvider;
import mod.chiselsandbits.api.IBitAccess;
import mod.chiselsandbits.api.IBitBag;
import mod.chiselsandbits.api.IBitBrush;
import mod.chiselsandbits.api.IBitLocation;
import mod.chiselsandbits.api.IChiselAndBitsAPI;
import mod.chiselsandbits.api.ItemStackHandler;
import mod.chiselsandbits.api.ItemType;
import mod.chiselsandbits.api.ModKeyBinding;
import mod.chiselsandbits.api.ParameterType;
import mod.chiselsandbits.api.ParameterType.BooleanParam;
import mod.chiselsandbits.api.ParameterType.DoubleParam;
import mod.chiselsandbits.api.ParameterType.FloatParam;
import mod.chiselsandbits.api.ParameterType.IntegerParam;
import mod.chiselsandbits.bitbag.BagStorage;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.ItemBlockChiseled;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.BitLocation;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.RenderHelper;
import mod.chiselsandbits.client.UndoTracker;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.BitInventoryFeeder;
import mod.chiselsandbits.helpers.BitOperation;
import mod.chiselsandbits.helpers.DeprecationHelper;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.items.ItemBitBag;
import mod.chiselsandbits.items.ItemChisel;
import mod.chiselsandbits.items.ItemChiseledBit;
import mod.chiselsandbits.items.ItemMirrorPrint;
import mod.chiselsandbits.items.ItemNegativePrint;
import mod.chiselsandbits.items.ItemPositivePrint;
import mod.chiselsandbits.items.ItemWrench;
import mod.chiselsandbits.modes.ChiselMode;
import mod.chiselsandbits.modes.PositivePatternMode;
import mod.chiselsandbits.modes.TapeMeasureModes;
import mod.chiselsandbits.registry.ModItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ChiselAndBitsAPI implements IChiselAndBitsAPI {

    private final Map<Block, ItemStackHandler> itemStackHandlers = new ConcurrentHashMap<>();
    private final List<BlockProvider> stateProviders = new ArrayList<>();

    public Map<Block, ItemStackHandler> getItemStackHandlers() {
        return itemStackHandlers;
    }

    @Override
    public void registerBlockProvider(BlockProvider provider) {
        Objects.requireNonNull(provider, "BlockProvider cannot be null");
        this.stateProviders.add(provider);
    }

    public List<BlockProvider> getStateProviders() {
        return stateProviders;
    }

    @Override
    public void registerItemStackHandler(Block block, @NotNull ItemStackHandler provider) {
        itemStackHandlers.put(
                Objects.requireNonNull(block, "Block cannot be null"),
                Objects.requireNonNull(provider, "ItemStackHandler cannot be null"));
    }

    @Override
    public ItemStack getItemStackForState(final BlockState state) {
        final ItemStack stack = IChiselAndBitsAPI.super.getItemStackForState(state);
        if (state != null && !stack.isEmpty()) {
            final ItemStackHandler handler = itemStackHandlers.get(state.getBlock());
            if (handler != null) {
                handler.apply(state, stack);
            }
        }
        return stack;
    }

    @Override
    public boolean canBeChiseled(final Level world, final BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }

        final BlockState state = world.getBlockState(pos);
        return state.getBlock() == Blocks.AIR
                || BlockBitInfo.canChisel(state)
                || ModUtil.getChiseledTileEntity(world, pos, false) != null;
    }

    @Override
    public boolean isBlockChiseled(final Level world, final BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }

        return ModUtil.getChiseledTileEntity(world, pos, false) != null;
    }

    @Override
    public IBitAccess getBitAccess(final Level world, final BlockPos pos) throws CannotBeChiseled {
        if (world == null || pos == null) {
            throw new CannotBeChiseled();
        }

        final BlockState state = world.getBlockState(pos);
        if (BlockBitInfo.isSupported(state) && !(state.getBlock() instanceof BlockChiseled)) {
            final VoxelBlob blob = new VoxelBlob();
            blob.fill(ModUtil.getStateId(state));
            return new BitAccess(world, pos, blob, VoxelBlob.NULL_BLOB);
        }

        if (world.isEmptyBlock(pos)) {
            final VoxelBlob blob = new VoxelBlob();
            return new BitAccess(world, pos, blob, VoxelBlob.NULL_BLOB);
        }

        final TileEntityBlockChiseled te = ModUtil.getChiseledTileEntity(world, pos, true);
        if (te != null) {
            final VoxelBlob mask = new VoxelBlob();
            return new BitAccess(world, pos, te.getBlob(), mask);
        }

        throw new CannotBeChiseled();
    }

    @Override
    public IBitBrush createBrush(final ItemStack stack) throws InvalidBitItem {
        if (ModUtil.isEmpty(stack)) {
            return new BitBrush(0);
        }

        if (getItemType(stack) == ItemType.CHISELED_BIT) {
            final int stateID = ItemChiseledBit.getStackState(stack);
            final BlockState state = ModUtil.getStateById(stateID);

            if (state != null && BlockBitInfo.canChisel(state)) {
                return new BitBrush(stateID);
            }
        }

        throw new InvalidBitItem();
    }

    @Override
    public IBitLocation getBitPos(
            final double hitX,
            final double hitY,
            final double hitZ,
            final Direction side,
            final BlockPos pos,
            final boolean placement) {
        Objects.requireNonNull(side, "Direction cannot be null");
        Objects.requireNonNull(pos, "BlockPos cannot be null");
        final BlockHitResult mop = new BlockHitResult(new Vec3(hitX, hitY, hitZ), side, pos, false);
        return new BitLocation(mop, placement ? BitOperation.PLACE : BitOperation.CHISEL);
    }

    @Override
    public ItemType getItemType(final ItemStack stack) {
        if (ModUtil.isEmpty(stack)) {
            return null;
        }

        if (stack.getItem() instanceof ItemChiseledBit) {
            return ItemType.CHISELED_BIT;
        }

        if (stack.getItem() instanceof ItemBitBag) {
            return ItemType.BIT_BAG;
        }

        if (stack.getItem() instanceof ItemChisel) {
            return ItemType.CHISEL;
        }

        if (stack.getItem() instanceof ItemBlockChiseled) {
            return ItemType.CHISELED_BLOCK;
        }

        if (stack.getItem() instanceof ItemMirrorPrint) {
            return ItemType.MIRROR_DESIGN;
        }

        if (stack.getItem() instanceof ItemPositivePrint) {
            return ItemType.POSITIVE_DESIGN;
        }

        if (stack.getItem() instanceof ItemNegativePrint) {
            return ItemType.NEGATIVE_DESIGN;
        }

        if (stack.getItem() instanceof ItemWrench) {
            return ItemType.WRENCH;
        }

        return null;
    }

    @Override
    public IBitAccess createBitItem(final ItemStack stack) {
        if (ModUtil.isEmpty(stack)) {
            return new BitAccess(null, null, new VoxelBlob(), VoxelBlob.NULL_BLOB);
        }

        final ItemType type = getItemType(stack);
        if (type != null && type.isBitAccess) {
            final VoxelBlob blob = ModUtil.getBlobFromStack(stack, null);
            return new BitAccess(null, null, blob, VoxelBlob.NULL_BLOB);
        }

        if (stack.getItem() instanceof BlockItem) {
            final BlockState state = DeprecationHelper.getStateFromItem(stack);

            if (BlockBitInfo.canChisel(state)) {
                final VoxelBlob blob = new VoxelBlob();
                blob.fill(ModUtil.getStateId(state));
                return new BitAccess(null, null, blob, VoxelBlob.NULL_BLOB);
            }
        }

        return null;
    }

    @Override
    public IBitBrush createBrushFromState(final BlockState state) throws InvalidBitItem {
        if (state == null || state.getBlock() == Blocks.AIR) {
            return new BitBrush(0);
        }

        if (!BlockBitInfo.canChisel(state)) {
            throw new InvalidBitItem();
        }

        return new BitBrush(ModUtil.getStateId(state));
    }

    @Override
    public ItemStack getBitItem(final BlockState state) throws InvalidBitItem {
        if (state == null || !BlockBitInfo.canChisel(state)) {
            throw new InvalidBitItem();
        }

        return ItemChiseledBit.createStack(ModUtil.getStateId(state), 1, true);
    }

    @Override
    public void giveBitToPlayer(final Player player, final ItemStack stack, Vec3 spawnPos) {
        if (ModUtil.isEmpty(stack)) {
            return;
        }

        Objects.requireNonNull(player, "Player cannot be null");

        if (spawnPos == null) {
            spawnPos = new Vec3(player.getX(), player.getY(), player.getZ());
        }

        final ItemEntity ei = new ItemEntity(player.level(), spawnPos.x, spawnPos.y, spawnPos.z, stack);

        if (stack.getItem() == ModItems.ITEM_BLOCK_BIT.get()) {
            if (player.level().isClientSide()) {
                return;
            }

            BitInventoryFeeder feeder = new BitInventoryFeeder(player, player.level());
            feeder.addItem(ei);
        } else if (!player.getInventory().add(stack)) {
            ei.setItem(stack);
            player.level().addFreshEntity(ei);
        }
    }

    @Override
    public IBitBag getBitbag(final ItemStack stack) {
        return !ModUtil.isEmpty(stack) && stack.getItem() instanceof ItemBitBag ? new BagStorage(stack) : null;
    }

    @Override
    public void beginUndoGroup(final Player player) {
        UndoTracker.getInstance().beginGroup(Objects.requireNonNull(player, "Player cannot be null"));
    }

    @Override
    public void endUndoGroup(final Player player) {
        UndoTracker.getInstance().endGroup(Objects.requireNonNull(player, "Player cannot be null"));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public KeyMapping getKeyBinding(ModKeyBinding modKeyBinding) {
        Objects.requireNonNull(modKeyBinding, "ModKeyBinding cannot be null");
        switch (modKeyBinding) {
            case SINGLE:
                return (KeyMapping) ChiselMode.SINGLE.binding;
            case SNAP2:
                return (KeyMapping) ChiselMode.SNAP2.binding;
            case SNAP4:
                return (KeyMapping) ChiselMode.SNAP4.binding;
            case SNAP8:
                return (KeyMapping) ChiselMode.SNAP8.binding;
            case LINE:
                return (KeyMapping) ChiselMode.LINE.binding;
            case PLANE:
                return (KeyMapping) ChiselMode.PLANE.binding;
            case CONNECTED_PLANE:
                return (KeyMapping) ChiselMode.CONNECTED_PLANE.binding;
            case CUBE_SMALL:
                return (KeyMapping) ChiselMode.CUBE_SMALL.binding;
            case CUBE_MEDIUM:
                return (KeyMapping) ChiselMode.CUBE_MEDIUM.binding;
            case CUBE_LARGE:
                return (KeyMapping) ChiselMode.CUBE_LARGE.binding;
            case SAME_MATERIAL:
                return (KeyMapping) ChiselMode.SAME_MATERIAL.binding;
            case DRAWN_REGION:
                return (KeyMapping) ChiselMode.DRAWN_REGION.binding;
            case CONNECTED_MATERIAL:
                return (KeyMapping) ChiselMode.CONNECTED_MATERIAL.binding;
            case REPLACE:
                return (KeyMapping) PositivePatternMode.REPLACE.binding;
            case ADDITIVE:
                return (KeyMapping) PositivePatternMode.ADDITIVE.binding;
            case PLACEMENT:
                return (KeyMapping) PositivePatternMode.PLACEMENT.binding;
            case IMPOSE:
                return (KeyMapping) PositivePatternMode.IMPOSE.binding;
            case BIT:
                return (KeyMapping) TapeMeasureModes.BIT.binding;
            case BLOCK:
                return (KeyMapping) TapeMeasureModes.BLOCK.binding;
            case DISTANCE:
                return (KeyMapping) TapeMeasureModes.DISTANCE.binding;
            default:
                return ClientSide.instance.getKeyBinding(modKeyBinding);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getParameter(final ParameterType<T> which) {
        Objects.requireNonNull(which, "ParameterType cannot be null");

        final Object value;
        if (which == BooleanParam.ENABLE_DAMAGE_TOOLS) {
            value = ChiselsAndBits.getConfig().getServer().damageTools.get();
        } else if (which == BooleanParam.ENABLE_BIT_LIGHT_SOURCE) {
            value = ChiselsAndBits.getConfig().getServer().enableBitLightSource.get();
        } else if (which == DoubleParam.BIT_MAX_DRAWN_REGION_SIZE) {
            value = ChiselsAndBits.getConfig().getClient().maxDrawnRegionSize.get();
        } else if (which == FloatParam.BLOCK_FULL_LIGHT_PERCENTAGE) {
            value = ChiselsAndBits.getConfig()
                    .getServer()
                    .bitLightPercentage
                    .get()
                    .floatValue();
        } else if (which == IntegerParam.BIT_BAG_MAX_STACK_SIZE) {
            value = ChiselsAndBits.getConfig().getServer().bagStackSize.get();
        } else {
            throw new IllegalArgumentException("Unsupported parameter: " + which);
        }

        return (T) value;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void renderModel(
            final PoseStack stack,
            final LegacyBakedModel model,
            final Level world,
            final BlockPos pos,
            final int alpha,
            final int combinedLight,
            final int combinedOverlay) {
        final int clampedAlpha = Math.max(0, Math.min(alpha, 255));
        RenderHelper.renderModel(stack, model, world, pos, clampedAlpha << 24, combinedLight, combinedOverlay);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void renderGhostModel(
            final PoseStack stack,
            final LegacyBakedModel model,
            final Level world,
            final BlockPos pos,
            final boolean isUnplaceable,
            final int combinedLight,
            final int combinedOverlay) {
        RenderHelper.renderGhostModel(stack, model, world, pos, isUnplaceable, combinedLight, combinedOverlay);
    }
}
