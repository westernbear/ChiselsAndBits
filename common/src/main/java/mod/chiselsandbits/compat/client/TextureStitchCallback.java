package mod.chiselsandbits.compat.client;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.client.renderer.texture.TextureAtlas;

public interface TextureStitchCallback {
    Event<Post> POST = EventFactory.of(callbacks -> atlas -> {
        for (final Post e : callbacks) {
            e.stitch(atlas);
        }
    });

    interface Post {
        void stitch(TextureAtlas atlas);
    }
}
