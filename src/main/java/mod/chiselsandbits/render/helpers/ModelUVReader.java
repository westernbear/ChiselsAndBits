package mod.chiselsandbits.render.helpers;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.Arrays;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class ModelUVReader extends BaseModelReader {

    public final float[] quadUVs = new float[] {0, 0, 0, 1, 1, 0, 1, 1};
    final float minU;
    final float maxUMinusMin;
    final float minV;
    final float maxVMinusMin;
    public int corners;
    int uCoord, vCoord;
    private float[] pos;
    private float[] uv;

    public ModelUVReader(final TextureAtlasSprite texture, final int uFaceCoord, final int vFaceCoord) {
        minU = texture.getU0();
        maxUMinusMin = texture.getU1() - minU;

        minV = texture.getV0();
        maxVMinusMin = texture.getV1() - minV;

        uCoord = uFaceCoord;
        vCoord = vFaceCoord;
    }

    @Override
    public void put(final int vertexIndex, final int element, final float... data) {
        final VertexFormat format = getVertexFormat();
        final VertexFormatElement ele = format.getElements().get(element);

        if (DefaultVertexFormat.UV0_SEMANTIC_NAME.equals(ele.name())) {
            uv = Arrays.copyOf(data, data.length);
        } else if (DefaultVertexFormat.POSITION_SEMANTIC_NAME.equals(ele.name())) {
            pos = Arrays.copyOf(data, data.length);
        }

        if (element == format.getElements().size() - 1) {
            if (ModelUtil.isZero(pos[uCoord]) && ModelUtil.isZero(pos[vCoord])) {
                corners = corners | 0x1;
                quadUVs[0] = (uv[0] - minU) / maxUMinusMin;
                quadUVs[1] = (uv[1] - minV) / maxVMinusMin;
            } else if (ModelUtil.isZero(pos[uCoord]) && ModelUtil.isOne(pos[vCoord])) {
                corners = corners | 0x2;
                quadUVs[4] = (uv[0] - minU) / maxUMinusMin;
                quadUVs[5] = (uv[1] - minV) / maxVMinusMin;
            } else if (ModelUtil.isOne(pos[uCoord]) && ModelUtil.isZero(pos[vCoord])) {
                corners = corners | 0x4;
                quadUVs[2] = (uv[0] - minU) / maxUMinusMin;
                quadUVs[3] = (uv[1] - minV) / maxVMinusMin;
            } else if (ModelUtil.isOne(pos[uCoord]) && ModelUtil.isOne(pos[vCoord])) {
                corners = corners | 0x8;
                quadUVs[6] = (uv[0] - minU) / maxUMinusMin;
                quadUVs[7] = (uv[1] - minV) / maxVMinusMin;
            }
        }
    }
}
