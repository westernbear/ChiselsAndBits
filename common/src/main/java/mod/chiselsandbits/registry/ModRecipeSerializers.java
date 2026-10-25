package mod.chiselsandbits.registry;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import mod.chiselsandbits.crafting.BagDyeing;
import mod.chiselsandbits.crafting.BitSawCrafting;
import mod.chiselsandbits.crafting.ChiselBlockCrafting;
import mod.chiselsandbits.crafting.ChiselCrafting;
import mod.chiselsandbits.crafting.MirrorTransferCrafting;
import mod.chiselsandbits.crafting.NegativeInversionCrafting;
import mod.chiselsandbits.crafting.StackableCrafting;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ModRecipeSerializers {

    public static final Supplier<RecipeSerializer<BagDyeing>> BAG_DYEING =
            Suppliers.memoize(() -> simple(BagDyeing::new));
    public static final Supplier<RecipeSerializer<ChiselCrafting>> CHISEL_CRAFTING =
            Suppliers.memoize(() -> simple(ChiselCrafting::new));
    public static final Supplier<RecipeSerializer<ChiselBlockCrafting>> CHISEL_BLOCK_CRAFTING =
            Suppliers.memoize(() -> simple(ChiselBlockCrafting::new));
    public static final Supplier<RecipeSerializer<StackableCrafting>> STACKABLE_CRAFTING =
            Suppliers.memoize(() -> simple(StackableCrafting::new));
    public static final Supplier<RecipeSerializer<NegativeInversionCrafting>> NEGATIVE_INVERSION_CRAFTING =
            Suppliers.memoize(() -> simple(NegativeInversionCrafting::new));
    public static final Supplier<RecipeSerializer<MirrorTransferCrafting>> MIRROR_TRANSFER_CRAFTING =
            Suppliers.memoize(() -> simple(MirrorTransferCrafting::new));
    public static final Supplier<RecipeSerializer<BitSawCrafting>> BIT_SAW_CRAFTING =
            Suppliers.memoize(() -> simple(BitSawCrafting::new));

    private ModRecipeSerializers() {
        throw new IllegalStateException("Tried to initialize: ModRecipeSerializers but this is a Utility class.");
    }

    private static <T extends CustomRecipe> RecipeSerializer<T> simple(final Supplier<T> factory) {
        return new RecipeSerializer<>(MapCodec.unit(factory), StreamCodec.unit(factory.get()));
    }

    public static void onModConstruction() {
        ModRegistry.add(ModRegistry.RECIPE_SERIALIZERS, "bag_dyeing", BAG_DYEING);
        ModRegistry.add(ModRegistry.RECIPE_SERIALIZERS, "chisel_crafting", CHISEL_CRAFTING);
        ModRegistry.add(ModRegistry.RECIPE_SERIALIZERS, "chisel_block_crafting", CHISEL_BLOCK_CRAFTING);
        ModRegistry.add(ModRegistry.RECIPE_SERIALIZERS, "stackable_crafting", STACKABLE_CRAFTING);
        ModRegistry.add(ModRegistry.RECIPE_SERIALIZERS, "negative_inversion_crafting", NEGATIVE_INVERSION_CRAFTING);
        ModRegistry.add(ModRegistry.RECIPE_SERIALIZERS, "mirror_transfer_crafting", MIRROR_TRANSFER_CRAFTING);
        ModRegistry.add(ModRegistry.RECIPE_SERIALIZERS, "bit_saw_crafting", BIT_SAW_CRAFTING);
    }
}
