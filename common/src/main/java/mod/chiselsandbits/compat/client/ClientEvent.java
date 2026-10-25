package mod.chiselsandbits.compat.client;

import java.lang.reflect.Array;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Minimal loader-neutral replacement for Fabric's {@code Event}/{@code EventFactory}.
 *
 * <p>The facade APIs in this package were written against Fabric's event API; keeping the same shape lets the shared
 * client code serve both loaders.
 */
public final class ClientEvent<T> {

    private final Class<T> type;
    private final List<T> listeners = new CopyOnWriteArrayList<>();
    private final Function<T[], T> combiner;

    private ClientEvent(final Class<T> type, final Function<T[], T> combiner) {
        this.type = type;
        this.combiner = combiner;
    }

    public static <T> ClientEvent<T> arrayBacked(final Class<T> type, final Function<T[], T> combiner) {
        return new ClientEvent<>(type, combiner);
    }

    public void register(final T listener) {
        listeners.add(listener);
    }

    @SuppressWarnings("unchecked")
    public T invoker() {
        final T[] array = (T[]) Array.newInstance(type, listeners.size());
        return combiner.apply(listeners.toArray(array));
    }
}
