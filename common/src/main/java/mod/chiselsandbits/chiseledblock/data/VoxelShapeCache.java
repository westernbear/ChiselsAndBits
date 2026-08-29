package mod.chiselsandbits.chiseledblock.data;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.BitSet;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import mod.chiselsandbits.api.BoxType;
import mod.chiselsandbits.core.ChiselsAndBits;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class VoxelShapeCache {

    private static final VoxelShapeCache INSTANCE = new VoxelShapeCache();
    private volatile Cache<CacheKey, VoxelShape> cache = createCache();

    private VoxelShapeCache() {}

    public static VoxelShapeCache getInstance() {
        return INSTANCE;
    }

    public static void onConfigurationReload() {
        INSTANCE.cache = createCache();
    }

    public VoxelShape get(VoxelBlob blob, BoxType type) {
        final CacheKey key = new CacheKey(type, (BitSet) blob.getNoneAir().clone());
        try {
            return cache.get(key, () -> calculateNewVoxelShape(blob, type));
        } catch (ExecutionException e) {
            return null;
        }
    }

    private VoxelShape calculateNewVoxelShape(final VoxelBlob data, final BoxType type) {
        return VoxelShapeCalculator.calculate(data, type).optimize();
    }

    private static Cache<CacheKey, VoxelShape> createCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(ChiselsAndBits.getConfig()
                        .getCommon()
                        .collisionBoxCacheSize
                        .get())
                .build();
    }

    private static final class CacheKey {
        private final BoxType type;
        private final BitSet noneAirMap;

        private CacheKey(final BoxType type, final BitSet noneAirMap) {
            this.type = type;
            this.noneAirMap = noneAirMap;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CacheKey cacheKey)) {
                return false;
            }
            return type == cacheKey.type && noneAirMap.equals(cacheKey.noneAirMap);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, noneAirMap);
        }
    }
}
