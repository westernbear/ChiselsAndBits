package mod.chiselsandbits.platform;

import java.util.Objects;
import mod.chiselsandbits.bitstorage.FabricBitStorageStorage;
import mod.chiselsandbits.bitstorage.TileEntityBitStorage;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;

@SuppressWarnings("unused")
public final class PlatformFluidUtilImpl {

    private PlatformFluidUtilImpl() {}

    public static int getColor(Fluid fluid) {
        return FluidVariantRendering.getColor(FluidVariant.of(fluid));
    }

    @SuppressWarnings("unchecked")
    public static boolean interactWithFluidHandler(Player player, InteractionHand hand, Object handler) {
        return FluidStorageUtil.interactWithFluidStorage(
                (Storage<FluidVariant>) Objects.requireNonNull(handler),
                Objects.requireNonNull(player),
                Objects.requireNonNull(hand));
    }

    public static boolean interactWithBitStorage(Player player, InteractionHand hand, TileEntityBitStorage storage) {
        return FluidStorageUtil.interactWithFluidStorage(FabricBitStorageStorage.fluidStorage(storage), player, hand);
    }
}
