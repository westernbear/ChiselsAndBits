package mod.chiselsandbits.client.model.baked;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * Internal compatibility boundary for the pre-26.2 C&B model generators.
 *
 * <p>Minecraft 26.2 split the former baked-model contract into block-state and
 * item-model pipelines. Keeping the generated quad representation behind this
 * interface lets those two pipelines share the existing geometry caches without
 * pretending this is still a vanilla model type.
 */
public interface LegacyBakedModel {
    default List<BakedQuad> getQuads(
            @Nullable final BlockState state, @Nullable final Direction side, final RandomSource random) {
        return Collections.emptyList();
    }

    default boolean useAmbientOcclusion() {
        return true;
    }

    default boolean isGui3d() {
        return true;
    }

    default boolean usesBlockLight() {
        return true;
    }

    default boolean isCustomRenderer() {
        return false;
    }

    @Nullable
    default TextureAtlasSprite getParticleIcon() {
        return null;
    }

    default ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    /**
     * Returns the legacy display transform as a matrix for the 26.2 item
     * render-state pipeline.
     */
    default Matrix4fc getItemTransform(final ItemDisplayContext context) {
        return new Matrix4f();
    }

    default LegacyItemOverrides getOverrides() {
        return LegacyItemOverrides.EMPTY;
    }
}
