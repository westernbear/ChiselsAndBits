package mod.chiselsandbits.bitstorage;

import java.util.List;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.LocalStrings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class ItemBlockBitStorage extends BlockItem {

    private static final String FLUID_NBT_KEY = "Fluid";

    public ItemBlockBitStorage(final Block block, final Item.Properties builder) {
        super(block, builder);
        FluidStorage.ITEM.registerForItems((stack, context) -> new TankItemFluidStorage(context), this);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(
            final ItemStack stack,
            @Nullable final Level worldIn,
            final List<Component> tooltip,
            final TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        final FluidVariant fluid = getFluidVariant(stack);
        final long amount = getFluidAmount(stack);

        if (fluid.isBlank() || amount <= 0) {
            ChiselsAndBits.getConfig().getCommon().helpText(LocalStrings.HelpBitTankEmpty, tooltip);
        } else {
            ChiselsAndBits.getConfig()
                    .getCommon()
                    .helpText(
                            LocalStrings.HelpBitTankFilled,
                            tooltip,
                            FluidVariantAttributes.getName(fluid).getString(),
                            String.valueOf(TileEntityBitStorage.dropletsToBits(amount)));
        }
    }

    @Override
    protected boolean updateCustomBlockEntityTag(
            final BlockPos pos,
            final Level worldIn,
            @Nullable final Player player,
            final ItemStack stack,
            final BlockState state) {
        super.updateCustomBlockEntityTag(pos, worldIn, player, stack, state);
        if (worldIn.isClientSide) {
            return false;
        }

        final BlockEntity blockEntity = worldIn.getBlockEntity(pos);
        if (!(blockEntity instanceof TileEntityBitStorage bitStorage)) {
            return false;
        }

        bitStorage.setFluid(getFluidVariant(stack), getFluidAmount(stack));
        return true;
    }

    public static FluidVariant getFluidVariant(final ItemStack stack) {
        final CompoundTag fluidNbt = getFluidNbt(stack);
        if (fluidNbt.contains("Variant", Tag.TAG_COMPOUND)) {
            return FluidVariant.fromNbt(fluidNbt.getCompound("Variant"));
        }
        if (fluidNbt.contains("FluidName", Tag.TAG_STRING)) {
            final Fluid fluid = BuiltInRegistries.FLUID.get(new ResourceLocation(fluidNbt.getString("FluidName")));
            if (fluid == Fluids.EMPTY) {
                return FluidVariant.blank();
            }
            return fluidNbt.contains("Tag", Tag.TAG_COMPOUND)
                    ? FluidVariant.of(fluid, fluidNbt.getCompound("Tag"))
                    : FluidVariant.of(fluid);
        }
        return FluidVariant.blank();
    }

    public static long getFluidAmount(final ItemStack stack) {
        final CompoundTag fluidNbt = getFluidNbt(stack);
        final long amount = fluidNbt.getLong("Amount");
        return fluidNbt.contains("FluidName", Tag.TAG_STRING) ? amount * 81L : amount;
    }

    public static void setFluid(final ItemStack stack, final FluidVariant fluid, final long amount) {
        final long clamped = Math.max(0, Math.min(FluidConstants.BUCKET, amount));
        if (fluid.isBlank() || clamped == 0) {
            final CompoundTag root = stack.getTag();
            if (root != null) {
                root.remove(FLUID_NBT_KEY);
                if (root.isEmpty()) {
                    stack.setTag(null);
                }
            }
            return;
        }

        final CompoundTag fluidNbt = new CompoundTag();
        fluidNbt.put("Variant", fluid.toNbt());
        fluidNbt.putLong("Amount", clamped);
        stack.getOrCreateTag().put(FLUID_NBT_KEY, fluidNbt);
    }

    private static CompoundTag getFluidNbt(final ItemStack stack) {
        final CompoundTag root = stack.getTag();
        return root == null ? new CompoundTag() : root.getCompound(FLUID_NBT_KEY);
    }

    private static final class TankItemFluidStorage extends SingleVariantItemStorage<FluidVariant> {

        private TankItemFluidStorage(final ContainerItemContext context) {
            super(context);
        }

        @Override
        protected FluidVariant getBlankResource() {
            return FluidVariant.blank();
        }

        @Override
        protected FluidVariant getResource(final ItemVariant currentVariant) {
            return getFluidVariant(currentVariant.toStack());
        }

        @Override
        protected long getAmount(final ItemVariant currentVariant) {
            return getFluidAmount(currentVariant.toStack());
        }

        @Override
        protected long getCapacity(final FluidVariant variant) {
            return FluidConstants.BUCKET;
        }

        @Override
        protected ItemVariant getUpdatedVariant(
                final ItemVariant currentVariant, final FluidVariant newResource, final long newAmount) {
            final ItemStack updatedStack = currentVariant.toStack();
            setFluid(updatedStack, newResource, newAmount);
            return ItemVariant.of(updatedStack);
        }
    }
}
