package mod.chiselsandbits.chiseledblock;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import net.minecraft.world.phys.AABB;

public class BoxCollection extends AbstractCollection<AABB> {

    private final AABB[][] arrays;

    public BoxCollection(final AABB[]... arrays) {
        this.arrays = arrays;
    }

    @Override
    public Iterator<AABB> iterator() {
        return Arrays.stream(arrays)
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .iterator();
    }

    @Override
    public int size() {
        return Arrays.stream(arrays)
                .filter(Objects::nonNull)
                .mapToInt(array -> array.length)
                .sum();
    }
}
