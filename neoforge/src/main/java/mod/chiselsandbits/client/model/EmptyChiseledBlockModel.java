package mod.chiselsandbits.client.model;

import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;

/**
 * Placeholder baked model for the chiseled block. The real geometry is produced per block entity by
 * {@code ChiseledBlockRenderer}, so the block model contributes no quads.
 */
public final class EmptyChiseledBlockModel implements UnbakedModel {

    public static final EmptyChiseledBlockModel INSTANCE = new EmptyChiseledBlockModel();

    private EmptyChiseledBlockModel() {}

    @Override
    public UnbakedGeometry geometry() {
        return UnbakedGeometry.EMPTY;
    }
}
