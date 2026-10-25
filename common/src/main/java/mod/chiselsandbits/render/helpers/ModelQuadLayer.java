package mod.chiselsandbits.render.helpers;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;

public class ModelQuadLayer {

    public float[] uvs;
    public TextureAtlasSprite sprite;
    public int light;
    public int color;
    public int tint;

    public BakedQuad sourceQuad;

    public Direction face;

    private volatile BakedQuad.MaterialInfo material;

    /**
     * The material every chiseled quad of this layer shares. A layer is cached per (state, chunk layer, face), so the
     * chunk layer and packed tint never change for one instance.
     */
    public BakedQuad.MaterialInfo material(final ChunkSectionLayer chunkLayer, final int packedTint) {
        BakedQuad.MaterialInfo cached = material;
        if (cached == null) {
            final int lightEmission = Math.max(0, Math.min(15, light));
            final BakedQuad.MaterialInfo derived = BakedQuad.MaterialInfo.of(
                    new Material.Baked(sprite, false), sprite.transparency(), packedTint, true, lightEmission);
            cached = new BakedQuad.MaterialInfo(
                    sprite, chunkLayer, derived.itemRenderType(), packedTint, true, lightEmission);
            material = cached;
        }
        return cached;
    }

    public static class ModelQuadLayerBuilder {
        public final ModelQuadLayer cache = new ModelQuadLayer();
        public final ModelLightMapReader lv;
        public ModelUVReader uvr;

        public ModelQuadLayerBuilder(final TextureAtlasSprite sprite, final int uCoord, final int vCoord) {
            cache.sprite = sprite;
            lv = new ModelLightMapReader();
            uvr = new ModelUVReader(sprite, uCoord, vCoord);
        }

        public void setSourceQuad(BakedQuad bakedQuad) {
            cache.sourceQuad = bakedQuad;
        }

        public void setFace(Direction face) {
            cache.face = face;
        }

        public void setTint(int tint) {
            this.cache.tint = tint;
        }

        public ModelQuadLayer build(final int stateId, final int color, final int lightValue) {
            cache.light = Math.max(lightValue, lv.lv);
            cache.uvs = uvr.quadUVs;

            cache.color = cache.tint != -1 ? color : 0xffffffff;

            if (0x00 <= cache.tint && cache.tint <= 0xff) {
                cache.color = 0xffffffff;
                cache.tint = (stateId << 8) | cache.tint;
            } else {
                cache.tint = -1;
            }

            return cache;
        }
    }
}
