package mod.chiselsandbits.registry;

import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import mod.chiselsandbits.bitstorage.BlockBitStorage;
import mod.chiselsandbits.bitstorage.ItemBlockBitStorage;
import mod.chiselsandbits.chiseledblock.BlockChiseled;
import mod.chiselsandbits.chiseledblock.ReflectionHelperBlock;
import mod.chiselsandbits.printer.ChiselPrinterBlock;
import mod.chiselsandbits.printer.ChiselPrinterBlockItem;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {

    public static final Supplier<BlockBitStorage> BIT_STORAGE_BLOCK =
            Suppliers.memoize(() -> new BlockBitStorage(blockProperties("bit_storage")
                    .strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .dynamicShape()
                    .noOcclusion()
                    .isValidSpawn((p_test_1_, p_test_2_, p_test_3_, p_test_4_) -> false)
                    .isRedstoneConductor((p_test_1_, p_test_2_, p_test_3_) -> false)
                    .isSuffocating((p_test_1_, p_test_2_, p_test_3_) -> false)
                    .isViewBlocking((p_test_1_, p_test_2_, p_test_3_) -> false)));

    public static final Supplier<ItemBlockBitStorage> BIT_STORAGE_BLOCK_ITEM =
            Suppliers.memoize(() -> new ItemBlockBitStorage(BIT_STORAGE_BLOCK.get(), itemProperties("bit_storage")));
    public static final Supplier<ChiselPrinterBlock> CHISEL_PRINTER_BLOCK =
            Suppliers.memoize(() -> new ChiselPrinterBlock(blockProperties("chiseled_printer")
                    .strength(1.5f, 6f)
                    .noOcclusion()
                    .isRedstoneConductor((p_test_1_, p_test_2_, p_test_3_) -> false)
                    .isViewBlocking((p_test_1_, p_test_2_, p_test_3_) -> false)));

    public static final Supplier<BlockChiseled> CHISELED_BLOCK = Suppliers.memoize(() -> new BlockChiseled(
            "chiseled_block",
            blockProperties("chiseled_block")
                    .pushReaction(PushReaction.BLOCK)
                    .strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .forceSolidOn()
                    .lightLevel(state -> state.getValue(BlockChiseled.LIGHT_LEVEL))
                    .isViewBlocking((p_test_1_, p_test_2_, p_test_3_) -> false)
                    .isRedstoneConductor((state, level, pos) -> BlockChiseled.isFullCube(state))
                    .noOcclusion()));
    public static final Supplier<BlockItem> CHISEL_PRINTER_ITEM = Suppliers.memoize(
            () -> new ChiselPrinterBlockItem(CHISEL_PRINTER_BLOCK.get(), itemProperties("chiseled_printer")));

    public static final Supplier<ReflectionHelperBlock> REFLECTION_HELPER_BLOCK =
            Suppliers.memoize(() -> new ReflectionHelperBlock(blockProperties("reflection_helper_block")));

    private ModBlocks() {
        throw new IllegalStateException("Tried to initialize: ModBlocks but this is a Utility class.");
    }

    private static BlockBehaviour.Properties blockProperties(final String name) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name)));
    }

    private static Item.Properties itemProperties(final String name) {
        return new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name)));
    }

    public static void onModConstruction() {
        Registry.register(
                BuiltInRegistries.BLOCK,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bit_storage"),
                BIT_STORAGE_BLOCK.get());
        Registry.register(
                BuiltInRegistries.BLOCK,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chiseled_printer"),
                CHISEL_PRINTER_BLOCK.get());
        Registry.register(
                BuiltInRegistries.BLOCK,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chiseled_block"),
                CHISELED_BLOCK.get());
        Registry.register(
                BuiltInRegistries.BLOCK,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "reflection_helper_block"),
                REFLECTION_HELPER_BLOCK.get());

        BIT_STORAGE_BLOCK_ITEM.get().registerBlocks(Item.BY_BLOCK, BIT_STORAGE_BLOCK_ITEM.get());
        CHISEL_PRINTER_ITEM.get().registerBlocks(Item.BY_BLOCK, CHISEL_PRINTER_ITEM.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bit_storage"),
                BIT_STORAGE_BLOCK_ITEM.get());
        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chiseled_printer"),
                CHISEL_PRINTER_ITEM.get());
    }

    public static BlockState getChiseledDefaultState() {
        return CHISELED_BLOCK.get().defaultBlockState();
    }

    public static BlockChiseled getChiseledBlock() {
        return CHISELED_BLOCK.get();
    }
}
