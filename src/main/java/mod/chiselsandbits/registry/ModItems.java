package mod.chiselsandbits.registry;

import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import mod.chiselsandbits.chiseledblock.ItemBlockChiseled;
import mod.chiselsandbits.items.ItemBitBag;
import mod.chiselsandbits.items.ItemBitSaw;
import mod.chiselsandbits.items.ItemChisel;
import mod.chiselsandbits.items.ItemChiseledBit;
import mod.chiselsandbits.items.ItemMagnifyingGlass;
import mod.chiselsandbits.items.ItemMirrorPrint;
import mod.chiselsandbits.items.ItemNegativePrint;
import mod.chiselsandbits.items.ItemPositivePrint;
import mod.chiselsandbits.items.ItemTapeMeasure;
import mod.chiselsandbits.items.ItemWrench;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public final class ModItems {

    public static final Supplier<ItemBlockChiseled> ITEM_CHISELED_BLOCK = Suppliers.memoize(
            () -> new ItemBlockChiseled(ModBlocks.CHISELED_BLOCK.get(), properties("chiseled_block")));
    public static final Supplier<ItemChisel> ITEM_CHISEL_STONE =
            Suppliers.memoize(() -> new ItemChisel(ToolMaterial.STONE, properties("chisel_stone")));
    public static final Supplier<ItemChisel> ITEM_CHISEL_IRON =
            Suppliers.memoize(() -> new ItemChisel(ToolMaterial.IRON, properties("chisel_iron")));
    public static final Supplier<ItemChisel> ITEM_CHISEL_GOLD =
            Suppliers.memoize(() -> new ItemChisel(ToolMaterial.GOLD, properties("chisel_gold")));
    public static final Supplier<ItemChisel> ITEM_CHISEL_DIAMOND =
            Suppliers.memoize(() -> new ItemChisel(ToolMaterial.DIAMOND, properties("chisel_diamond")));
    public static final Supplier<ItemChisel> ITEM_CHISEL_NETHERITE = Suppliers.memoize(() -> new ItemChisel(
            ToolMaterial.NETHERITE, properties("chisel_netherite").fireResistant()));
    public static final Supplier<ItemChiseledBit> ITEM_BLOCK_BIT =
            Suppliers.memoize(() -> new ItemChiseledBit(properties("block_bit")));
    public static final Supplier<ItemMirrorPrint> ITEM_MIRROR_PRINT =
            Suppliers.memoize(() -> new ItemMirrorPrint(properties("mirrorprint")));
    public static final Supplier<ItemMirrorPrint> ITEM_MIRROR_PRINT_WRITTEN =
            Suppliers.memoize(() -> new ItemMirrorPrint(properties("mirrorprint_written")));
    public static final Supplier<ItemPositivePrint> ITEM_POSITIVE_PRINT =
            Suppliers.memoize(() -> new ItemPositivePrint(properties("positiveprint")));
    public static final Supplier<ItemPositivePrint> ITEM_POSITIVE_PRINT_WRITTEN =
            Suppliers.memoize(() -> new ItemPositivePrint(properties("positiveprint_written")));
    public static final Supplier<ItemNegativePrint> ITEM_NEGATIVE_PRINT =
            Suppliers.memoize(() -> new ItemNegativePrint(properties("negativeprint")));
    public static final Supplier<ItemNegativePrint> ITEM_NEGATIVE_PRINT_WRITTEN =
            Suppliers.memoize(() -> new ItemNegativePrint(properties("negativeprint_written")));
    public static final Supplier<ItemBitBag> ITEM_BIT_BAG_DEFAULT =
            Suppliers.memoize(() -> new ItemBitBag(properties("bit_bag")));
    public static final Supplier<ItemBitBag> ITEM_BIT_BAG_DYED =
            Suppliers.memoize(() -> new ItemBitBag(properties("bit_bag_dyed")));
    public static final Supplier<ItemWrench> ITEM_WRENCH =
            Suppliers.memoize(() -> new ItemWrench(properties("wrench_wood")));
    public static final Supplier<ItemBitSaw> ITEM_BIT_SAW_STONE =
            Suppliers.memoize(() -> new ItemBitSaw(ToolMaterial.STONE, properties("bitsaw_stone")));
    public static final Supplier<ItemBitSaw> ITEM_BIT_SAW_GOLD =
            Suppliers.memoize(() -> new ItemBitSaw(ToolMaterial.GOLD, properties("bitsaw_gold")));
    public static final Supplier<ItemBitSaw> ITEM_BIT_SAW_IRON =
            Suppliers.memoize(() -> new ItemBitSaw(ToolMaterial.IRON, properties("bitsaw_iron")));
    public static final Supplier<ItemBitSaw> ITEM_BIT_SAW_DIAMOND =
            Suppliers.memoize(() -> new ItemBitSaw(ToolMaterial.DIAMOND, properties("bitsaw_diamond")));
    public static final Supplier<ItemBitSaw> ITEM_BIT_SAW_NETHERITE = Suppliers.memoize(() -> new ItemBitSaw(
            ToolMaterial.NETHERITE, properties("bitsaw_netherite").fireResistant()));
    public static final Supplier<ItemTapeMeasure> ITEM_TAPE_MEASURE =
            Suppliers.memoize(() -> new ItemTapeMeasure(properties("tape_measure")));
    public static final Supplier<ItemMagnifyingGlass> ITEM_MAGNIFYING_GLASS =
            Suppliers.memoize(() -> new ItemMagnifyingGlass(properties("magnifying_glass")));

    private ModItems() {
        throw new IllegalStateException("Tried to initialize: ModItems but this is a Utility class.");
    }

    private static Item.Properties properties(final String name) {
        return new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name)));
    }

    public static void onModConstruction() {
        ITEM_CHISELED_BLOCK.get().registerBlocks(Item.BY_BLOCK, ITEM_CHISELED_BLOCK.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chiseled_block"),
                ITEM_CHISELED_BLOCK.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chisel_stone"),
                ITEM_CHISEL_STONE.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chisel_iron"),
                ITEM_CHISEL_IRON.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chisel_gold"),
                ITEM_CHISEL_GOLD.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chisel_diamond"),
                ITEM_CHISEL_DIAMOND.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chisel_netherite"),
                ITEM_CHISEL_NETHERITE.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "block_bit"),
                ITEM_BLOCK_BIT.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "mirrorprint"),
                ITEM_MIRROR_PRINT.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "mirrorprint_written"),
                ITEM_MIRROR_PRINT_WRITTEN.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "positiveprint"),
                ITEM_POSITIVE_PRINT.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "positiveprint_written"),
                ITEM_POSITIVE_PRINT_WRITTEN.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "negativeprint"),
                ITEM_NEGATIVE_PRINT.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "negativeprint_written"),
                ITEM_NEGATIVE_PRINT_WRITTEN.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bit_bag"),
                ITEM_BIT_BAG_DEFAULT.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bit_bag_dyed"),
                ITEM_BIT_BAG_DYED.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "wrench_wood"),
                ITEM_WRENCH.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bitsaw_stone"),
                ITEM_BIT_SAW_STONE.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bitsaw_gold"),
                ITEM_BIT_SAW_GOLD.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bitsaw_iron"),
                ITEM_BIT_SAW_IRON.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bitsaw_diamond"),
                ITEM_BIT_SAW_DIAMOND.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bitsaw_netherite"),
                ITEM_BIT_SAW_NETHERITE.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "tape_measure"),
                ITEM_TAPE_MEASURE.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "magnifying_glass"),
                ITEM_MAGNIFYING_GLASS.get());
    }
}
