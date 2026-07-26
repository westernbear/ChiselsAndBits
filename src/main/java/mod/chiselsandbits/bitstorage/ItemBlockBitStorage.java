package mod.chiselsandbits.bitstorage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mod.chiselsandbits.components.FluidStorageData;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.registry.ModDataComponents;
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
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
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
            final TooltipContext context,
            final TooltipDisplay display,
            final Consumer<Component> tooltip,
            final TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        final FluidVariant fluid = getFluidVariant(stack);
        final long amount = getFluidAmount(stack);
        final List<Component> helpText = new ArrayList<>();

        if (fluid.isBlank() || amount <= 0) {
            ChiselsAndBits.getConfig().getCommon().helpText(LocalStrings.HelpBitTankEmpty, helpText);
        } else {
            ChiselsAndBits.getConfig()
                    .getCommon()
                    .helpText(
                            LocalStrings.HelpBitTankFilled,
                            helpText,
                            FluidVariantAttributes.getName(fluid).getString(),
                            String.valueOf(TileEntityBitStorage.dropletsToBits(amount)));
        }
        helpText.forEach(tooltip);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(
            final BlockPos pos,
            final Level worldIn,
            @Nullable final Player player,
            final ItemStack stack,
            final BlockState state) {
        super.updateCustomBlockEntityTag(pos, worldIn, player, stack, state);
        if (worldIn.isClientSide()) {
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
        return getFluidStorageData(stack).variant();
    }

    public static long getFluidAmount(final ItemStack stack) {
        return getFluidStorageData(stack).amount();
    }

    private static FluidStorageData getFluidStorageData(final ItemStack stack) {
        final FluidStorageData component = stack.get(ModDataComponents.FLUID_STORAGE);
        if (component != null) {
            return component;
        }

        final CompoundTag root = ModUtil.getTagCompound(stack);
        final CompoundTag fluidNbt = root.getCompoundOrEmpty(FLUID_NBT_KEY);
        FluidVariant variant = FluidVariant.blank();
        if (fluidNbt.contains("Variant")) {
            variant = FluidVariant.CODEC
                    .parse(NbtOps.INSTANCE, fluidNbt.get("Variant"))
                    .result()
                    .orElse(FluidVariant.blank());
        } else if (fluidNbt.contains("FluidName")) {
            final Fluid fluid = BuiltInRegistries.FLUID.getValue(
                    Identifier.parse(fluidNbt.getStringOr("FluidName", "minecraft:empty")));
            if (fluid != Fluids.EMPTY) {
                variant = FluidVariant.of(fluid);
            }
        }
        final long rawAmount = fluidNbt.getLongOr("Amount", 0L);
        final long amount = fluidNbt.contains("FluidName") ? rawAmount * 81L : rawAmount;
        final FluidStorageData migrated = new FluidStorageData(variant, amount);
        if (!variant.isBlank() && amount > 0) {
            stack.set(ModDataComponents.FLUID_STORAGE, migrated);
            root.remove(FLUID_NBT_KEY);
            ModUtil.setTagCompound(stack, root);
        }
        return migrated;
    }

    public static void setFluid(final ItemStack stack, final FluidVariant fluid, final long amount) {
        final long clamped = Math.max(0, Math.min(FluidConstants.BUCKET, amount));
        final CompoundTag root = ModUtil.getTagCompound(stack);
        if (fluid.isBlank() || clamped == 0) {
            stack.remove(ModDataComponents.FLUID_STORAGE);
            root.remove(FLUID_NBT_KEY);
            ModUtil.setTagCompound(stack, root);
            return;
        }

        stack.set(ModDataComponents.FLUID_STORAGE, new FluidStorageData(fluid, clamped));
        root.remove(FLUID_NBT_KEY);
        ModUtil.setTagCompound(stack, root);
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
