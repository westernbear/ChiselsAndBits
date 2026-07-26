package mod.chiselsandbits.chiseledblock;

import com.mojang.serialization.Codec;
import java.io.IOException;
import java.nio.ByteBuffer;
import mod.chiselsandbits.api.VoxelStats;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.data.VoxelBlobStateReference;
import mod.chiselsandbits.chiseledblock.serialization.StringStates;
import mod.chiselsandbits.components.ChiseledData;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModDataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class NBTBlobConverter {

    public static final String NBT_SIDE_FLAGS = "s";
    public static final String NBT_NORMALCUBE_FLAG = "nc";
    public static final String NBT_LIGHTVALUE = "lv";

    public static final String NBT_PRIMARY_STATE = "b";
    public static final String NBT_LEGACY_VOXEL = "v";
    public static final String NBT_VERSIONED_VOXEL = "X";
    private final boolean triggerUpdates;
    TileEntityBlockChiseled tile;
    private int sideState;
    private int lightValue;
    private boolean isNormalCube;
    private int primaryBlockState;
    private VoxelBlobStateReference voxelBlobRef;
    private int format = -1;

    public NBTBlobConverter() {
        triggerUpdates = false;
    }

    public NBTBlobConverter(final boolean triggerBlockUpdates, final TileEntityBlockChiseled tile) {
        this.tile = tile;

        triggerUpdates = triggerBlockUpdates;
        sideState = tile.sideState;
        lightValue = tile.getLightValue();
        isNormalCube = tile.isNormalCube;
        primaryBlockState = ModUtil.getStateId(tile.getBlockState(Blocks.COBBLESTONE));
        voxelBlobRef = tile.getBlobStateReference();
        format = voxelBlobRef == null ? -1 : voxelBlobRef.getFormat();
    }

    public int getSideState() {
        return sideState;
    }

    public int getLightValue() {
        return lightValue;
    }

    public boolean isNormalCube() {
        return isNormalCube;
    }

    public int getPrimaryBlockStateID() {
        return primaryBlockState;
    }

    public BlockState getPrimaryBlockState() {
        return ModUtil.getStateById(primaryBlockState);
    }

    public VoxelBlobStateReference getVoxelRef(final int version, final long weight) throws Exception {
        final VoxelBlobStateReference voxelRef = getRef();

        if (format == version) {
            return new VoxelBlobStateReference(voxelRef.getByteArray(), weight);
        }

        return new VoxelBlobStateReference(voxelRef.getVoxelBlobCatchable().blobToBytes(version), weight);
    }

    public void fillWith(final BlockState state) {
        voxelBlobRef = new VoxelBlobStateReference(ModUtil.getStateId(state), 0);
        updateFromBlob();
    }

    public final void writeChisleData(final CompoundTag compound, final boolean crossWorld) {
        final VoxelBlobStateReference voxelRef = getRef();

        if (primaryBlockState == 0) {
            return;
        }

        final int newFormat = crossWorld ? VoxelBlob.VERSION_CROSSWORLD : VoxelBlob.VERSION_COMPACT_PALLETED;
        final byte[] voxelBytes = newFormat == format
                ? voxelRef.getByteArray()
                : voxelRef.getVoxelBlob().blobToBytes(newFormat);

        compound.putInt(NBT_LIGHTVALUE, lightValue);

        if (crossWorld) {
            compound.putString(NBT_PRIMARY_STATE, StringStates.getNameFromStateID(primaryBlockState));
        } else {
            compound.putInt(NBT_PRIMARY_STATE, primaryBlockState);
        }

        compound.putInt(NBT_SIDE_FLAGS, sideState);
        compound.putBoolean(NBT_NORMALCUBE_FLAG, isNormalCube);
        compound.putByteArray(NBT_VERSIONED_VOXEL, voxelBytes);
    }

    /**
     * Produces the typed item component used by Minecraft 26.2. The primary state is always stored by its stable
     * registry/property name; the compact/cross-world choice only affects the voxel payload format.
     */
    @Nullable
    public final ChiseledData toComponent(final boolean crossWorld) {
        final VoxelBlobStateReference voxelRef = getRef();

        if (primaryBlockState == 0) {
            return null;
        }

        final int newFormat = crossWorld ? VoxelBlob.VERSION_CROSSWORLD : VoxelBlob.VERSION_COMPACT_PALLETED;
        final byte[] voxelBytes = newFormat == format
                ? voxelRef.getByteArray()
                : voxelRef.getVoxelBlob().blobToBytes(newFormat);

        return new ChiseledData(
                StringStates.getNameFromStateID(primaryBlockState), voxelBytes, sideState, lightValue, isNormalCube);
    }

    public final boolean writeToStack(final ItemStack stack, final boolean crossWorld) {
        final ChiseledData data = toComponent(crossWorld);
        if (data == null) {
            stack.remove(ModDataComponents.CHISELED_DATA);
            return false;
        }

        stack.set(ModDataComponents.CHISELED_DATA, data);
        clearLegacyChiseledData(stack);
        return true;
    }

    public final void writeChisleData(final ValueOutput output, final boolean crossWorld) {
        final VoxelBlobStateReference voxelRef = getRef();

        if (primaryBlockState == 0) {
            return;
        }

        final int newFormat = crossWorld ? VoxelBlob.VERSION_CROSSWORLD : VoxelBlob.VERSION_COMPACT_PALLETED;
        final byte[] voxelBytes = newFormat == format
                ? voxelRef.getByteArray()
                : voxelRef.getVoxelBlob().blobToBytes(newFormat);

        output.putInt(NBT_LIGHTVALUE, lightValue);
        if (crossWorld) {
            output.putString(NBT_PRIMARY_STATE, StringStates.getNameFromStateID(primaryBlockState));
        } else {
            output.putInt(NBT_PRIMARY_STATE, primaryBlockState);
        }
        output.putInt(NBT_SIDE_FLAGS, sideState);
        output.putBoolean(NBT_NORMALCUBE_FLAG, isNormalCube);
        output.store(NBT_VERSIONED_VOXEL, Codec.BYTE_BUFFER, ByteBuffer.wrap(voxelBytes));
    }

    public final boolean readChisleData(final ValueInput input, final int preferedFormat) {
        final String primaryState = input.getString(NBT_PRIMARY_STATE)
                .orElseGet(() -> StringStates.getNameFromStateID(input.getIntOr(NBT_PRIMARY_STATE, 0)));
        byte[] voxelBytes = input.read(NBT_VERSIONED_VOXEL, Codec.BYTE_BUFFER)
                .map(NBTBlobConverter::copyBytes)
                .orElseGet(() -> new byte[0]);
        final byte[] legacyBytes = input.read(NBT_LEGACY_VOXEL, Codec.BYTE_BUFFER)
                .map(NBTBlobConverter::copyBytes)
                .orElseGet(() -> new byte[0]);

        if (voxelBytes.length == 0 && legacyBytes.length > 0) {
            final VoxelBlob legacyBlob = new VoxelBlob();
            try {
                legacyBlob.fromLegacyByteArray(legacyBytes);
                voxelBytes = legacyBlob.blobToBytes(VoxelBlob.VERSION_COMPACT_PALLETED);
            } catch (final IOException ignored) {
                voxelBytes = new byte[0];
            }
        }

        return readChisleData(
                new ChiseledData(
                        primaryState,
                        voxelBytes,
                        input.getIntOr(NBT_SIDE_FLAGS, 0),
                        input.getIntOr(NBT_LIGHTVALUE, 0),
                        input.getBooleanOr(NBT_NORMALCUBE_FLAG, false)),
                preferedFormat);
    }

    public final boolean readChisleData(@Nullable final ChiseledData data, final int preferedFormat) {
        if (data == null) {
            return readChisleData((CompoundTag) null, preferedFormat);
        }

        sideState = data.sideState();
        primaryBlockState = StringStates.getStateIDFromName(data.primaryState());
        lightValue = data.lightValue();
        isNormalCube = data.normalCube();

        byte[] voxelBytes = data.voxelData();
        if (primaryBlockState == 0) {
            primaryBlockState = ModUtil.getStateId(Blocks.COBBLESTONE.defaultBlockState());
        }

        if (voxelBytes.length == 0) {
            voxelBlobRef = new VoxelBlobStateReference(0, 0);
            format = voxelBlobRef.getFormat();
        } else {
            voxelBlobRef = new VoxelBlobStateReference(voxelBytes, 0);
            format = voxelBlobRef.getFormat();
        }

        boolean formatChanged = false;
        if (preferedFormat != format && preferedFormat != VoxelBlob.VERSION_ANY) {
            formatChanged = true;
            voxelBytes = voxelBlobRef.getVoxelBlob().blobToBytes(preferedFormat);
            voxelBlobRef = new VoxelBlobStateReference(voxelBytes, 0);
            format = voxelBlobRef.getFormat();
        }

        if (tile != null) {
            if (formatChanged) {
                tile.setChanged();
            }

            return tile.updateBlob(this, triggerUpdates);
        }

        return true;
    }

    public final boolean readFromStack(final ItemStack stack, final int preferedFormat) {
        final ChiseledData component = stack.get(ModDataComponents.CHISELED_DATA);
        if (component != null) {
            return readChisleData(component, preferedFormat);
        }

        final CompoundTag legacy = findLegacyChiseledData(stack);
        if (legacy == null) {
            return false;
        }

        final Direction legacySide = findLegacyPlacementSide(stack, legacy);
        final boolean result = readChisleData(legacy, preferedFormat);
        final ChiseledData migrated = toComponent(true);
        if (migrated != null) {
            stack.set(ModDataComponents.CHISELED_DATA, migrated);
        }
        if (legacySide != null) {
            stack.set(ModDataComponents.PLACEMENT_SIDE, legacySide);
        }
        clearLegacyChiseledData(stack);
        return result;
    }

    @Nullable
    public static ChiseledData getComponent(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        ChiseledData component = stack.get(ModDataComponents.CHISELED_DATA);
        if (component == null) {
            final NBTBlobConverter converter = new NBTBlobConverter();
            if (converter.readFromStack(stack, VoxelBlob.VERSION_ANY)) {
                component = stack.get(ModDataComponents.CHISELED_DATA);
            }
        }
        return component;
    }

    public static boolean hasChiseledData(final ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && (stack.get(ModDataComponents.CHISELED_DATA) != null || findLegacyChiseledData(stack) != null);
    }

    private static byte[] copyBytes(final ByteBuffer source) {
        final ByteBuffer duplicate = source.duplicate();
        final byte[] bytes = new byte[duplicate.remaining()];
        duplicate.get(bytes);
        return bytes;
    }

    public final boolean readChisleData(final CompoundTag compound, final int preferedFormat) {
        if (compound == null) {
            voxelBlobRef = new VoxelBlobStateReference(0, 0);
            format = voxelBlobRef.getFormat();

            if (tile != null) {
                return tile.updateBlob(this, triggerUpdates);
            }

            return false;
        }

        sideState = compound.getIntOr(NBT_SIDE_FLAGS, 0);

        if (compound.get(NBT_PRIMARY_STATE) instanceof StringTag) {
            primaryBlockState = StringStates.getStateIDFromName(compound.getStringOr(NBT_PRIMARY_STATE, ""));
        } else {
            primaryBlockState = compound.getIntOr(NBT_PRIMARY_STATE, 0);
        }

        lightValue = compound.getIntOr(NBT_LIGHTVALUE, 0);
        isNormalCube = compound.getBooleanOr(NBT_NORMALCUBE_FLAG, false);
        byte[] v = compound.getByteArray(NBT_VERSIONED_VOXEL).orElseGet(() -> new byte[0]);

        if (v.length == 0) {
            final byte[] vx = compound.getByteArray(NBT_LEGACY_VOXEL).orElseGet(() -> new byte[0]);
            if (vx.length > 0) {
                final VoxelBlob bx = new VoxelBlob();

                try {
                    bx.fromLegacyByteArray(vx);
                } catch (final IOException e) {
                }

                v = bx.blobToBytes(VoxelBlob.VERSION_COMPACT_PALLETED);
                format = VoxelBlob.VERSION_COMPACT_PALLETED;
            }
        }

        if (primaryBlockState == 0) {
            // if load fails default to cobble stone...
            primaryBlockState = ModUtil.getStateId(Blocks.COBBLESTONE.defaultBlockState());
        }

        voxelBlobRef = new VoxelBlobStateReference(v, 0);
        format = voxelBlobRef.getFormat();

        boolean formatChanged = false;

        if (preferedFormat != format && preferedFormat != VoxelBlob.VERSION_ANY) {
            formatChanged = true;
            v = voxelBlobRef.getVoxelBlob().blobToBytes(preferedFormat);
            voxelBlobRef = new VoxelBlobStateReference(v, 0);
            format = voxelBlobRef.getFormat();
        }

        if (tile != null) {
            if (formatChanged) {
                // this only works on already loaded tiles, so i'm not sure
                // there is much point in it.
                tile.setChanged();
            }

            return tile.updateBlob(this, triggerUpdates);
        }

        return true;
    }

    public void updateFromBlob() {
        final VoxelBlob vb = getRef().getVoxelBlob();

        final VoxelStats common = vb.getVoxelStats();
        final float floatLight = common.blockLight;

        isNormalCube = common.isNormalBlock;
        lightValue = Math.max(0, Math.min(15, (int) (floatLight * 15)));
        sideState = vb.getSideFlags(5, 11, 4 * 4);
        primaryBlockState = common.mostCommonState;
    }

    public ItemStack getItemStack(final boolean crossWorld) {
        final Block blk = ModBlocks.getChiseledBlock();

        if (blk != null) {
            final ItemStack is = new ItemStack(blk);
            if (writeToStack(is, crossWorld)) {
                return is;
            }
        }

        return null;
    }

    private VoxelBlobStateReference getRef() {
        if (voxelBlobRef == null) {
            voxelBlobRef = new VoxelBlobStateReference(0, 0);
        }

        return voxelBlobRef;
    }

    public VoxelBlob getBlob() {
        return getRef().getVoxelBlob();
    }

    public void setBlob(final VoxelBlob vb) {
        voxelBlobRef = new VoxelBlobStateReference(vb, 0);
        format = voxelBlobRef.getFormat();
        updateFromBlob();
    }

    @Nullable
    private static CompoundTag findLegacyChiseledData(final ItemStack stack) {
        final TypedEntityData<?> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            final CompoundTag blockEntityTag = blockEntityData.copyTagWithoutId();
            if (hasLegacyPayload(blockEntityTag)) {
                return blockEntityTag;
            }
        }

        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return null;
        }

        final CompoundTag root = customData.copyTag();
        final CompoundTag nested = root.getCompoundOrEmpty(ModUtil.NBT_BLOCKENTITYTAG);
        if (hasLegacyPayload(nested)) {
            return nested;
        }
        return hasLegacyPayload(root) ? root : null;
    }

    private static boolean hasLegacyPayload(final CompoundTag tag) {
        return tag.contains(NBT_VERSIONED_VOXEL) || tag.contains(NBT_LEGACY_VOXEL);
    }

    @Nullable
    private static Direction findLegacyPlacementSide(final ItemStack stack, final CompoundTag chiseledData) {
        int ordinal = chiseledData.getIntOr(ModUtil.NBT_SIDE, Integer.MIN_VALUE);

        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (ordinal == Integer.MIN_VALUE && customData != null) {
            final CompoundTag root = customData.copyTag();
            ordinal = root.getIntOr(ModUtil.NBT_SIDE, Integer.MIN_VALUE);
            if (ordinal == Integer.MIN_VALUE) {
                ordinal = root.getCompoundOrEmpty(ModUtil.NBT_BLOCKENTITYTAG)
                        .getIntOr(ModUtil.NBT_SIDE, Integer.MIN_VALUE);
            }
        }

        if (ordinal < 0 || ordinal >= Direction.values().length) {
            return null;
        }

        final Direction side = Direction.values()[ordinal];
        return side.getAxis() == Direction.Axis.Y ? Direction.NORTH : side;
    }

    private static void clearLegacyChiseledData(final ItemStack stack) {
        final TypedEntityData<?> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null && hasLegacyPayload(blockEntityData.copyTagWithoutId())) {
            stack.remove(DataComponents.BLOCK_ENTITY_DATA);
        }

        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return;
        }

        final CompoundTag root = customData.copyTag();
        removeLegacyKeys(root);

        if (root.contains(ModUtil.NBT_BLOCKENTITYTAG)) {
            final CompoundTag nested = root.getCompoundOrEmpty(ModUtil.NBT_BLOCKENTITYTAG);
            removeLegacyKeys(nested);
            if (nested.isEmpty()) {
                root.remove(ModUtil.NBT_BLOCKENTITYTAG);
            }
        }

        ModUtil.setTagCompound(stack, root);
    }

    private static void removeLegacyKeys(final CompoundTag tag) {
        tag.remove(NBT_PRIMARY_STATE);
        tag.remove(NBT_LEGACY_VOXEL);
        tag.remove(NBT_VERSIONED_VOXEL);
        tag.remove(NBT_SIDE_FLAGS);
        tag.remove(NBT_NORMALCUBE_FLAG);
        tag.remove(NBT_LIGHTVALUE);
        tag.remove(ModUtil.NBT_SIDE);
        tag.remove("cw");
    }
}
