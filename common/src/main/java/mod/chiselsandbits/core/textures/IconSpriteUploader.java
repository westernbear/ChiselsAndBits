package mod.chiselsandbits.core.textures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/** Provides C&B icon sprites from the 26.2-managed block atlas. */
public class IconSpriteUploader {
    public static final Identifier TEXTURE_MAP_NAME = TextureAtlas.LOCATION_BLOCKS;

    public IconSpriteUploader() {}

    public @NotNull TextureAtlasSprite getSprite(@NotNull Identifier location) {
        return Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(location);
    }
}
