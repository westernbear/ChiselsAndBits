package mod.chiselsandbits.client.model.baked;

import java.util.List;
import mod.chiselsandbits.client.model.data.IModelData;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.render.NullBakedModel;
import mod.chiselsandbits.render.chiseledblock.ChiselRenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseSmartModel implements DataAwareBakedModel {

    private final LegacyItemOverrides overrides;

    public BaseSmartModel() {
        overrides = new OverrideHelper(this);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        final TextureAtlasSprite sprite = Minecraft.getInstance()
                .getModelManager()
                .getBlockStateModelSet()
                .getParticleMaterial(Blocks.STONE.defaultBlockState())
                .sprite();

        if (sprite == null) {
            return ClientSide.instance.getMissingIcon();
        }

        return sprite;
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(
            @Nullable final BlockState state,
            @Nullable final Direction side,
            @NotNull final RandomSource rand,
            @NotNull final IModelData extraData,
            @NotNull final ChiselRenderType renderType) {

        final DataAwareBakedModel model = (DataAwareBakedModel) handleBlockState(state, rand, extraData, renderType);
        return model.getQuads(state, side, rand, extraData, renderType);
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable final BlockState state, @Nullable final Direction side, final RandomSource rand) {
        final LegacyBakedModel model = handleBlockState(state, rand);
        return model.getQuads(state, side, rand);
    }

    public LegacyBakedModel handleBlockState(final BlockState state, final RandomSource rand) {
        return NullBakedModel.instance;
    }

    public LegacyBakedModel handleBlockState(
            final BlockState state,
            final RandomSource random,
            final IModelData modelData,
            ChiselRenderType renderType) {
        return NullBakedModel.instance;
    }

    @Override
    public LegacyItemOverrides getOverrides() {
        return overrides;
    }

    public LegacyBakedModel resolve(
            final LegacyBakedModel originalModel, final ItemStack stack, final Level world, final LivingEntity entity) {
        return originalModel;
    }

    /** Resolves the packed legacy tint index to an ARGB item tint. */
    public int getItemTint(
            final ItemStack stack,
            final int packedTint,
            @Nullable final ClientLevel level,
            @Nullable final LivingEntity entity) {
        final BlockState containedState = ModUtil.getStateById(packedTint >>> 8);
        final int tintIndex = packedTint & 0xff;
        final BlockTintSource tintSource =
                Minecraft.getInstance().getBlockColors().getTintSource(containedState, tintIndex);
        final int color = tintSource == null ? -1 : tintSource.color(containedState);
        return (color & 0xff000000) == 0 ? color | 0xff000000 : color;
    }

    private static class OverrideHelper extends LegacyItemOverrides {
        final BaseSmartModel parent;

        public OverrideHelper(final BaseSmartModel p) {
            super();

            parent = p;
        }

        @Nullable
        @Override
        public LegacyBakedModel resolve(
                LegacyBakedModel bakedModel,
                ItemStack itemStack,
                @Nullable ClientLevel clientLevel,
                @Nullable LivingEntity livingEntity,
                int i) {
            return parent.resolve(bakedModel, itemStack, clientLevel, livingEntity);
        }
    }
}
