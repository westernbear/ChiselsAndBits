package mod.chiselsandbits.compat.client;

import net.minecraft.client.renderer.texture.TextureAtlas;

public interface TextureStitchCallback {
    ClientEvent<Post> POST = ClientEvent.arrayBacked(Post.class, callbacks -> atlas -> {
        for (Post e : callbacks) {
            e.stitch(atlas);
        }
    });

    interface Post {
        void stitch(TextureAtlas atlas);
    }
}
