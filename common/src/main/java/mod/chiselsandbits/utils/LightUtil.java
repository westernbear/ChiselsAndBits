package mod.chiselsandbits.utils;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import mod.chiselsandbits.utils.forge.IVertexConsumer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;

public final class LightUtil {

    private LightUtil() {
        throw new IllegalStateException("Tried to construct a LightUtil instance, but this is a utility class!");
    }

    public static void pipe(final IVertexConsumer consumer) {}

    /** Replays the 26.2 baked-quad record through the legacy C&B model readers. */
    public static void put(final IVertexConsumer consumer, final BakedQuad quad) {
        final BakedQuad.MaterialInfo material = quad.materialInfo();
        consumer.setTexture(material.sprite());
        consumer.setQuadOrientation(quad.direction());
        if (material.isTinted()) {
            consumer.setQuadTint(material.tintIndex());
        }
        consumer.setApplyDiffuseLighting(material.shade());

        final VertexFormat format = consumer.getVertexFormat();
        final int elementCount = format.getElements().size();
        final float packedLight = material.lightEmission() * (32.0f / 0xffff);
        for (int vertex = 0; vertex < 4; vertex++) {
            final Vector3fc position = quad.position(vertex);
            final long packedUv = quad.packedUV(vertex);
            for (int elementIndex = 0; elementIndex < elementCount; elementIndex++) {
                final VertexFormatElement element = format.getElements().get(elementIndex);
                switch (element.name()) {
                    case DefaultVertexFormat.POSITION_SEMANTIC_NAME ->
                        consumer.put(vertex, elementIndex, position.x(), position.y(), position.z(), 1);
                    case DefaultVertexFormat.COLOR_SEMANTIC_NAME -> consumer.put(vertex, elementIndex, 1, 1, 1, 1);
                    case DefaultVertexFormat.UV0_SEMANTIC_NAME ->
                        consumer.put(vertex, elementIndex, UVPair.unpackU(packedUv), UVPair.unpackV(packedUv));
                    case DefaultVertexFormat.UV2_SEMANTIC_NAME ->
                        consumer.put(vertex, elementIndex, packedLight, packedLight);
                    case DefaultVertexFormat.NORMAL_SEMANTIC_NAME ->
                        consumer.put(
                                vertex,
                                elementIndex,
                                quad.direction().getStepX(),
                                quad.direction().getStepY(),
                                quad.direction().getStepZ());
                    default -> consumer.put(vertex, elementIndex);
                }
            }
        }

        consumer.onComplete();
    }

    public static float diffuseLight(final Direction side) {
        return switch (side) {
            case DOWN -> .5f;
            case UP -> 1f;
            case NORTH, SOUTH -> .8f;
            default -> .6f;
        };
    }
}
