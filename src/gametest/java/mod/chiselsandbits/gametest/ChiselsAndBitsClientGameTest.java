package mod.chiselsandbits.gametest;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import mod.chiselsandbits.api.IChiselAndBitsAPI;
import mod.chiselsandbits.api.ModKeyBinding;
import mod.chiselsandbits.bitbag.BagGui;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.model.baked.LegacyBakedModel;
import mod.chiselsandbits.client.model.data.ModelDataMap;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.items.ItemChiseledBit;
import mod.chiselsandbits.printer.ChiselPrinterScreen;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.render.SmartModelManager;
import mod.chiselsandbits.render.chiseledblock.ChiselRenderType;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockSmartModel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class ChiselsAndBitsClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(final ClientGameTestContext context) {
        context.runOnClient(client -> {
            final IChiselAndBitsAPI api = IChiselAndBitsAPI.getInstance();
            if (api != ChiselsAndBits.getApi()) {
                throw new AssertionError("API singleton accessor returned another instance");
            }
            for (final ModKeyBinding binding : ModKeyBinding.values()) {
                if (api.getKeyBinding(binding) == null) {
                    throw new AssertionError("missing key binding: " + binding);
                }
            }
            // Options only lists Fabric-registered KeyMappings. Creating KeyMapping objects without
            // KeyMappingHelper.registerKeyMapping leaves the Controls category empty.
            final Set<String> optionNames = new HashSet<>();
            for (final KeyMapping mapping : client.options.keyMappings) {
                optionNames.add(mapping.getName());
            }
            for (final String expected : new String[] {
                "mod.chiselsandbits.other.mode",
                "mod.chiselsandbits.other.rotate.ccw",
                "mod.chiselsandbits.other.rotate.cw",
                "mod.chiselsandbits.other.pickbit",
                "mod.chiselsandbits.other.offgrid",
                "mod.chiselsandbits.other.undo",
                "mod.chiselsandbits.other.redo",
                "mod.chiselsandbits.other.add_to_clipboard",
                "mod.chiselsandbits.chiselmode.single"
            }) {
                if (!optionNames.contains(expected)) {
                    throw new AssertionError("key binding not registered in Options: " + expected);
                }
            }
            ChiselsAndBits.getConfig().getClient().onUpdateClient();
            ChiselsAndBits.getConfig().getServer().onSyncClient();
        });

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
            context.runOnClient(client -> {
                checkCachedModelDataReuse(client);
                checkDensePaletteModel(client);
                checkBitItemGuiModel(client);
            });

            final int stoneBitState = ModUtil.getStateId(Blocks.STONE.defaultBlockState());
            singleplayer
                    .getServer()
                    .runCommand(
                            "item replace entity @a[limit=1] hotbar.1 with chiselsandbits:block_bit[chiselsandbits:bit_state=%d]"
                                    .formatted(stoneBitState));
            context.waitTicks(5);
            context.runOnClient(client -> {
                client.player.getInventory().setSelectedSlot(1);
                final ItemStack hotbarBit = client.player.getInventory().getItem(1);
                if (!hotbarBit.is(ModItems.ITEM_BLOCK_BIT.get())) {
                    throw new AssertionError("hotbar slot 1 did not receive a block bit");
                }
                checkBitItemGuiModel(client, hotbarBit);
            });
            context.takeScreenshot("chiselsandbits_stone_bit_hotbar");

            singleplayer.getServer().runCommand("item replace entity @a[limit=1] hotbar.0 with chiselsandbits:bit_bag");
            context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
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

    private static void checkCachedModelDataReuse(final net.minecraft.client.Minecraft client) {
        final Map<ChiselRenderType, LegacyBakedModel> cachedModels = Map.of();
        final ModelDataMap modelData = new ModelDataMap();
        modelData.setData(TileEntityBlockChiseled.MODEL_PROP, cachedModels);
        modelData.setData(TileEntityBlockChiseled.MODEL_UPDATE, false);

        new ChiseledBlockSmartModel()
                .updateModelData(client.level, BlockPos.ZERO, Blocks.STONE.defaultBlockState(), modelData);
        if (modelData.getData(TileEntityBlockChiseled.MODEL_PROP) != cachedModels) {
            throw new AssertionError("unchanged model data was rebuilt");
        }
    }

    private static void checkDensePaletteModel(final net.minecraft.client.Minecraft client) {
        final int[] states = {
            ModUtil.getStateId(Blocks.STONE.defaultBlockState()),
            ModUtil.getStateId(Blocks.DIRT.defaultBlockState()),
            ModUtil.getStateId(Blocks.COBBLESTONE.defaultBlockState()),
            ModUtil.getStateId(Blocks.OAK_PLANKS.defaultBlockState())
        };
        final VoxelBlob blob = new VoxelBlob();
        for (int z = 0; z < VoxelBlob.dim; z++) {
            for (int y = 0; y < VoxelBlob.dim; y++) {
                for (int x = 0; x < VoxelBlob.dim; x++) {
                    blob.set(x, y, z, states[(x / 4 + y / 4 + z / 4) & 3]);
                }
            }
        }

        if (ChiseledBlockSmartModel.getCachedModel(
                        states[0], blob, ChiselRenderType.SOLID, DefaultVertexFormat.BLOCK, RandomSource.create())
                .isEmpty()) {
            throw new AssertionError("dense four-state model produced no quads");
        }
    }

    private static void checkBitItemGuiModel(final net.minecraft.client.Minecraft client) {
        final int stoneId = ModUtil.getStateId(Blocks.STONE.defaultBlockState());
        checkBitItemGuiModel(client, ItemChiseledBit.createStack(stoneId, 1, true));

        final ItemStack grassBit =
                ItemChiseledBit.createStack(ModUtil.getStateId(Blocks.GRASS_BLOCK.defaultBlockState()), 1, true);
        checkBitItemGuiModel(client, grassBit);
    }

    private static void checkBitItemGuiModel(final net.minecraft.client.Minecraft client, final ItemStack bit) {
        final LegacyBakedModel resolved =
                SmartModelManager.getInstance().resolveLegacyItemModel(bit, client.level, client.player);
        if (resolved == null
                || resolved.getQuads(null, null, RandomSource.create()).isEmpty()) {
            throw new AssertionError("bit legacy model produced no GUI quads for " + bit);
        }

        final ItemStackRenderState renderState = new ItemStackRenderState();
        client.getItemModelResolver()
                .updateForTopItem(renderState, bit, ItemDisplayContext.GUI, client.level, client.player, 0);
        if (renderState.isEmpty()) {
            throw new AssertionError("bit ItemStackRenderState is empty in GUI for " + bit);
        }

        final AtomicInteger extentCount = new AtomicInteger();
        renderState.visitExtents(extent -> extentCount.incrementAndGet());
        if (extentCount.get() == 0) {
            throw new AssertionError("bit GUI model has no extents for " + bit);
        }

        final AABB bounds = renderState.getModelBoundingBox();
        if (bounds.getXsize() <= 0 || bounds.getYsize() <= 0 || bounds.getZsize() <= 0) {
            throw new AssertionError("bit GUI model has degenerate bounds for " + bit + ": " + bounds);
        }
        // GuiItemAtlas centers items around the slot; geometry must stay near the origin
        // after ItemTransform centering. Corner-pivoted transforms push bits outside the
        // 16x16 atlas scissor and show up as empty inventory icons (#1).
        if (Math.abs(bounds.getCenter().x) > 0.75
                || Math.abs(bounds.getCenter().y) > 0.75
                || Math.abs(bounds.getCenter().z) > 0.75) {
            throw new AssertionError("bit GUI model is not centered for atlas blit: " + bounds);
        }
    }

    private static String command(final String format, final BlockPos position) {
        return format.formatted(position.getX() + " " + position.getY() + " " + position.getZ());
    }
}
