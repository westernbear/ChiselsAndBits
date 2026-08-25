package mod.chiselsandbits.client.model.baked;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import mod.chiselsandbits.utils.TransformationUtils;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class BaseBakedPerspectiveModel implements LegacyBakedModel {

    protected static final RandomSource RANDOM = RandomSource.create();

    protected static final Transformation ground;
    protected static final Transformation gui;
    protected static final Transformation fixed;
    protected static final Transformation firstPerson_righthand;
    protected static final Transformation firstPerson_lefthand;
    protected static final Transformation thirdPerson_righthand;
    protected static final Transformation thirdPerson_lefthand;

    private static final ItemTransforms ITEM_TRANSFORMS;

    static {
        gui = getMatrix(0, 0, 0, 30, 225, 0, 0.625f);
        ground = getMatrix(0, 3 / 16.0f, 0, 0, 0, 0, 0.25f);
        fixed = getMatrix(0, 0, 0, 0, 0, 0, 0.5f);
        thirdPerson_lefthand = thirdPerson_righthand = getMatrix(0, 2.5f / 16.0f, 0, 75, 45, 0, 0.375f);
        firstPerson_righthand = firstPerson_lefthand = getMatrix(0, 0, 0, 0, 45, 0, 0.40f);

        // ItemTransform.apply() always finishes with translate(-0.5) so models in 0..1
        // space rotate around their center. These must stay as ItemTransforms rather than
        // being folded into localTransform, or GUI atlas draws clip to empty slots.
        ITEM_TRANSFORMS = new ItemTransforms(
                itemTransform(0, 2.5f / 16.0f, 0, 75, 45, 0, 0.375f),
                itemTransform(0, 2.5f / 16.0f, 0, 75, 45, 0, 0.375f),
                itemTransform(0, 0, 0, 0, 45, 0, 0.40f),
                itemTransform(0, 0, 0, 0, 45, 0, 0.40f),
                ItemTransform.NO_TRANSFORM,
                itemTransform(0, 0, 0, 30, 225, 0, 0.625f),
                itemTransform(0, 3 / 16.0f, 0, 0, 0, 0, 0.25f),
                itemTransform(0, 0, 0, 0, 0, 0, 0.5f),
                itemTransform(0, 0, 0, 0, 0, 0, 0.5f));
    }

    private static Transformation getMatrix(
            final float transX,
            final float transY,
            final float transZ,
            final float rotX,
            final float rotY,
            final float rotZ,
            final float scaleXYZ) {
        final Vector3f translation = new Vector3f(transX, transY, transZ);
        final Vector3f scale = new Vector3f(scaleXYZ, scaleXYZ, scaleXYZ);
        final Quaternionf rotation = TransformationUtils.quatFromXYZ(rotX, rotY, rotZ, true);
        return new Transformation(translation, rotation, scale, null);
    }

    private static ItemTransform itemTransform(
            final float transX,
            final float transY,
            final float transZ,
            final float rotX,
            final float rotY,
            final float rotZ,
            final float scaleXYZ) {
        return new ItemTransform(
                new Vector3f(rotX, rotY, rotZ),
                new Vector3f(transX, transY, transZ),
                new Vector3f(scaleXYZ, scaleXYZ, scaleXYZ));
    }

    @Override
    public ItemTransforms getTransforms() {
        return ITEM_TRANSFORMS;
    }

    @Override
    public Matrix4fc getItemTransform(final ItemDisplayContext context) {
        return switch (context) {
            case FIRST_PERSON_LEFT_HAND -> firstPerson_lefthand.getMatrix();
            case FIRST_PERSON_RIGHT_HAND -> firstPerson_righthand.getMatrix();
            case THIRD_PERSON_LEFT_HAND -> thirdPerson_lefthand.getMatrix();
            case THIRD_PERSON_RIGHT_HAND ->
                new Matrix4f(thirdPerson_righthand.getMatrix()).mul(firstPerson_righthand.getMatrix());
            case FIXED -> firstPerson_righthand.getMatrix();
            case GROUND -> ground.getMatrix();
            case GUI -> gui.getMatrix();
            default -> fixed.getMatrix();
        };
    }

    public LegacyBakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
        switch (context) {
            case FIRST_PERSON_LEFT_HAND:
                TransformationUtils.push(poseStack, firstPerson_lefthand, false);
                return this;
            case FIRST_PERSON_RIGHT_HAND:
                TransformationUtils.push(poseStack, firstPerson_righthand, false);
                return this;
            case THIRD_PERSON_LEFT_HAND:
                TransformationUtils.push(poseStack, thirdPerson_lefthand, false);
                return this;
            case THIRD_PERSON_RIGHT_HAND:
                TransformationUtils.push(poseStack, thirdPerson_righthand, false);
            case FIXED:
                TransformationUtils.push(poseStack, firstPerson_righthand, false);
                return this;
            case GROUND:
                TransformationUtils.push(poseStack, ground, false);
                return this;
            case GUI:
                TransformationUtils.push(poseStack, gui, false);
                return this;
            default:
        }

        TransformationUtils.push(poseStack, fixed, false);
        return this;
    }
}
