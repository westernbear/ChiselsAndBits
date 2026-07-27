package mod.chiselsandbits.chiseledblock;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import mod.chiselsandbits.api.BoxType;
import mod.chiselsandbits.api.ChiselsAndBitsEvents;
import mod.chiselsandbits.api.EventBlockBitPostModification;
import mod.chiselsandbits.api.EventFullBlockRestoration;
import mod.chiselsandbits.api.IBitAccess;
import mod.chiselsandbits.api.IChiseledBlockTileEntity;
import mod.chiselsandbits.api.ItemType;
import mod.chiselsandbits.api.VoxelStats;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.data.VoxelBlobStateReference;
import mod.chiselsandbits.client.UndoTracker;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.client.model.data.IModelData;
import mod.chiselsandbits.client.model.data.ModelDataMap;
import mod.chiselsandbits.client.model.data.ModelProperty;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.Log;
import mod.chiselsandbits.core.api.BitAccess;
import mod.chiselsandbits.helpers.DeprecationHelper;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModTileEntityTypes;
import mod.chiselsandbits.render.chiseledblock.ChiselRenderType;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockSmartModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TileEntityBlockChiseled extends BlockEntity
        implements IChiseledBlockTileEntity, LegacyBlockEntityProperties {
    public static final ModelProperty<VoxelBlobStateReference> MP_VBSR = new ModelProperty<>();
    public static final ModelProperty<Integer> MP_PBSI = new ModelProperty<>();
    public static final ModelProperty<Map<ChiselRenderType, LegacyBakedModel>> MODEL_PROP = new ModelProperty<>();
    public static final ModelProperty<Boolean> MODEL_UPDATE = new ModelProperty<>();
    private static final ThreadLocal<Integer> LOCAL_LIGHT_LEVEL = new ThreadLocal<>();
    boolean isNormalCube = false;
    int sideState = 0;
    int lightLevel = -1;
    private boolean renderUpdate = false;
    private BlockState state;
    private VoxelBlobStateReference blobStateReference;
    private int primaryBlockStateId;
    private IModelData modelData = newModelData();
    /**
     * prevent mods that constantly ask for pick block from killing the client... ( looking at you waila )
     **/
    private ItemStackGeneratedCache pickCache = null;

    public TileEntityBlockChiseled(BlockPos pos, BlockState state) {
        this(ModTileEntityTypes.CHISELED.get(), pos, state);
    }

    public TileEntityBlockChiseled(final BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn == null ? ModTileEntityTypes.CHISELED.get() : tileEntityTypeIn, pos, state);
    }

    @Override
    public void setLevel(final Level level) {
        super.setLevel(level);
        if (!level.isClientSide() && getBlockState().getBlock() instanceof BlockChiseled) {
            final var server = level.getServer();
            if (server != null) {
                server.schedule(server.wrapRunnable(() -> {
                    if (level.isLoaded(worldPosition) && level.getBlockEntity(worldPosition) == this) {
                        synchronizeBlockStateProperties();
                    }
                }));
            }
        }
    }

    public static long getPositionRandom(final BlockPos pos) {
        if (pos != null && FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return Mth.getSeed(pos);
        }

        return 0;
    }

    public static void setLightFromBlock(final BlockState defaultState) {
        if (defaultState == null) {
            LOCAL_LIGHT_LEVEL.remove();
        } else {
            LOCAL_LIGHT_LEVEL.set(DeprecationHelper.getLightValue(defaultState));
        }
    }

    public VoxelBlobStateReference getBlobStateReference() {
        return blobStateReference;
    }

    private void setBlobStateReference(final VoxelBlobStateReference blobStateReference) {
        if (this.blobStateReference == null || !this.blobStateReference.equals(blobStateReference)) {
            this.blobStateReference = blobStateReference;
            modelData.setData(MP_VBSR, blobStateReference);
            modelData.setData(MODEL_UPDATE, true);
        }
    }

    public int getPrimaryBlockStateId() {
        return primaryBlockStateId;
    }

    public void setPrimaryBlockStateId(final int primaryBlockStateId) {
        this.primaryBlockStateId = primaryBlockStateId;
        modelData.setData(MP_PBSI, primaryBlockStateId);
        setLightFromBlock(ModUtil.getStateById(primaryBlockStateId));
    }

    @NotNull
    protected BlockState getState() {
        if (state == null) {
            state = ModBlocks.getChiseledDefaultState();
        }

        return Objects.requireNonNull(state);
    }

    public BlockState getBlockState(final Block alternative) {
        final int stateID = getPrimaryBlockStateId();

        final BlockState state = ModUtil.getStateById(stateID);
        if (state != null) {
            return state;
        }

        return alternative.defaultBlockState();
    }

    public void setState(final BlockState blockState, final VoxelBlobStateReference newRef) {
        final VoxelBlobStateReference originalRef = getBlobStateReference();

        this.state = blockState;

        if (newRef != null && !newRef.equals(originalRef)) {
            final EventBlockBitPostModification bmm =
                    new EventBlockBitPostModification(Objects.requireNonNull(getLevel()), getBlockPos());
            ChiselsAndBitsEvents.BLOCK_BIT_POST_MODIFICATION.invoker().handle(bmm);
            setBlobStateReference(newRef);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        final CompoundTag compound = new CompoundTag();
        writeChiselData(compound);

        if (compound.size() == 0) {
            return null;
        }

        return ClientboundBlockEntityDataPacket.create(this, (blockEntity, registryAccess) -> compound);
    }

    @NotNull
    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag compound = new CompoundTag();

        compound.putInt("x", worldPosition.getX());
        compound.putInt("y", worldPosition.getY());
        compound.putInt("z", worldPosition.getZ());

        writeChiselData(compound);

        return compound;
    }

    public boolean readChiselData(final CompoundTag tag) {
        final NBTBlobConverter converter = new NBTBlobConverter(false, this);
        return converter.readChisleData(tag, VoxelBlob.VERSION_COMPACT_PALLETED);
    }

    public void writeChiselData(final CompoundTag tag) {
        new NBTBlobConverter(false, this).writeChisleData(tag, false);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        new NBTBlobConverter(false, this).writeChisleData(output, false);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        final VoxelBlobStateReference current = getBlobStateReference();
        final int oldLight = lightLevel;
        final boolean changed =
                new NBTBlobConverter(false, this).readChisleData(input, VoxelBlob.VERSION_COMPACT_PALLETED);

        if (level != null && changed) {
            level.setBlocksDirty(worldPosition, level.getBlockState(worldPosition), Blocks.AIR.defaultBlockState());

            // fixes lighting on placement when tile packet arrives.
            if (oldLight != lightLevel) {
                level.getLightEngine().checkBlock(worldPosition);
            }
        }

        if (level != null) {
            if (level.isClientSide()) {
                UndoTracker.getInstance().onNetworkUpdate(current, getBlobStateReference());
            }
        }
    }

    @NotNull
    @Override
    public CompoundTag writeTileEntityToTag(@NotNull final CompoundTag tag, final boolean crossWorld) {
        new NBTBlobConverter(false, this).writeChisleData(tag, crossWorld);
        tag.putBoolean("cw", crossWorld);
        return tag;
    }

    public void mirror(@NotNull final Mirror mirrorIn) {
        switch (mirrorIn) {
            case FRONT_BACK:
                setBlob(getBlob().mirror(Axis.X), true);
                break;
            case LEFT_RIGHT:
                setBlob(getBlob().mirror(Axis.Z), true);
                break;
            case NONE:
            default:
                break;
        }
    }

    @Override
    public BlockState mirror(LevelAccessor level, BlockPos pos, BlockState blockState, Mirror mirrorIn) {
        switch (mirrorIn) {
            case FRONT_BACK:
                setBlob(getBlob().mirror(Axis.X), true);
                break;
            case LEFT_RIGHT:
                setBlob(getBlob().mirror(Axis.Z), true);
                break;
            case NONE:
            default:
                break;
        }

        return blockState;
    }

    public void rotate(@NotNull final Direction axis, @NotNull final Rotation rotationIn) {
        VoxelBlob blob = ModUtil.rotate(getBlob(), axis.getAxis(), rotationIn);
        if (blob != null) {
            setBlob(blob, true);
        }
    }

    public void rotate(@NotNull final Rotation rotationIn) {
        VoxelBlob blob = ModUtil.rotate(getBlob(), Axis.Y, rotationIn);
        if (blob != null) {
            setBlob(blob, true);
        }
    }

    public void fillWith(final BlockState blockType) {
        final int ref = ModUtil.getStateId(blockType);

        sideState = 0xff;
        lightLevel = DeprecationHelper.getLightValue(blockType);
        isNormalCube = ModUtil.isNormalCube(blockType);

        BlockState defaultState = getState();

        // required for placing bits
        if (ref != 0) {
            setPrimaryBlockStateId(ref);
        }

        setState(
                defaultState,
                new VoxelBlobStateReference(ModUtil.getStateId(blockType), getPositionRandom(worldPosition)));

        setChanged();
        synchronizeBlockStateProperties();
    }

    @Override
    public @Nullable Object getRenderData() {
        modelData.setData(MP_VBSR, getBlobStateReference());
        modelData.setData(MP_PBSI, getPrimaryBlockStateId());
        return modelData;
    }

    public VoxelBlob getBlob() {
        VoxelBlob vb;
        final VoxelBlobStateReference vbs = getBlobStateReference();

        if (vbs != null) {
            vb = vbs.getVoxelBlob();
        } else {
            vb = new VoxelBlob();
        }

        return vb;
    }

    public void setBlob(final VoxelBlob vb) {
        setBlob(vb, true);
    }

    public boolean updateBlob(final NBTBlobConverter converter, final boolean triggerUpdates) {
        final int oldLV = getLightValue();
        final boolean oldNC = isNormalCube();
        final int oldSides = sideState;

        final VoxelBlobStateReference originalRef = getBlobStateReference();

        VoxelBlobStateReference voxelRef;

        sideState = converter.getSideState();
        final int b = converter.getPrimaryBlockStateID();
        lightLevel = converter.getLightValue();
        isNormalCube = converter.isNormalCube();

        try {
            voxelRef = converter.getVoxelRef(VoxelBlob.VERSION_COMPACT_PALLETED, getPositionRandom(worldPosition));
        } catch (final Exception e) {
            Log.logError("Unable to read blob at " + getBlockPos(), e);
            voxelRef = new VoxelBlobStateReference(0, getPositionRandom(worldPosition));
        }

        setPrimaryBlockStateId(b);
        setBlobStateReference(voxelRef);
        setState(getState(), voxelRef);

        if (getLevel() != null) {
            if (oldLV != getLightValue() || oldNC != isNormalCube()) {
                synchronizeBlockStateProperties();
            }

            if (triggerUpdates && oldSides != sideState) {
                Objects.requireNonNull(level)
                        .updateNeighborsAt(
                                worldPosition,
                                level.getBlockState(worldPosition).getBlock());
            }
        }

        return voxelRef == null || !voxelRef.equals(originalRef);
    }

    public void setBlob(final VoxelBlob vb, final boolean triggerUpdates) {
        final int olv = getLightValue();
        final boolean oldNC = isNormalCube();

        final VoxelStats common = vb.getVoxelStats();
        final float light = common.blockLight;
        final boolean nc = common.isNormalBlock;
        final int lv = Math.max(0, Math.min(15, (int) (light * 15)));

        // are most of the bits in the center solid?
        final int sideFlags = vb.getSideFlags(5, 11, 4 * 4);

        if (getLevel() == null) {
            if (common.mostCommonState == 0) {
                common.mostCommonState = getPrimaryBlockStateId();
            }

            sideState = sideFlags;
            lightLevel = lv;
            isNormalCube = nc;

            setBlobStateReference(new VoxelBlobStateReference(
                    vb.blobToBytes(VoxelBlob.VERSION_COMPACT_PALLETED), getPositionRandom(worldPosition)));
            setPrimaryBlockStateId(common.mostCommonState);
            setState(getState(), getBlobStateReference());
            return;
        }

        if (common.isFullBlock) {
            setBlobStateReference(
                    new VoxelBlobStateReference(common.mostCommonState, getPositionRandom(worldPosition)));
            setState(getState(), getBlobStateReference());

            final BlockState newState = ModUtil.getStateById(common.mostCommonState);
            if (ChiselsAndBits.getConfig().getServer().canRevertToBlock(newState)) {
                EventFullBlockRestoration evt =
                        new EventFullBlockRestoration(Objects.requireNonNull(level), worldPosition, newState);
                ChiselsAndBitsEvents.FULL_BLOCK_RESTORATION.invoker().handle(evt);
                if (!evt.isCancelled()) {
                    level.setBlock(worldPosition, newState, triggerUpdates ? 3 : 0);
                }
            }
        } else if (common.mostCommonState != 0) {
            sideState = sideFlags;
            lightLevel = lv;
            isNormalCube = nc;

            setBlobStateReference(new VoxelBlobStateReference(
                    vb.blobToBytes(VoxelBlob.VERSION_COMPACT_PALLETED), getPositionRandom(worldPosition)));
            setPrimaryBlockStateId(common.mostCommonState);
            setState(getState(), getBlobStateReference());

            setChanged();
            ModUtil.sendUpdate(Objects.requireNonNull(getLevel()), worldPosition);

            // since its possible for bits to occlude parts.. update every time.
            final Block blk =
                    Objects.requireNonNull(level).getBlockState(worldPosition).getBlock();
            // worldObj.notifyBlockOfStateChange( pos, blk, false );

            if (triggerUpdates) {
                level.updateNeighborsAt(worldPosition, blk);
            }
        } else {
            setState(getState(), new VoxelBlobStateReference(0, getPositionRandom(worldPosition)));
            setPrimaryBlockStateId(0);
            ModUtil.removeChiseledBlock(Objects.requireNonNull(level), worldPosition);
        }

        if (olv != lv || oldNC != nc) {
            synchronizeBlockStateProperties();
        }
    }

    public void synchronizeBlockStateProperties() {
        final Level currentLevel = getLevel();
        if (currentLevel == null) {
            return;
        }

        if (currentLevel.isClientSide()) {
            currentLevel.getLightEngine().checkBlock(worldPosition);
            return;
        }

        final BlockState currentState = currentLevel.getBlockState(worldPosition);
        if (!(currentState.getBlock() instanceof BlockChiseled chiseledBlock)
                || currentLevel.getBlockEntity(worldPosition) != this) {
            return;
        }

        final int effectiveLight =
                Mth.clamp(chiseledBlock.getLightEmission(currentState, currentLevel, worldPosition), 0, 15);
        final BlockState synchronizedState = currentState
                .setValue(BlockChiseled.FULL_BLOCK, isNormalCube())
                .setValue(BlockChiseled.LIGHT_LEVEL, effectiveLight);

        final boolean fullBlockChanged = currentState.getValue(BlockChiseled.FULL_BLOCK) != isNormalCube();
        final boolean lightChanged = currentState.getValue(BlockChiseled.LIGHT_LEVEL) != effectiveLight;

        if (fullBlockChanged || lightChanged) {
            currentLevel.setBlock(
                    worldPosition, synchronizedState, fullBlockChanged ? Block.UPDATE_ALL : Block.UPDATE_CLIENTS);
        }

        if (lightChanged) {
            currentLevel.getLightEngine().checkBlock(worldPosition);
        }
    }

    public boolean shouldRenderUpdate() {
        return renderUpdate;
    }

    public void setShouldRenderUpdate(boolean renderUpdate) {
        this.renderUpdate = renderUpdate;
    }

    public ItemStack getItemStack(final Player player) {
        final ItemStackGeneratedCache cache = pickCache;

        if (player != null) {
            Direction placingFace = ModUtil.getPlaceFace(player);
            final int rotations = ModUtil.getRotationIndex(placingFace);

            if (cache != null
                    && cache.rotations == rotations
                    && cache.ref == getBlobStateReference()
                    && cache.out != null) {
                return cache.getItemStack();
            }

            VoxelBlob vb = getBlob();

            int countDown = rotations;
            while (countDown > 0) {
                countDown--;
                placingFace = placingFace.getCounterClockWise();
                vb = vb.spin(Axis.Y);
            }

            final BitAccess ba = new BitAccess(null, null, vb, VoxelBlob.NULL_BLOB);
            final ItemStack itemstack = ba.getBitsAsItem(placingFace, ItemType.CHISELED_BLOCK, false);

            pickCache = new ItemStackGeneratedCache(itemstack, getBlobStateReference(), rotations);
            return itemstack;
        } else {
            if (cache != null && cache.rotations == 0 && cache.ref == getBlobStateReference()) {
                return cache.getItemStack();
            }

            final BitAccess ba = new BitAccess(null, null, getBlob(), VoxelBlob.NULL_BLOB);
            final ItemStack itemstack = ba.getBitsAsItem(null, ItemType.CHISELED_BLOCK, false);

            pickCache = new ItemStackGeneratedCache(itemstack, getBlobStateReference(), 0);
            return itemstack;
        }
    }

    public boolean isNormalCube() {
        return isNormalCube;
    }

    public void setNormalCube(final boolean b) {
        isNormalCube = b;
    }

    public boolean isSideSolid(final Direction side) {
        return (sideState & 1 << side.ordinal()) != 0;
    }

    public boolean isSideOpaque(final Direction side) {
        if (this.getLevel() != null && this.getLevel().isClientSide()) {
            return isInnerSideOpaque(side);
        }

        return false;
    }

    @Environment(EnvType.CLIENT)
    public boolean isInnerSideOpaque(final Direction side) {
        final int sideFlags = ChiseledBlockSmartModel.getSides(this);
        return (sideFlags & 1 << side.ordinal()) != 0;
    }

    public void completeEditOperation(final VoxelBlob vb) {
        final VoxelBlobStateReference before = getBlobStateReference();
        setBlob(vb);
        final VoxelBlobStateReference after = getBlobStateReference();
        if (level != null) {
            level.setBlocksDirty(worldPosition, level.getBlockState(worldPosition), Blocks.AIR.defaultBlockState());
        }

        UndoTracker.getInstance().add(getLevel(), getBlockPos(), before, after);
    }

    // TODO: Figure this out.
    public void rotateBlock() {
        final VoxelBlob occluded = new VoxelBlob();

        VoxelBlob postRotation = getBlob();
        int maxRotations = 4;
        while (--maxRotations > 0) {
            postRotation = postRotation.spin(Axis.Y);

            if (occluded.canMerge(postRotation)) {
                setBlob(postRotation);
                return;
            }
        }
    }

    public boolean canMerge(final VoxelBlob voxelBlob) {
        return getBlob().canMerge(voxelBlob);
    }

    //    @Override
    //    public AABB getRenderBoundingBox()
    //    {
    //        final BlockPos p = getBlockPos();
    //        return new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1, p.getZ() + 1);
    //    }

    @NotNull
    @Override
    public Collection<AABB> getBoxes(@NotNull final BoxType type) {
        final VoxelBlobStateReference ref = getBlobStateReference();

        if (ref != null) {
            return ref.getBoxes(type);
        } else {
            return Collections.emptyList();
        }
    }

    public int getLightValue() {
        // first time requested, pull from local, or default to 0
        if (lightLevel < 0) {
            final Integer tmp = LOCAL_LIGHT_LEVEL.get();
            lightLevel = tmp == null ? 0 : tmp;
        }

        return lightLevel;
    }

    @NotNull
    @Override
    public IBitAccess getBitAccess() {
        VoxelBlob mask = VoxelBlob.NULL_BLOB;

        if (level != null) {
            mask = new VoxelBlob();
        }

        return new BitAccess(level, worldPosition, getBlob(), mask);
    }

    public IModelData newModelData() {
        final IModelData data = new ModelDataMap();
        data.setData(MP_PBSI, getPrimaryBlockStateId());
        data.setData(MP_VBSR, getBlobStateReference());
        data.setData(MODEL_UPDATE, true);
        return data;
    }

    private static class ItemStackGeneratedCache {
        final ItemStack out;
        final VoxelBlobStateReference ref;
        final int rotations;

        public ItemStackGeneratedCache(
                final ItemStack itemstack, final VoxelBlobStateReference blobStateReference, final int rotations2) {
            out = itemstack == null ? null : itemstack.copy();
            ref = blobStateReference;
            rotations = rotations2;
        }

        public ItemStack getItemStack() {
            return out == null ? null : out.copy();
        }
    }
}
