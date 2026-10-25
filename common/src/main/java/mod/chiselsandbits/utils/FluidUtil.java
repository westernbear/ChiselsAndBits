package mod.chiselsandbits.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class FluidUtil {

    private FluidUtil() {}

    public static String getTranslationKey(Fluid fluid) {
        if (fluid == Fluids.EMPTY) {
            return "";
        }
        if (fluid == Fluids.WATER) {
            return "block.minecraft.water";
        }
        if (fluid == Fluids.LAVA) {
            return "block.minecraft.lava";
        }
        Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
        String key = Util.makeDescriptionId("block", id);
        String translated = I18n.get(key);
        return translated.equals(key) ? Util.makeDescriptionId("fluid", id) : key;
    }

    public static TextureAtlasSprite getStillTexture(Fluid fluid) {
        return Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(fluid.defaultFluidState())
                .stillMaterial()
                .sprite();
    }

    public static TextureAtlasSprite getFlowingTexture(Fluid fluid) {
        return Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(fluid.defaultFluidState())
                .flowingMaterial()
                .sprite();
    }
}
