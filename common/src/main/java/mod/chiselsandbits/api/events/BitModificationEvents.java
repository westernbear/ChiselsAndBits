package mod.chiselsandbits.api.events;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import mod.chiselsandbits.api.EventBlockBitModification;
import mod.chiselsandbits.api.EventBlockBitPostModification;
import mod.chiselsandbits.api.EventFullBlockRestoration;

/** Loader-agnostic Chisels & Bits event bus. */
public final class BitModificationEvents {

    private BitModificationEvents() {}

    public static final Event<BlockBitModificationListener> BLOCK_BIT_MODIFICATION =
            EventFactory.createLoop(BlockBitModificationListener.class);

    public static final Event<BlockBitPostModificationListener> BLOCK_BIT_POST_MODIFICATION =
            EventFactory.createLoop(BlockBitPostModificationListener.class);

    public static final Event<FullBlockRestorationListener> FULL_BLOCK_RESTORATION =
            EventFactory.createLoop(FullBlockRestorationListener.class);

    @FunctionalInterface
    public interface BlockBitModificationListener {
        void handle(EventBlockBitModification event);
    }

    @FunctionalInterface
    public interface BlockBitPostModificationListener {
        void handle(EventBlockBitPostModification event);
    }

    @FunctionalInterface
    public interface FullBlockRestorationListener {
        void handle(EventFullBlockRestoration event);
    }

    public static void registerBlockBitModification(BlockBitModificationListener listener) {
        BLOCK_BIT_MODIFICATION.register(listener);
    }

    public static void registerBlockBitPostModification(BlockBitPostModificationListener listener) {
        BLOCK_BIT_POST_MODIFICATION.register(listener);
    }

    public static void registerFullBlockRestoration(FullBlockRestorationListener listener) {
        FULL_BLOCK_RESTORATION.register(listener);
    }
}
