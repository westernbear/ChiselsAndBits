package mod.chiselsandbits.bitstorage;

import dev.architectury.injectables.annotations.Environment;
import dev.architectury.utils.Env;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mod.chiselsandbits.components.FluidStorageData;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.registry.ModDataComponents;
import mod.chiselsandbits.utils.FluidUnits;
import mod.chiselsandbits.utils.FluidUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
    }

    @Environment(Env.CLIENT)
    @Override
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final TooltipDisplay display,
            final Consumer<Component> tooltip,
            final TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        final Fluid fluid = getStoredFluid(stack);
        final long amount = getFluidAmount(stack);
        final List<Component> helpText = new ArrayList<>();

        if (fluid == null || fluid == Fluids.EMPTY || amount <= 0) {
            ChiselsAndBits.getConfig().getCommon().helpText(LocalStrings.HelpBitTankEmpty, helpText);
        } else {
            ChiselsAndBits.getConfig()
                    .getCommon()
                    .helpText(
                            LocalStrings.HelpBitTankFilled,
                            helpText,
                            I18n.get(FluidUtil.getTranslationKey(fluid)),
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

        bitStorage.setFluid(getStoredFluid(stack), getFluidAmount(stack));
        return true;
    }

    @Nullable
    public static Fluid getStoredFluid(final ItemStack stack) {
        final FluidStorageData data = getFluidStorageData(stack);
        return data.isEmpty() ? null : data.fluid();
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
        Identifier fluidId = FluidStorageData.EMPTY_FLUID;
        if (fluidNbt.contains("FluidName")) {
            fluidId = Identifier.parse(fluidNbt.getStringOr("FluidName", "minecraft:empty"));
        }
        final long rawAmount = fluidNbt.getLongOr("Amount", 0L);
        final long amount = fluidNbt.contains("FluidName") ? rawAmount * 81L : rawAmount;
        final FluidStorageData migrated = new FluidStorageData(fluidId, amount);
        if (!migrated.isEmpty()) {
            stack.set(ModDataComponents.FLUID_STORAGE, migrated);
            root.remove(FLUID_NBT_KEY);
            ModUtil.setTagCompound(stack, root);
        }
        return migrated;
    }

    public static void setFluid(final ItemStack stack, @Nullable final Fluid fluid, final long amount) {
        final long clamped = Math.max(0, Math.min(FluidUnits.BUCKET, amount));
        final CompoundTag root = ModUtil.getTagCompound(stack);
        if (fluid == null || fluid == Fluids.EMPTY || clamped == 0) {
            stack.remove(ModDataComponents.FLUID_STORAGE);
            root.remove(FLUID_NBT_KEY);
            ModUtil.setTagCompound(stack, root);
            return;
        }

        stack.set(ModDataComponents.FLUID_STORAGE, FluidStorageData.of(fluid, clamped));
        root.remove(FLUID_NBT_KEY);
        ModUtil.setTagCompound(stack, root);
    }
}
