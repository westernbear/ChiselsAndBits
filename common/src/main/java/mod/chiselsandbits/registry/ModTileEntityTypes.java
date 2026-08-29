package mod.chiselsandbits.registry;

import com.google.common.base.Suppliers;
import java.util.Set;
import java.util.function.Supplier;
import mod.chiselsandbits.bitstorage.TileEntityBitStorage;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.printer.ChiselPrinterTileEntity;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModTileEntityTypes {

    private ModTileEntityTypes() {
        throw new IllegalStateException("Tried to initialize: ModTileEntityTypes but this is a Utility class.");
    }

    public static void onModConstruction() {
        Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chiseled"),
                CHISELED.get());
        Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "bit_storage"),
                BIT_STORAGE.get());
        Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chisel_printer"),
                CHISEL_PRINTER.get());
    }

    public static final Supplier<BlockEntityType<TileEntityBlockChiseled>> CHISELED = Suppliers.memoize(
            () -> new BlockEntityType<>(TileEntityBlockChiseled::new, Set.of(ModBlocks.CHISELED_BLOCK.get())));

    public static final Supplier<BlockEntityType<TileEntityBitStorage>> BIT_STORAGE = Suppliers.memoize(
            () -> new BlockEntityType<>(TileEntityBitStorage::new, Set.of(ModBlocks.BIT_STORAGE_BLOCK.get())));

    public static final Supplier<BlockEntityType<ChiselPrinterTileEntity>> CHISEL_PRINTER = Suppliers.memoize(
            () -> new BlockEntityType<>(ChiselPrinterTileEntity::new, Set.of(ModBlocks.CHISEL_PRINTER_BLOCK.get())));
}
