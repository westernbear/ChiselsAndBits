package mod.chiselsandbits.render.helpers;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import mod.chiselsandbits.render.chiseledblock.IFaceBuilder;
import mod.chiselsandbits.utils.forge.IVertexConsumer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

public final class BakedQuadBuilder implements IVertexConsumer, IFaceBuilder {
    private static final int SIZE = DefaultVertexFormat.BLOCK.getElements().size();
    private float[][][] unpackedData = new float[4][SIZE][4];
    private int tint = -1;
    private Direction orientation;
    private TextureAtlasSprite texture;
    private boolean applyDiffuseLighting = true;
    private int vertices = 0;
    private int elements = 0;
    private VertexFormat vertexFormat;
    private ChunkSectionLayer chunkLayer;

    public BakedQuadBuilder(TextureAtlasSprite texture) {
        this.texture = texture;
    }

    public BakedQuadBuilder(VertexFormat vertexFormat) {
        this.vertexFormat = vertexFormat;
    }

    public BakedQuadBuilder(VertexFormat vertexFormat, ChunkSectionLayer chunkLayer) {
        this.vertexFormat = vertexFormat;
        this.chunkLayer = chunkLayer;
    }

    @Override
    public VertexFormat getVertexFormat() {
        return vertexFormat;
    }

    public void setVertexFormat(VertexFormat vertexFormat) {
        this.vertexFormat = vertexFormat;
    }

    @Override
    public void setQuadTint(int tint) {
        this.tint = tint;
    }

    @Override
    public void setQuadOrientation(Direction orientation) {
        this.orientation = orientation;
    }

    @Override
    public void setTexture(TextureAtlasSprite texture) {
        this.texture = texture;
    }

    @Override
    public void setApplyDiffuseLighting(boolean diffuse) {
        this.applyDiffuseLighting = diffuse;
    }

    @Override
    public void put(int vertexIndex, int element, float... data) {
        //        for (int i = 0; i < 4; i++) {
        //            if (i < data.length) {
        //                unpackedData[vertexIndex][element][i] = data[i];
        //            } else {
        //                unpackedData[vertexIndex][element][i] = 0;
        //            }
        //        }
        //        elements++;
        //        if (elements == SIZE) {
        //            elements = 0;
        //        }
        //        if (vertexIndex == 4) {
        //            full = true;
        //        }
        put(element, data);
    }

    @Override
    public void setFace(Direction myFace, int tintIndex) {
        setQuadOrientation(myFace);
        setQuadTint(tintIndex);
    }

    @Override
    public void put(final int element, final float... data) {
        for (int i = 0; i < 4; i++) {
            if (i < data.length) {
                unpackedData[vertices][element][i] = data[i];
            } else {
                unpackedData[vertices][element][i] = 0;
            }
        }

        elements++;

        if (elements == getVertexFormat().getElements().size()) {
            vertices++;
            elements = 0;
        }
    }

    @Override
    public void begin() {
        unpackedData = new float[4][getVertexFormat().getElements().size()][4];
        tint = -1;
        orientation = null;
        texture = null;
        vertices = 0;
        elements = 0;
    }

    @Override
    public BakedQuad create(TextureAtlasSprite sprite) {
        setTexture(sprite);
        return build();
    }

    @Override
    public VertexFormat getFormat() {
        return vertexFormat;
    }

    public BakedQuad build() {
        if (texture == null) {
            throw new IllegalStateException("texture not set");
        }

        final int positionElement = findElement(DefaultVertexFormat.POSITION_SEMANTIC_NAME);
        final int uvElement = findElement(DefaultVertexFormat.UV0_SEMANTIC_NAME);
        final int lightElement = findElement(DefaultVertexFormat.UV2_SEMANTIC_NAME);
        if (positionElement < 0 || uvElement < 0 || orientation == null) {
            throw new IllegalStateException("incomplete quad data");
        }

        final Vector3f[] positions = new Vector3f[4];
        final long[] packedUvs = new long[4];
        for (int vertex = 0; vertex < 4; vertex++) {
            final float[] position = unpackedData[vertex][positionElement];
            final float[] uv = unpackedData[vertex][uvElement];
            positions[vertex] = new Vector3f(position[0], position[1], position[2]);
            packedUvs[vertex] = UVPair.pack(uv[0], uv[1]);
        }

        int lightEmission = 0;
        if (lightElement >= 0) {
            final float maxLightmap = 32.0f / 0xffff;
            lightEmission = Math.max(0, Math.min(15, Math.round(unpackedData[0][lightElement][0] / maxLightmap)));
        }

        final BakedQuad.MaterialInfo derived = BakedQuad.MaterialInfo.of(
                new Material.Baked(texture, false), texture.transparency(), tint, applyDiffuseLighting, lightEmission);
        final BakedQuad.MaterialInfo materialInfo = new BakedQuad.MaterialInfo(
                texture,
                chunkLayer == null ? derived.layer() : chunkLayer,
                derived.itemRenderType(),
                tint,
                applyDiffuseLighting,
                lightEmission);

        return new BakedQuad(
                positions[0],
                positions[1],
                positions[2],
                positions[3],
                packedUvs[0],
                packedUvs[1],
                packedUvs[2],
                packedUvs[3],
                orientation,
                materialInfo);
    }

    private int findElement(final String name) {
        final var elements = getVertexFormat().getElements();
        for (int index = 0; index < elements.size(); index++) {
            final VertexFormatElement element = elements.get(index);
            if (name.equals(element.name())) {
                return index;
            }
        }

        return -1;
    }
}
