package mod.chiselsandbits.bitstorage;

import java.util.List;
import mod.chiselsandbits.core.Log;
import mod.chiselsandbits.helpers.ExceptionNoTileEntity;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.utils.FluidUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class BlockBitStorage extends Block implements EntityBlock {

    private static final Property<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public BlockBitStorage(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new TileEntityBitStorage(blockPos, blockState);
    }

    public TileEntityBitStorage getTileEntity(final BlockEntity te) throws ExceptionNoTileEntity {
        if (te instanceof TileEntityBitStorage) {
            return (TileEntityBitStorage) te;
        }
        throw new ExceptionNoTileEntity();
    }

    public TileEntityBitStorage getTileEntity(final BlockGetter world, final BlockPos pos)
            throws ExceptionNoTileEntity {
        return getTileEntity(world.getBlockEntity(pos));
    }

    @Override
    public InteractionResult use(
            final BlockState state,
            final Level worldIn,
            final BlockPos pos,
            final Player player,
            final InteractionHand handIn,
            final BlockHitResult hit) {
        try {
            final TileEntityBitStorage tank = getTileEntity(worldIn, pos);
            final ItemStack current = ModUtil.nonNull(player.inventory.getSelected());

            if (!ModUtil.isEmpty(current)) {
                if (FluidUtil.interactWithFluidHandler(player, handIn, tank.getFluidStorage(null))) {
                    return InteractionResult.SUCCESS;
                }

                if (tank.addHeldBits(current, player)) {
                    return InteractionResult.SUCCESS;
                }
            } else {
                if (tank.addAllPossibleBits(player)) {
                    return InteractionResult.SUCCESS;
                }
            }

            if (tank.extractBits(player, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, pos)) {
                return InteractionResult.SUCCESS;
            }
        } catch (final ExceptionNoTileEntity e) {
            Log.noTileError(e);
        }

        return InteractionResult.FAIL;
    }

    @Environment(EnvType.CLIENT)
    public float getShadeBrightness(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 1.0F;
    }

    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public List<ItemStack> getDrops(BlockState blockState, LootParams.Builder builder) {
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) == null) {
            return List.of();
        }

        return List.of(
                getTankDrop((TileEntityBitStorage) builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)));
    }

    public ItemStack getTankDrop(final TileEntityBitStorage bitTank) {
        final ItemStack tankStack = new ItemStack(this);
        ItemBlockBitStorage.setFluid(tankStack, bitTank.getFluidVariant(), bitTank.getFluidAmount());
        return tankStack;
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof TileEntityBitStorage bitTank
                ? getTankDrop(bitTank)
                : super.getCloneItemStack(level, pos, state);
    }
}
