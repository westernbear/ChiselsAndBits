package mod.chiselsandbits.chiseledblock.serialization;

import io.netty.buffer.Unpooled;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import net.minecraft.network.FriendlyByteBuf;

public class BlobSerilizationCache {

    private static final ThreadLocal<BitStream> bitbuffer = new ThreadLocal<BitStream>();
    private static final ThreadLocal<Deflater> deflater = new ThreadLocal<Deflater>();
    private static final ThreadLocal<ByteBuffer> buffer = new ThreadLocal<ByteBuffer>();
    private static final ThreadLocal<FriendlyByteBuf> pbuffer = new ThreadLocal<FriendlyByteBuf>();

    public static BitStream getCacheBitStream() {
        BitStream bb = bitbuffer.get();

        if (bb == null) {
            bb = new BitStream();
            bitbuffer.set(bb);
        }

        bb.reset();
        return bb;
    }

    public static Deflater getCacheDeflater() {
        Deflater bb = deflater.get();

        if (bb == null) {
            bb = new Deflater(Deflater.BEST_COMPRESSION);
            deflater.set(bb);
        }

        return bb;
    }

    public static ByteBuffer getCacheBuffer() {
        ByteBuffer bb = buffer.get();

        if (bb == null) {
            bb = ByteBuffer.allocate(3145728);
            buffer.set(bb);
        }

        return bb;
    }

    public static FriendlyByteBuf getCachePacketBuffer() {
        FriendlyByteBuf bb = pbuffer.get();

        if (bb == null) {
            bb = new FriendlyByteBuf(Unpooled.buffer());
            pbuffer.set(bb);
        }

        bb.resetReaderIndex();
        bb.resetWriterIndex();

        return bb;
    }
}
