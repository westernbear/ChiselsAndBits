package mod.chiselsandbits.render;

import java.util.List;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.client.model.baked.LegacyItemOverrides;
import mod.chiselsandbits.core.ClientSide;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class NullBakedModel implements LegacyBakedModel {

    public static final NullBakedModel instance = new NullBakedModel();

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState blockState, @Nullable Direction direction, RandomSource randomSource) {
        return List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return ClientSide.instance.getMissingIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public LegacyItemOverrides getOverrides() {
        return LegacyItemOverrides.EMPTY;
    }
}
