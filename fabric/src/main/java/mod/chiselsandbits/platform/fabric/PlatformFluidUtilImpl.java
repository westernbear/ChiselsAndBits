package mod.chiselsandbits.platform.fabric;

import mod.chiselsandbits.bitstorage.FabricBitStorageStorage;
import mod.chiselsandbits.bitstorage.TileEntityBitStorage;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;

@SuppressWarnings("unused")
public final class PlatformFluidUtilImpl {

    private PlatformFluidUtilImpl() {}

    public static int getColor(Fluid fluid) {
        return FluidVariantRendering.getColor(FluidVariant.of(fluid));
    }

    public static boolean interactWithBitStorage(Player player, InteractionHand hand, TileEntityBitStorage storage) {
        return FluidStorageUtil.interactWithFluidStorage(FabricBitStorageStorage.fluidStorage(storage), player, hand);
    }
}
