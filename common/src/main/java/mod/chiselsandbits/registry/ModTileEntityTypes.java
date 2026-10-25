package mod.chiselsandbits.registry;

import com.google.common.base.Suppliers;
import java.util.Set;
import java.util.function.Supplier;
import mod.chiselsandbits.bitstorage.TileEntityBitStorage;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.printer.ChiselPrinterTileEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModTileEntityTypes {

    private ModTileEntityTypes() {
        throw new IllegalStateException("Tried to initialize: ModTileEntityTypes but this is a Utility class.");
    }

    public static void onModConstruction() {
        ModRegistry.add(ModRegistry.BLOCK_ENTITY_TYPES, "chiseled", CHISELED);
        ModRegistry.add(ModRegistry.BLOCK_ENTITY_TYPES, "bit_storage", BIT_STORAGE);
        ModRegistry.add(ModRegistry.BLOCK_ENTITY_TYPES, "chisel_printer", CHISEL_PRINTER);
    }

    public static final Supplier<BlockEntityType<TileEntityBlockChiseled>> CHISELED = Suppliers.memoize(
            () -> new BlockEntityType<>(TileEntityBlockChiseled::new, Set.of(ModBlocks.CHISELED_BLOCK.get())));

    public static final Supplier<BlockEntityType<TileEntityBitStorage>> BIT_STORAGE = Suppliers.memoize(
            () -> new BlockEntityType<>(TileEntityBitStorage::new, Set.of(ModBlocks.BIT_STORAGE_BLOCK.get())));

    public static final Supplier<BlockEntityType<ChiselPrinterTileEntity>> CHISEL_PRINTER = Suppliers.memoize(
            () -> new BlockEntityType<>(ChiselPrinterTileEntity::new, Set.of(ModBlocks.CHISEL_PRINTER_BLOCK.get())));
}
