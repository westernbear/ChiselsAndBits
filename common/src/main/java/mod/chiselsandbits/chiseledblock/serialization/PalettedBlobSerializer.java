package mod.chiselsandbits.chiseledblock.serialization;

import java.util.List;
import java.util.Map;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.utils.PaletteUtils;
import net.minecraft.core.IdMapper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.HashMapPalette;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PaletteResize;

public class PalettedBlobSerializer extends BlobSerializer implements PaletteResize<BlockState> {
    private final IdMapper<BlockState> registry = Block.BLOCK_STATE_REGISTRY;
    private Palette<BlockState> palette = new GlobalPalette<>(Block.BLOCK_STATE_REGISTRY);
    private int bits = 0;

    public PalettedBlobSerializer(final VoxelBlob toDeflate) {
        super(toDeflate);
        this.setBits(4);

        // Setup the palette ids.
        final Map<Integer, Integer> entries = toDeflate.getBlockSums();
        for (final Map.Entry<Integer, Integer> o : entries.entrySet()) {
            this.palette.idFor(ModUtil.getStateById(o.getKey()), this);
        }
    }

    public PalettedBlobSerializer(final FriendlyByteBuf toInflate) {
        super();
        this.setBits(4);

        // Setup the palette ids.
        this.setBits(toInflate.readVarInt());
        this.palette.read(toInflate, registry);
    }

    private void setBits(int bitsIn) {
        setBits(bitsIn, false);
    }

    private void setBits(int bitsIn, boolean forceBits) {
        if (bitsIn != this.bits) {
            this.bitsPerInt = bitsIn;
            this.bitsPerIntMinus1 = bitsIn - 1;

            this.bits = bitsIn;
            //            if (this.bits <= 8) {
            //                this.bits = 4;
            //                this.palette = new LinearPalette<>(this.registry, this.bits, this,
            // NbtUtils::readBlockState);
            //            } else if (this.bits < 17) {
            //                this.palette = new HashMapPalette<>(this.registry, this.bits, this,
            // NbtUtils::readBlockState, NbtUtils::writeBlockState);
            //            } else {
            //                this.palette = this.registryPalette;
            //                this.bits = Mth.ceillog2(this.registry.size());
            //                if (forceBits)
            //                    this.bits = bitsIn;
            //            }
            this.palette = new HashMapPalette<>(this.bits);
            this.palette.idFor(Blocks.AIR.defaultBlockState(), this);
        }
    }

    @Override
    public void write(final FriendlyByteBuf to) {
        to.writeVarInt(this.bits);
        this.palette.write(to, registry);
    }

    @Override
    protected int readStateID(final FriendlyByteBuf buffer) {
        // Not needed because of different palette system.
        return 0;
    }

    @Override
    protected void writeStateID(final FriendlyByteBuf buffer, final int key) {
        // Noop
    }

    @Override
    protected int getIndex(final int stateID) {
        return this.palette.idFor(ModUtil.getStateById(stateID), this);
    }

    @Override
    protected int getStateID(final int indexID) {
        return ModUtil.getStateId(this.palette.valueFor(indexID));
    }

    @Override
    public int getVersion() {
        return VoxelBlob.VERSION_COMPACT_PALLETED;
    }

    @Override
    public int onResize(final int newBitSize, final BlockState violatingBlockState) {
        final Palette<BlockState> currentPalette = this.palette;
        this.setBits(newBitSize);

        final List<BlockState> ids = PaletteUtils.getOrderedListInPalette(currentPalette);
        ids.forEach(state -> this.palette.idFor(state, this));

        return this.palette.idFor(violatingBlockState, this);
    }
}
