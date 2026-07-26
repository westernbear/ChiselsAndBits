package mod.chiselsandbits.client.model.baked;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import mod.chiselsandbits.utils.TransformationUtils;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class BaseBakedPerspectiveModel implements BakedModel {

    protected static final RandomSource RANDOM = RandomSource.create();

    protected static final Transformation ground;
    protected static final Transformation gui;
    protected static final Transformation fixed;
    protected static final Transformation firstPerson_righthand;
    protected static final Transformation firstPerson_lefthand;
    protected static final Transformation thirdPerson_righthand;
    protected static final Transformation thirdPerson_lefthand;

    static {
        gui = getMatrix(0, 0, 0, 30, 225, 0, 0.625f);
        ground = getMatrix(0, 3 / 16.0f, 0, 0, 0, 0, 0.25f);
        fixed = getMatrix(0, 0, 0, 0, 0, 0, 0.5f);
        thirdPerson_lefthand = thirdPerson_righthand = getMatrix(0, 2.5f / 16.0f, 0, 75, 45, 0, 0.375f);
        firstPerson_righthand = firstPerson_lefthand = getMatrix(0, 0, 0, 0, 45, 0, 0.40f);
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

    @Override
    public ItemTransforms getTransforms() {
        return new PerspectiveItemModelDelegate();
    }

    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
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

    private final class PerspectiveItemModelDelegate extends ItemTransforms {
        private PerspectiveItemModelDelegate() {
            super(
                    ItemTransform.NO_TRANSFORM,
                    ItemTransform.NO_TRANSFORM,
                    ItemTransform.NO_TRANSFORM,
                    ItemTransform.NO_TRANSFORM,
                    ItemTransform.NO_TRANSFORM,
                    ItemTransform.NO_TRANSFORM,
                    ItemTransform.NO_TRANSFORM,
                    ItemTransform.NO_TRANSFORM);
        }

        @Override
        public ItemTransform getTransform(ItemDisplayContext itemDisplayContext) {
            return new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f()) {
                @Override
                public void apply(boolean bl, PoseStack poseStack) {
                    BaseBakedPerspectiveModel.this.applyTransform(itemDisplayContext, poseStack, bl);
                }
            };
        }
    }
}
