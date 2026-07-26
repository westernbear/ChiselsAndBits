package mod.chiselsandbits.utils;

import java.util.Objects;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
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
        String translationKey;

        if (fluid == Fluids.EMPTY) {
            translationKey = "";
        } else if (fluid == Fluids.WATER) {
            translationKey = "block.minecraft.water";
        } else if (fluid == Fluids.LAVA) {
            translationKey = "block.minecraft.lava";
        } else {
            Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
            String key = Util.makeDescriptionId("block", id);
            String translated = I18n.get(key);
            translationKey = translated.equals(key) ? Util.makeDescriptionId("fluid", id) : key;
        }

        return translationKey;
    }

    public static Identifier getRegistryName(Fluid fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid);
    }

    public static int getColor(Fluid fluid) {
        return FluidVariantRendering.getColor(FluidVariant.of(fluid));
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
            final @NotNull Player player,
            final @NotNull InteractionHand hand,
            final @NotNull Storage<FluidVariant> handler) {
        return FluidStorageUtil.interactWithFluidStorage(
                Objects.requireNonNull(handler), Objects.requireNonNull(player), Objects.requireNonNull(hand));
    }
}
