package mod.chiselsandbits.chiseledblock.serialization;

import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import net.minecraft.network.FriendlyByteBuf;

public class CrossWorldBlobSerializer extends BlobSerializer {

    public CrossWorldBlobSerializer(final FriendlyByteBuf toInflate) {
        super(toInflate);
    }

    public CrossWorldBlobSerializer(final VoxelBlob toDeflate) {
        super(toDeflate);
    }

    @Override
    protected int readStateID(final FriendlyByteBuf buffer) {
        final String name = buffer.readUtf();
        return StringStates.getStateIDFromName(name);
    }

    @Override
    protected void writeStateID(final FriendlyByteBuf buffer, final int key) {
        final String sname = StringStates.getNameFromStateID(key);

        buffer.writeUtf(sname);
    }

    @Override
    public int getVersion() {
        return VoxelBlob.VERSION_CROSSWORLD;
    }
}
