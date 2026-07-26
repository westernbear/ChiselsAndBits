package mod.chiselsandbits.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TransformationUtils {

    private TransformationUtils() {
        throw new IllegalStateException("Tried to initialize TransformationUtils. But this is a utility class!");
    }

    public static void push(PoseStack stack, final Transformation transformation, final boolean requiresStackPush) {
        if (requiresStackPush) {
            stack.pushPose();
        }

        var trans = transformation.translation();
        stack.translate(trans.x(), trans.y(), trans.z());

        stack.mulPose(transformation.leftRotation());

        var scale = transformation.scale();
        stack.scale(scale.x(), scale.y(), scale.z());

        stack.mulPose(transformation.rightRotation());
    }

    public static Quaternionf quatFromXYZ(Vector3f xyz, boolean degrees) {
        return quatFromXYZ(xyz.x, xyz.y, xyz.z, degrees);
    }

    public static Quaternionf quatFromXYZ(float[] xyz, boolean degrees) {
        return quatFromXYZ(xyz[0], xyz[1], xyz[2], degrees);
    }

    public static Quaternionf quatFromXYZ(float x, float y, float z, boolean degrees) {
        float conversionFactor = degrees ? 0.017453292F : 1.0F;
        return (new Quaternionf()).rotationXYZ(x * conversionFactor, y * conversionFactor, z * conversionFactor);
    }
}
