package mod.chiselsandbits.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleMaxSizedCache<K, V> extends LinkedHashMap<K, V> {

    private long maxSize;

    public SimpleMaxSizedCache(final long maxSize) {
        // Access order: evict the least recently used entry, not the oldest insert.
        super(16, 0.75f, true);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }

    public void changeMaxSize(final long newSize) {
        if (maxSize != newSize) {
            clear();
            maxSize = newSize;
        }
    }
}
