package mod.chiselsandbits.helpers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mod.chiselsandbits.bitbag.BagInventory;
import mod.chiselsandbits.chiseledblock.ItemBlockChiseled;
import mod.chiselsandbits.chiseledblock.NBTBlobConverter;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.IntegerBox;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.components.ChiseledData;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.StateLookup.CachedStateLookup;
import mod.chiselsandbits.items.ItemBitBag;
import mod.chiselsandbits.items.ItemChiseledBit;
import mod.chiselsandbits.registry.ModDataComponents;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.render.chiseledblock.ChiselRenderType;
import mod.chiselsandbits.render.helpers.SimpleInstanceCache;
import mod.chiselsandbits.utils.SingleBlockBlockReader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DirtPathBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModUtil {

    @NotNull
    public static final String NBT_SIDE = "side";

    @NotNull
    public static final String NBT_BLOCKENTITYTAG = "BlockEntityTag";

    private static final Random RAND = new Random();
    private static final float DEG_TO_RAD = 0.017453292f;

    private static final SimpleInstanceCache<ChiseledData, VoxelBlob> STACK_VOXEL_BLOB_SIMPLE_INSTANCE_CACHE =
            new SimpleInstanceCache<>(null, null);
    private static StateLookup IDRelay = new StateLookup();

    public static Direction getPlaceFace(final LivingEntity placer) {
        Direction[] orderedDirections = Direction.orderedByNearest(placer);
        return orderedDirections[0].getAxis() == Direction.Axis.Y
                ? orderedDirections[1].getOpposite()
                : orderedDirections[0].getOpposite();
    }

    public static Pair<Vec3, Vec3> getPlayerRay(final Player playerIn) {
        final double reachDistance = playerIn.blockInteractionRange();

        final double x = playerIn.xo + (playerIn.getX() - playerIn.xo);
        final double y = playerIn.yo + (playerIn.getY() - playerIn.yo) + playerIn.getEyeHeight();
        final double z = playerIn.zo + (playerIn.getZ() - playerIn.zo);

        final float playerPitch = playerIn.getXRot();
        final float playerYaw = playerIn.getYRot();

        final float yawRayX = Mth.sin(-playerYaw * DEG_TO_RAD - (float) Math.PI);
        final float yawRayZ = Mth.cos(-playerYaw * DEG_TO_RAD - (float) Math.PI);

        final float pitchMultiplier = -Mth.cos(-playerPitch * DEG_TO_RAD);
        final float eyeRayY = Mth.sin(-playerPitch * DEG_TO_RAD);
        final float eyeRayX = yawRayX * pitchMultiplier;
        final float eyeRayZ = yawRayZ * pitchMultiplier;

        final Vec3 from = new Vec3(x, y, z);
        final Vec3 to = from.add(eyeRayX * reachDistance, eyeRayY * reachDistance, eyeRayZ * reachDistance);

        return Pair.of(from, to);
    }

    public static IItemInInventory findBit(final ActingPlayer who, final BlockPos pos, final int StateID) {
        final ItemStack inHand = who.getCurrentEquippedItem();
        final Container inv = who.getInventory();
        final boolean canEdit = who.canPlayerManipulate(pos, Direction.UP, inHand, true);

        if (getStackSize(inHand) > 0
                && inHand.getItem() instanceof ItemChiseledBit
                && ItemChiseledBit.getStackState(inHand) == StateID) {
            return new ItemStackSlot(inv, who.getCurrentItem(), inHand, who, canEdit);
        }

        for (int x = 0; x < inv.getContainerSize(); x++) {
            final ItemStack is = inv.getItem(x);
            if (getStackSize(is) > 0
                    && is.getItem() instanceof ItemChiseledBit
                    && ItemChiseledBit.sameBit(is, StateID)) {
                return new ItemStackSlot(inv, x, is, who, canEdit);
            }
        }

        return new ItemStackSlot(inv, -1, ModUtil.getEmptyStack(), who, canEdit);
    }

    public static Set<Integer> getAllStates(VoxelBlob blob) {
        if (blob == null || blob.getBlockSums() == null) {
            return Set.of();
        }
        return blob.getBlockSums().keySet();
    }

    public static ChiselRenderType[] getRenderType(BlockState state) {
        final LinkedHashSet<ChiselRenderType> result = new LinkedHashSet<>();
        for (final ChunkSectionLayer layer : getBlockRenderLayers(state)) {
            result.add(ChiselRenderType.fromLayer(layer, false));
        }

        if (!state.getFluidState().isEmpty()) {
            result.add(ChiselRenderType.fromLayer(getFluidRenderLayer(state), true));
        }

        return result.toArray(ChiselRenderType[]::new);
    }

    public static Set<ChunkSectionLayer> getBlockRenderLayers(final BlockState state) {
        final LinkedHashSet<ChunkSectionLayer> result = new LinkedHashSet<>();
        if (state == null || state.isAir()) {
            return result;
        }

        final BlockStateModel model = Minecraft.getInstance()
                .getModelManager()
                .getBlockStateModelSet()
                .get(state);
        final List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42L), parts);

        for (final BlockStateModelPart part : parts) {
            collectLayers(result, part.getQuads(null));
            for (final Direction direction : Direction.values()) {
                collectLayers(result, part.getQuads(direction));
            }
        }

        if (result.isEmpty() && state.getFluidState().isEmpty()) {
            result.add(ChunkSectionLayer.SOLID);
        }

        return result;
    }

    private static void collectLayers(final Set<ChunkSectionLayer> target, final List<BakedQuad> quads) {
        for (final BakedQuad quad : quads) {
            target.add(quad.materialInfo().layer());
        }
    }

    public static ChunkSectionLayer getFluidRenderLayer(final BlockState state) {
        return Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(state.getFluidState())
                .layer();
    }

    public static ChunkSectionLayer get(final BlockState state) {
        if (!state.getFluidState().isEmpty() && ModUtil.isFluid(state)) {
            return getFluidRenderLayer(state);
        }

        return getBlockRenderLayers(state).stream().findFirst().orElse(ChunkSectionLayer.SOLID);
    }

    public static Set<ChunkSectionLayer> extractRenderTypes(VoxelBlob blob) {
        return getAllStates(blob).stream()
                .map(ModUtil::getStateById)
                .filter(state -> !state.isAir())
                .flatMap(state -> Stream.of(getRenderType(state)))
                .map(renderType -> renderType.layer)
                .collect(Collectors.toSet());
    }

    public static Set<ChiselRenderType> getAllRenderTypes(VoxelBlob blob) {
        return getAllStates(blob).stream()
                .flatMap(id -> Stream.of(getRenderType(ModUtil.getStateById(id))))
                .collect(Collectors.toSet());
    }

    public static @NotNull ItemStack copy(final ItemStack st) {
        if (st == null) {
            return ModUtil.getEmptyStack();
        }

        return nonNull(st.copy());
    }

    public static @NotNull ItemStack nonNull(final ItemStack st) {
        if (st == null) {
            return ModUtil.getEmptyStack();
        }

        return st;
    }

    public static boolean isHoldingPattern(final Player player, AtomicBoolean positivePattern) {
        final ItemStack inHand = player.getMainHandItem();

        if (inHand != null && inHand.getItem().equals(ModItems.ITEM_POSITIVE_PRINT_WRITTEN.get())) {
            positivePattern.set(true);
            return true;
        }

        if (inHand != null && inHand.getItem().equals(ModItems.ITEM_NEGATIVE_PRINT_WRITTEN.get())) {
            positivePattern.set(false);
            return true;
        }

        return false;
    }

    public static boolean isHoldingChiseledBlock(final Player player) {
        final ItemStack inHand = player.getMainHandItem();

        return inHand != null && inHand.getItem() instanceof ItemBlockChiseled;
    }

    public static int getRotationIndex(final Direction face) {
        return face.get2DDataValue();
    }

    public static int getRotations(final LivingEntity placer, final Direction oldYaw) {
        final Direction newFace = ModUtil.getPlaceFace(placer);
        int rotations = getRotationIndex(newFace) - getRotationIndex(oldYaw);

        // work out the rotation math...
        while (rotations < 0) {
            rotations = 4 + rotations;
        }
        while (rotations > 4) {
            rotations = rotations - 4;
        }

        return 4 - rotations;
    }

    public static BlockPos getPartialOffset(
            final Direction side, final BlockPos partial, final IntegerBox modelBounds) {
        int offset_x = modelBounds.minX;
        int offset_y = modelBounds.minY;
        int offset_z = modelBounds.minZ;

        final int partial_x = partial.getX();
        final int partial_y = partial.getY();
        final int partial_z = partial.getZ();

        int middle_x = (modelBounds.maxX - modelBounds.minX) / -2;
        int middle_y = (modelBounds.maxY - modelBounds.minY) / -2;
        int middle_z = (modelBounds.maxZ - modelBounds.minZ) / -2;

        switch (side) {
            case DOWN:
                offset_y = modelBounds.maxY;
                middle_y = 0;
                break;
            case EAST:
                offset_x = modelBounds.minX;
                middle_x = 0;
                break;
            case NORTH:
                offset_z = modelBounds.maxZ;
                middle_z = 0;
                break;
            case SOUTH:
                offset_z = modelBounds.minZ;
                middle_z = 0;
                break;
            case UP:
                offset_y = modelBounds.minY;
                middle_y = 0;
                break;
            case WEST:
                offset_x = modelBounds.maxX;
                middle_x = 0;
                break;
            default:
                throw new NullPointerException();
        }

        final int t_x = -offset_x + middle_x + partial_x;
        int t_y = -offset_y + middle_y + partial_y;
        final int t_z = -offset_z + middle_z + partial_z;

        return new BlockPos(t_x, t_y, t_z);
    }

    @SafeVarargs
    public static <T> T firstNonNull(final T... options) {
        for (final T i : options) {
            if (i != null) {
                return i;
            }
        }

        throw new NullPointerException("Unable to find a non null item.");
    }

    public static BlockEntity getTileEntitySafely(final @NotNull BlockGetter world, final @NotNull BlockPos pos) {

        // stupid...
        if (world instanceof Level) {
            return ((Level) world).getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
        }

        // yep... stupid.
        else {
            return world.getBlockEntity(pos);
        }
    }

    public static boolean isFluid(BlockState state) {
        return !state.getFluidState().isEmpty();
    }

    public static TileEntityBlockChiseled getChiseledTileEntity(
            @NotNull final BlockGetter world, @NotNull final BlockPos pos) {
        final BlockEntity te = getTileEntitySafely(world, pos);
        if (te instanceof TileEntityBlockChiseled) {
            return (TileEntityBlockChiseled) te;
        }

        return null;
    }

    public static TileEntityBlockChiseled getChiseledTileEntity(
            @NotNull final Level world, @NotNull final BlockPos pos, final boolean create) {
        if (world.hasChunkAt(pos)) {
            final BlockEntity te = world.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
            if (te instanceof TileEntityBlockChiseled) {
                return (TileEntityBlockChiseled) te;
            }

            return null;
        }
        return null;
    }

    public static void removeChiseledBlock(@NotNull final Level world, @NotNull final BlockPos pos) {
        final BlockEntity te = world.getBlockEntity(pos);
        final BlockState oldState = world.getBlockState(pos);

        if (te instanceof TileEntityBlockChiseled) {
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); // no physical matter left...
            return;
        }

        world.setBlocksDirty(pos, oldState, Blocks.AIR.defaultBlockState());
    }

    public static boolean containsAtLeastOneOf(final Container inv, final ItemStack is) {
        boolean seen = false;
        for (int x = 0; x < inv.getContainerSize(); x++) {
            final ItemStack which = inv.getItem(x);

            if (which != null
                    && which.getItem() == is.getItem()
                    && ItemChiseledBit.sameBit(which, ItemChiseledBit.getStackState(is))) {
                if (!seen) {
                    seen = true;
                }
            }
        }
        return seen;
    }

    public static List<BagInventory> getBags(final ActingPlayer player) {
        if (player.isCreative()) {
            return java.util.Collections.emptyList();
        }

        final List<BagInventory> bags = new ArrayList<BagInventory>();
        final Container inv = player.getInventory();

        for (int zz = 0; zz < inv.getContainerSize(); zz++) {
            final ItemStack which = inv.getItem(zz);
            if (which != null && which.getItem() instanceof ItemBitBag) {
                bags.add(new BagInventory(which));
            }
        }

        return bags;
    }

    public static int consumeBagBit(final List<BagInventory> bags, final int inPattern, final int howMany) {
        int remaining = howMany;
        for (final BagInventory inv : bags) {
            remaining -= inv.extractBit(inPattern, remaining);
            if (remaining == 0) {
                return howMany;
            }
        }

        return howMany - remaining;
    }

    public static VoxelBlob getBlobFromStack(final ItemStack stack, final LivingEntity rotationPlayer) {
        final ChiseledData data = NBTBlobConverter.getComponent(stack);
        if (data != null) {
            VoxelBlob blob;
            if (STACK_VOXEL_BLOB_SIMPLE_INSTANCE_CACHE.needsUpdate(data)) {
                final NBTBlobConverter tmp = new NBTBlobConverter();
                tmp.readChisleData(data, VoxelBlob.VERSION_ANY);
                blob = tmp.getBlob();
                STACK_VOXEL_BLOB_SIMPLE_INSTANCE_CACHE.updateCachedValue(new VoxelBlob(blob));
            } else {
                blob = new VoxelBlob(STACK_VOXEL_BLOB_SIMPLE_INSTANCE_CACHE.getCached());
            }

            if (rotationPlayer != null) {
                int xrotations = ModUtil.getRotations(rotationPlayer, ModUtil.getSide(stack));
                while (xrotations-- > 0) {
                    blob = blob.spin(Direction.Axis.Y);
                }
            }

            return blob;
        }

        return new VoxelBlob();
    }

    public static void sendUpdate(@Nullable final Level worldObj, @NotNull final BlockPos pos) {
        if (worldObj == null) {
            return;
        }

        final BlockState state = worldObj.getBlockState(pos);
        worldObj.sendBlockUpdated(pos, state, state, 0);
    }

    private static Item getItem(@NotNull final BlockState blockState) {
        final Block block = blockState.getBlock();
        if (block.equals(Blocks.LAVA)) {
            return Items.LAVA_BUCKET;
        } else if (block instanceof CropBlock) {
            final Item item = block.asItem();
            if (item != Items.AIR) {
                return item;
            }

            return Items.WHEAT_SEEDS;
        }
        // oh no...
        else if (block instanceof FarmlandBlock || block instanceof DirtPathBlock) {
            return getItemFromBlock(Blocks.DIRT);
        } else if (block instanceof FireBlock) {
            return Items.FLINT_AND_STEEL;
        } else if (block instanceof FlowerPotBlock) {
            return Items.FLOWER_POT;
        } else if (block == Blocks.BAMBOO_SAPLING) {
            return Items.BAMBOO;
        } else {
            return getItemFromBlock(block);
        }
    }

    private static Item getItemFromBlock(final Block block) {
        return Item.byBlock(block);
    }

    /**
     * Mimics pick block.
     *
     * @param blockState the block and state we are creating an ItemStack for.
     * @return ItemStack fromt the BlockState.
     */
    public static ItemStack getItemStackFromBlockState(@NotNull final BlockState blockState) {
        final Fluid fluid = blockState.getFluidState().getType();
        if (fluid != Fluids.EMPTY && fluid.getBucket() != Items.AIR) {
            return new ItemStack(fluid.getBucket());
        }
        final Item item = getItem(blockState);
        if (item != Items.AIR && item != null) {
            if (item instanceof BlockItem blockItem) {
                return new ItemStack(blockItem.getBlock().asItem());
            }
            return new ItemStack(item, 1);
        }

        return new ItemStack(blockState.getBlock(), 1);
    }

    public static boolean support(int primaryStateId, ChunkSectionLayer renderType) {
        final BlockState blockState = ModUtil.getStateById(primaryStateId);
        return getBlockRenderLayers(blockState).contains(renderType);
    }

    @Nullable
    public static VoxelBlob rotate(final VoxelBlob blob, final Direction.Axis axis, final Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90:
                return blob.spin(axis).spin(axis).spin(axis);
            case CLOCKWISE_180:
                return blob.spin(axis).spin(axis);
            case COUNTERCLOCKWISE_90:
                return blob.spin(axis);
            case NONE:
            default:
                break;
        }
        return null;
    }

    public static boolean isNormalCube(final BlockState blockType, final BlockGetter reader, final BlockPos pos) {
        return blockType.isRedstoneConductor(reader, pos);
    }

    public static boolean isNormalCube(final BlockState blockState) {
        return isNormalCube(blockState, new SingleBlockBlockReader(blockState, blockState.getBlock()), BlockPos.ZERO);
    }

    public static Direction getSide(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Direction.NORTH;
        }

        Direction side = stack.get(ModDataComponents.PLACEMENT_SIDE);
        if (side == null) {
            NBTBlobConverter.getComponent(stack);
            side = stack.get(ModDataComponents.PLACEMENT_SIDE);
        }

        return side == null || side.getAxis() == Direction.Axis.Y ? Direction.NORTH : side;
    }

    public static void setSide(final ItemStack stack, final Direction side) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        stack.set(
                ModDataComponents.PLACEMENT_SIDE,
                side == null || side.getAxis() == Direction.Axis.Y ? Direction.NORTH : side);

        final CompoundTag legacy = getTagCompound(stack);
        legacy.remove(NBT_SIDE);
        if (legacy.contains(NBT_BLOCKENTITYTAG)) {
            final CompoundTag nested = legacy.getCompoundOrEmpty(NBT_BLOCKENTITYTAG);
            nested.remove(NBT_SIDE);
            if (nested.isEmpty()) {
                legacy.remove(NBT_BLOCKENTITYTAG);
            }
        }
        setTagCompound(stack, legacy);
    }

    public static boolean hasChiseledData(final ItemStack stack) {
        return NBTBlobConverter.hasChiseledData(stack);
    }

    public static BlockState getStateById(final int blockStateID) {
        return IDRelay.getStateById(blockStateID);
    }

    public static int getStateId(final BlockState state) {
        return Math.max(0, IDRelay.getStateId(state));
    }

    public static void cacheFastStates() {
        if (!ChiselsAndBits.getConfig().getServer().lowMemoryMode.get()) {
            // cache id -> state table as an array for faster rendering lookups.
            IDRelay = new CachedStateLookup();
        }
    }

    public static int getStackSize(final ItemStack stack) {
        return stack == null ? 0 : stack.getCount();
    }

    public static void setStackSize(final @NotNull ItemStack stack, final int stackSize) {
        stack.setCount(stackSize);
    }

    public static void adjustStackSize(final @NotNull ItemStack is, final int sizeDelta) {
        setStackSize(is, getStackSize(is) + sizeDelta);
    }

    public static CompoundTag getSubCompound(final ItemStack stack, final String tag, final boolean create) {
        final CompoundTag root = getTagCompound(stack);
        if (!root.contains(tag) && create) {
            root.put(tag, new CompoundTag());
            setTagCompound(stack, root);
        }

        return root.getCompoundOrEmpty(tag);
    }

    public static @NotNull ItemStack getEmptyStack() {
        return ItemStack.EMPTY;
    }

    public static boolean notEmpty(final ItemStack itemStack) {
        return itemStack != null && !itemStack.isEmpty();
    }

    public static boolean isEmpty(final ItemStack itemStack) {
        return itemStack == null || itemStack.isEmpty();
    }

    public static @NotNull CompoundTag getTagCompound(final ItemStack ei) {
        final CustomData customData = ei.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }

    public static boolean hasTag(final ItemStack stack) {
        final CustomData customData = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && !customData.isEmpty();
    }

    public static void setTagCompound(final ItemStack stack, @Nullable final CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        }
    }

    @SuppressWarnings("deprecation")
    public static BlockState getStateFromItem(final ItemStack is) {
        try {
            if (!ModUtil.isEmpty(is) && is.getItem() instanceof BlockItem iblk) {
                final BlockState state = iblk.getBlock().defaultBlockState();

                return state;
            }
        } catch (final Throwable t) {
            // : (
        }

        return Blocks.AIR.defaultBlockState();
    }

    public static void damageItem(@NotNull final ItemStack is, @NotNull final RandomSource r) {
        if (is.isDamageableItem()) {
            is.setDamageValue(is.getDamageValue() + 1);
            if (is.isBroken()) {
                is.shrink(1);
                is.setDamageValue(0);
            }
        }
    }

    @NotNull
    public static ItemStack makeStack(final Item item) {
        return makeStack(item, 1);
    }

    @NotNull
    public static ItemStack makeStack(final Item item, final int stackSize) {
        return new ItemStack(item, stackSize);
    }

    public static boolean isEmpty(final Item item) {
        return item == Items.AIR;
    }
}
