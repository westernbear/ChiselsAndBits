package mod.chiselsandbits.client.model;

import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;

/**
 * Placeholder model for the chiseled block. Baking swaps it for {@link ChiseledBlockStateModel}, which meshes each
 * block entity's bits into the chunk; this one only supplies the particle and material flags.
 */
public final class EmptyChiseledBlockModel implements UnbakedModel {

    public static final EmptyChiseledBlockModel INSTANCE = new EmptyChiseledBlockModel();

    private EmptyChiseledBlockModel() {}

    @Override
    public UnbakedGeometry geometry() {
        return UnbakedGeometry.EMPTY;
    }
}
