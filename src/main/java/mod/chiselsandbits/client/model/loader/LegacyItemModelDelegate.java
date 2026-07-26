package mod.chiselsandbits.client.model.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mod.chiselsandbits.client.model.baked.BaseSmartModel;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.render.NullBakedModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

/** Adapts C&B's cached legacy quad generators to the 26.2 item model pipeline. */
public final class LegacyItemModelDelegate implements ItemModel {

    private final BaseSmartModel dynamicModel;
    private final ItemModel fallback;
    private final Matrix4fc bakeTransformation;

    public LegacyItemModelDelegate(
            final BaseSmartModel dynamicModel, final ItemModel fallback, final Matrix4fc bakeTransformation) {
        this.dynamicModel = dynamicModel;
        this.fallback = fallback;
        this.bakeTransformation = bakeTransformation;
    }

    @Override
    public void update(
            final ItemStackRenderState renderState,
            final ItemStack stack,
            final ItemModelResolver resolver,
            final ItemDisplayContext displayContext,
            final ClientLevel level,
            final ItemOwner itemOwner,
            final int seed) {
        final LivingEntity livingEntity = itemOwner == null ? null : itemOwner.asLivingEntity();
        final LegacyBakedModel resolved = dynamicModel.resolve(NullBakedModel.instance, stack, level, livingEntity);

        if (resolved == null || resolved == NullBakedModel.instance || resolved == dynamicModel) {
            fallback.update(renderState, stack, resolver, displayContext, level, itemOwner, seed);
            return;
        }

        renderState.appendModelIdentityElement(dynamicModel);
        renderState.appendModelIdentityElement(resolved);

        final ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
        final List<BakedQuad> quads = collectQuads(resolved, seed);
        remapTints(quads, layer, stack, level, livingEntity);

        layer.setUsesBlockLight(resolved.usesBlockLight());
        final TextureAtlasSprite particle = resolved.getParticleIcon();
        if (particle != null) {
            layer.setParticleMaterial(new Material.Baked(particle, false));
        }

        final Matrix4f transform = new Matrix4f(resolved.getItemTransform(displayContext));
        if (bakeTransformation != null) {
            transform.mul(bakeTransformation);
        }
        layer.setLocalTransform(transform);

        final Vector3fc[] extents = CuboidItemModelWrapper.computeExtents(quads);
        layer.setExtents(() -> extents);

        if (stack.hasFoil()) {
            layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
            renderState.setAnimated();
        }
    }

    private static List<BakedQuad> collectQuads(final LegacyBakedModel model, final int seed) {
        final RandomSource random = RandomSource.create(seed);
        final List<BakedQuad> quads = new ArrayList<>();

        for (final Direction direction : Direction.values()) {
            quads.addAll(model.getQuads(null, direction, random));
        }
        quads.addAll(model.getQuads(null, null, random));

        return quads;
    }

    private void remapTints(
            final List<BakedQuad> quads,
            final ItemStackRenderState.LayerRenderState layer,
            final ItemStack stack,
            final ClientLevel level,
            final LivingEntity livingEntity) {
        final Map<Integer, Integer> remappedIndices = new HashMap<>();
        final List<BakedQuad> output = layer.prepareQuadList();

        for (final BakedQuad quad : quads) {
            final int packedTint = quad.materialInfo().tintIndex();
            if (packedTint < 0) {
                output.add(quad);
                continue;
            }

            final int tintIndex = remappedIndices.computeIfAbsent(packedTint, ignored -> {
                final int newIndex = layer.tintLayers().size();
                layer.tintLayers().add(dynamicModel.getItemTint(stack, packedTint, level, livingEntity));
                return newIndex;
            });
            output.add(withTintIndex(quad, tintIndex));
        }
    }

    private static BakedQuad withTintIndex(final BakedQuad quad, final int tintIndex) {
        final BakedQuad.MaterialInfo material = quad.materialInfo();
        final BakedQuad.MaterialInfo remappedMaterial = new BakedQuad.MaterialInfo(
                material.sprite(),
                material.layer(),
                material.itemRenderType(),
                tintIndex,
                material.shade(),
                material.lightEmission());

        return new BakedQuad(
                quad.position0(),
                quad.position1(),
                quad.position2(),
                quad.position3(),
                quad.packedUV0(),
                quad.packedUV1(),
                quad.packedUV2(),
                quad.packedUV3(),
                quad.direction(),
                remappedMaterial);
    }
}
