package mod.chiselsandbits.render.bit;

import com.mojang.math.Quadrant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mod.chiselsandbits.client.model.baked.BaseBakedBlockModel;
import mod.chiselsandbits.client.model.data.IModelData;
import mod.chiselsandbits.core.ClientSide;
import mod.chiselsandbits.render.chiseledblock.ChiselRenderType;
import mod.chiselsandbits.render.helpers.ModelQuadLayer;
import mod.chiselsandbits.render.helpers.ModelUtil;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class BitItemBaked extends BaseBakedBlockModel {
    public static final float PIXELS_PER_BLOCK = 16.0f;

    private static final float BIT_BEGIN = 6.0f;
    private static final float BIT_END = 10.0f;
    private static final RandomSource RANDOM = RandomSource.create();
    private static final ModelBaker.Interner INTERNER = new ModelBaker.Interner() {
        @Override
        public Vector3fc vector(final Vector3fc value) {
            return value;
        }

        @Override
        public BakedQuad.MaterialInfo materialInfo(final BakedQuad.MaterialInfo value) {
            return value;
        }
    };
    final ArrayList<BakedQuad> generic = new ArrayList<BakedQuad>(6);

    public BitItemBaked(final int BlockRef) {
        final Vector3f to = new Vector3f(BIT_BEGIN, BIT_BEGIN, BIT_BEGIN);
        final Vector3f from = new Vector3f(BIT_END, BIT_END, BIT_END);

        for (final Direction myFace : Direction.values()) {
            for (final ChunkSectionLayer layer : ChunkSectionLayer.values()) {

                final ModelQuadLayer[] layers = ModelUtil.getCachedFace(BlockRef, RANDOM, myFace, layer);

                if (layers == null) {
                    continue;
                }

                for (final ModelQuadLayer clayer : layers) {
                    final float[] faceUvs = getFaceUvs(myFace);

                    Vector3f toB, fromB;

                    switch (myFace) {
                        case UP:
                            toB = new Vector3f(to.x(), from.y(), to.z());
                            fromB = new Vector3f(from.x(), from.y(), from.z());
                            break;
                        case EAST:
                            toB = new Vector3f(from.x(), to.y(), to.z());
                            fromB = new Vector3f(from.x(), from.y(), from.z());
                            break;
                        case NORTH:
                            toB = new Vector3f(to.x(), to.y(), to.z());
                            fromB = new Vector3f(from.x(), from.y(), to.z());
                            break;
                        case SOUTH:
                            toB = new Vector3f(to.x(), to.y(), from.z());
                            fromB = new Vector3f(from.x(), from.y(), from.z());
                            break;
                        case DOWN:
                            toB = new Vector3f(to.x(), to.y(), to.z());
                            fromB = new Vector3f(from.x(), to.y(), from.z());
                            break;
                        case WEST:
                            toB = new Vector3f(to.x(), to.y(), to.z());
                            fromB = new Vector3f(to.x(), from.y(), from.z());
                            break;
                        default:
                            throw new NullPointerException();
                    }
                    final int packedTint =
                            0 <= clayer.tint && clayer.tint <= 0xff ? (BlockRef << 8) | clayer.tint : clayer.tint;
                    final BakedQuad.MaterialInfo materialInfo = BakedQuad.MaterialInfo.of(
                            new Material.Baked(clayer.sprite, false),
                            clayer.sprite.transparency(),
                            packedTint,
                            true,
                            clayer.light);
                    generic.add(FaceBakery.bakeQuad(
                            INTERNER,
                            toB,
                            fromB,
                            new CuboidFace.UVs(faceUvs[0], faceUvs[1], faceUvs[2], faceUvs[3]),
                            Quadrant.R0,
                            materialInfo,
                            myFace,
                            BlockModelRotation.IDENTITY,
                            null));
                }
            }
        }

        generic.trimToSize();
    }

    private float[] getFaceUvs(final Direction face) {
        float[] afloat;

        final int from_x = 7;
        final int from_y = 7;
        final int from_z = 7;

        final int to_x = 8;
        final int to_y = 8;
        final int to_z = 8;

        switch (face) {
            case DOWN:
            case UP:
                afloat = new float[] {from_x, from_z, to_x, to_z};
                break;
            case NORTH:
            case SOUTH:
                afloat = new float[] {from_x, PIXELS_PER_BLOCK - to_y, to_x, PIXELS_PER_BLOCK - from_y};
                break;
            case WEST:
            case EAST:
                afloat = new float[] {from_z, PIXELS_PER_BLOCK - to_y, to_z, PIXELS_PER_BLOCK - from_y};
                break;
            default:
                throw new NullPointerException();
        }

        return afloat;
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState blockState, @Nullable Direction direction, RandomSource randomSource) {
        if (direction != null) {
            return Collections.emptyList();
        }

        return generic;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return ClientSide.instance.getMissingIcon();
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            @NotNull RandomSource rand,
            @NotNull IModelData extraData,
            @NotNull ChiselRenderType renderType) {
        return getQuads(state, side, rand);
    }
}
