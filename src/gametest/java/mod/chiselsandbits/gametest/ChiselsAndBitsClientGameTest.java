package mod.chiselsandbits.gametest;

import mod.chiselsandbits.bitbag.BagGui;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.printer.ChiselPrinterScreen;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class ChiselsAndBitsClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(final ClientGameTestContext context) {
        final BlockPos target;
        final TestWorldSave worldSave;
        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                .create()) {
            MixinEnvironment.getCurrentEnvironment().audit();
            target = context.computeOnClient(
                    client -> client.player.blockPosition().east(2).above(2));
            worldSave = singleplayer.getWorldSave();

            singleplayer.getServer().runCommand(command("setblock %s minecraft:stone", target));
            singleplayer
                    .getServer()
                    .runCommand("item replace entity @a[limit=1] hotbar.0 with chiselsandbits:chisel_diamond");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            context.waitFor(client -> client.level.getBlockState(target).is(Blocks.STONE)
                    && client.player.getMainHandItem().is(ModItems.ITEM_CHISEL_DIAMOND.get()));
            final BlockBitInfo.SupportsAnalysisResult stoneSupport =
                    BlockBitInfo.doSupportAnalysis(Blocks.STONE.defaultBlockState());
            if (!stoneSupport.isSupported()) {
                throw new AssertionError("stone is not chiselable: " + stoneSupport.getUnsupportedReason());
            }
            context.getInput().lookAt(target);
            context.waitTick();
            context.waitFor(client -> client.hitResult instanceof BlockHitResult hit
                    && hit.getBlockPos().equals(target));
            context.runOnClient(client -> {
                final BlockHitResult hit = (BlockHitResult) client.hitResult;
                client.gameMode.startDestroyBlock(target, hit.getDirection());
            });
            context.waitFor(client -> client.level.getBlockState(target).is(ModBlocks.CHISELED_BLOCK.get()));
            waitForSingleBitRemoved(context, singleplayer, target);
            context.takeScreenshot("chiselsandbits_chiseled_block");

            singleplayer.getServer().runCommand("item replace entity @a[limit=1] hotbar.0 with chiselsandbits:bit_bag");
            context.waitFor(client -> client.player.getMainHandItem().is(ModItems.ITEM_BIT_BAG_DEFAULT.get()));
            context.getInput().lookAt(0, -90);
            context.waitTick();
            context.getInput().pressKey(options -> options.keyUse);
            context.waitForScreen(BagGui.class);
            context.takeScreenshot("chiselsandbits_bit_bag");
            context.setScreen(() -> null);

            final BlockPos printer = target.south(2);
            singleplayer.getServer().runCommand(command("setblock %s chiselsandbits:chiseled_printer", printer));
            context.waitFor(client -> client.level.getBlockState(printer).is(ModBlocks.CHISEL_PRINTER_BLOCK.get()));
            context.getInput().lookAt(printer);
            context.waitTick();
            context.getInput().pressKey(options -> options.keyUse);
            context.waitForScreen(ChiselPrinterScreen.class);
            context.takeScreenshot("chiselsandbits_printer");
            context.setScreen(() -> null);
        }

        try (TestSingleplayerContext singleplayer = worldSave.open()) {
            singleplayer.getClientLevel().waitForChunksRender();
            context.waitFor(client -> client.level.getBlockState(target).is(ModBlocks.CHISELED_BLOCK.get()));
            waitForSingleBitRemoved(context, singleplayer, target);
            context.takeScreenshot("chiselsandbits_reloaded");
        }
    }

    private static void waitForSingleBitRemoved(
            final ClientGameTestContext context, final TestSingleplayerContext singleplayer, final BlockPos target) {
        for (int tick = 0; tick < ClientGameTestContext.DEFAULT_TIMEOUT; tick++) {
            final boolean ready = singleplayer.getServer().computeOnServer(server -> {
                final var blockEntity = server.overworld().getBlockEntity(target);
                return blockEntity instanceof TileEntityBlockChiseled bits
                        && bits.getBlob().filled() == 4095;
            });
            if (ready) {
                return;
            }
            context.waitTick();
        }

        throw new AssertionError("chiseled block did not contain exactly 4095 bits");
    }

    private static String command(final String format, final BlockPos position) {
        return format.formatted(position.getX() + " " + position.getY() + " " + position.getZ());
    }
}
