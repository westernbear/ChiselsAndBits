package mod.chiselsandbits.utils;

import mod.chiselsandbits.platform.PlatformFluidUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

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

    public static Identifier getRegistryName(Fluid fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid);
    }

    public static int getColor(Fluid fluid) {
        return PlatformFluidUtil.getColor(fluid);
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

    public static boolean interactWithFluidHandler(
            final @NotNull Player player, final @NotNull InteractionHand hand, final @NotNull Object handler) {
        return PlatformFluidUtil.interactWithFluidHandler(player, hand, handler);
    }
}
