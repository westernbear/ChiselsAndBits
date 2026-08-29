package mod.chiselsandbits.render.chiseledblock;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;

public interface IFaceBuilder {

    void setFace(Direction myFace, int tintIndex);

    void put(int element, float... args);

    default void put(int vertexIndex, int element, float... args) {}

    void begin();

    BakedQuad create(TextureAtlasSprite sprite);

    VertexFormat getFormat();
}
