package mod.chiselsandbits.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import mod.chiselsandbits.chiseledblock.TileEntityBlockChiseled;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.model.ChiseledBlockStateModel;
import mod.chiselsandbits.client.model.loader.LegacyItemModelDelegate;
import mod.chiselsandbits.helpers.ModUtil;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.render.SmartModelManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.io.FileUtils;

/**
 * Headless client render smoke, enabled by {@code CHISELSANDBITS_CLIENT_SMOKE}.
 *
 * <p>Asserts the baked model layer, loads a fresh creative world, meshes chiseled blocks through the chunk model and
 * checks the rendered frame, then exits the client with a status code so CI can gate on it.
 */
public final class NeoForgeClientSmoke {

    private static final String ENV = "CHISELSANDBITS_CLIENT_SMOKE";
    private static final int START_TICK = 80;
    private static final int SCREENSHOT_TICKS = 80;
    private static final int SMOKE_TINT = 0x00ff00;

    private static final List<String> FAILURES = new ArrayList<>();

    private static final int WORLD_TICKS = 2400;
    private static final int SETTLE_TICKS = 200;
    private static final String WORLD = "chiselsandbits-smoke";

    private static int ticks;
    private static Set<String> screenshotsBefore = Set.of();
    private static boolean worldRequested;
    private static int worldReadyAt;
    private static int grabbedAt;

    private NeoForgeClientSmoke() {}

    public static void tick() {
        if (System.getenv(ENV) == null) {
            return;
        }

        ticks++;
        if (ticks < START_TICK) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (!worldRequested) {
            verifyStructure();
            worldRequested = true;
            createWorld(minecraft);
            return;
        }

        if (grabbedAt == 0) {
            if (minecraft.level == null || minecraft.player == null || minecraft.gui.screen() != null) {
                if (ticks > START_TICK + WORLD_TICKS) {
                    fail("the smoke world did not load");
                    finish();
                }
                return;
            }
            // The terrain loading view outlives the player's arrival by a few seconds.
            if (worldReadyAt == 0) {
                worldReadyAt = ticks;
            }
            if (ticks < worldReadyAt + SETTLE_TICKS) {
                return;
            }

            // Bit colours need item components, which are bound once a world is loaded.
            verifyChunkBaking(minecraft
                    .getModelManager()
                    .getBlockStateModelSet()
                    .get(ModBlocks.CHISELED_BLOCK.get().defaultBlockState()));
            screenshotsBefore = screenshotFiles();
            Screenshot.grab(minecraft, false);
            grabbedAt = ticks;
            return;
        }

        final Path fresh = newestScreenshot();
        if (fresh != null && verifyFrame(fresh)) {
            finish();
        } else if (ticks > grabbedAt + SCREENSHOT_TICKS) {
            fail("no screenshot was written");
            finish();
        }
    }

    private static void createWorld(final Minecraft minecraft) {
        try {
            FileUtils.deleteDirectory(
                    minecraft.getLevelSource().getBaseDir().resolve(WORLD).toFile());
        } catch (final IOException e) {
            fail("could not clear the previous smoke world: " + e);
        }
        minecraft
                .createWorldOpenFlows()
                .createFreshLevel(
                        WORLD,
                        new LevelSettings(
                                WORLD,
                                GameType.CREATIVE,
                                LevelSettings.DifficultySettings.DEFAULT,
                                false,
                                WorldDataConfiguration.DEFAULT),
                        new WorldOptions(0L, false, false),
                        WorldPresets::createNormalWorldDimensions,
                        new TitleScreen());
    }

    private static void verifyStructure() {
        verifyKeyMappingAccessor();

        final Minecraft minecraft = Minecraft.getInstance();
        final BlockStateModel blockModel = minecraft
                .getModelManager()
                .getBlockStateModelSet()
                .get(ModBlocks.CHISELED_BLOCK.get().defaultBlockState());
        if (!(blockModel instanceof ChiseledBlockStateModel)) {
            fail("chiseled block is not chunk-baked: " + blockModel.getClass().getName());
        }
        final List<BlockStateModelPart> parts = new ArrayList<>();
        blockModel.collectParts(RandomSource.create(0L), parts);
        int quads = 0;
        for (final BlockStateModelPart part : parts) {
            for (final Direction direction : Direction.values()) {
                quads += part.getQuads(direction).size();
            }
            quads += part.getQuads(null).size();
        }
        if (quads != 0) {
            fail("chiseled block model should contribute no quads without level context, got " + quads + " ("
                    + blockModel.getClass().getName() + ")");
        }

        final ItemModel itemModel =
                minecraft.getModelManager().getItemModel(SmartModelManager.itemId(ModItems.ITEM_BLOCK_BIT.get()));
        if (!(itemModel instanceof LegacyItemModelDelegate)) {
            fail("bit item model is not wrapped: " + itemModel);
        }
    }

    /**
     * Meshes chiseled block entities through the chunk model against a stub level: every side of a full block is
     * drawn and culled per side, a full chiseled neighbour hides the shared side, and bit tints are baked in.
     */
    private static void verifyChunkBaking(final BlockStateModel blockModel) {
        if (!(blockModel instanceof ChiseledBlockStateModel)) {
            return; // already reported by verifyStructure
        }
        final BlockState chiseled = ModBlocks.CHISELED_BLOCK.get().defaultBlockState();
        final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
        final BlockAndTintGetter level = new BlockAndTintGetter() {
            @Override
            public CardinalLighting cardinalLighting() {
                return CardinalLighting.DEFAULT;
            }

            @Override
            public LevelLightEngine getLightEngine() {
                return null; // collectParts never reads light
            }

            @Override
            public int getBlockTint(final BlockPos pos, final ColorResolver resolver) {
                return SMOKE_TINT;
            }

            @Override
            public BlockEntity getBlockEntity(final BlockPos pos) {
                return blockEntities.get(pos);
            }

            @Override
            public BlockState getBlockState(final BlockPos pos) {
                return blockEntities.containsKey(pos) ? chiseled : Blocks.AIR.defaultBlockState();
            }

            @Override
            public FluidState getFluidState(final BlockPos pos) {
                return Fluids.EMPTY.defaultFluidState();
            }

            @Override
            public int getHeight() {
                return 256;
            }

            @Override
            public int getMinY() {
                return 0;
            }
        };

        blockEntities.put(BlockPos.ZERO, chiseledOf(chiseled, BlockPos.ZERO, Blocks.STONE));
        List<List<BakedQuad>> sides = sides(blockModel, level, chiseled);
        for (final Direction direction : Direction.values()) {
            if (sides.get(direction.get3DDataValue()).isEmpty()) {
                fail("full chiseled stone has no " + direction + " quads");
            }
        }
        if (!sides.get(6).isEmpty()) {
            fail("full chiseled stone has unculled quads: " + sides.get(6).size());
        }

        blockEntities.put(BlockPos.ZERO.east(), chiseledOf(chiseled, BlockPos.ZERO.east(), Blocks.STONE));
        sides = sides(blockModel, level, chiseled);
        if (!sides.get(Direction.EAST.get3DDataValue()).isEmpty()
                || sides.get(Direction.WEST.get3DDataValue()).isEmpty()) {
            fail("an opaque chiseled neighbour must hide only the shared side");
        }

        blockEntities.clear();
        blockEntities.put(BlockPos.ZERO, chiseledOf(chiseled, BlockPos.ZERO, Blocks.GRASS_BLOCK));
        boolean tinted = false;
        for (final List<BakedQuad> quads : sides(blockModel, level, chiseled)) {
            for (final BakedQuad quad : quads) {
                if (quad.materialInfo().tintIndex() >= 0) {
                    fail("a packed bit tint reached the chunk renderer: "
                            + quad.materialInfo().tintIndex());
                }
                tinted |= quad.bakedColors().color(0) == (0xff000000 | SMOKE_TINT);
            }
        }
        if (!tinted) {
            fail("chiseled grass was not tinted with the level colour");
        }
    }

    private static TileEntityBlockChiseled chiseledOf(final BlockState chiseled, final BlockPos pos, final Block bits) {
        final TileEntityBlockChiseled entity = new TileEntityBlockChiseled(pos, chiseled);
        final VoxelBlob blob = new VoxelBlob();
        blob.fill(ModUtil.getStateId(bits.defaultBlockState()));
        entity.setBlob(blob);
        return entity;
    }

    /** Quads per {@code Direction#get3DDataValue}, then the unculled ones at index 6. */
    private static List<List<BakedQuad>> sides(
            final BlockStateModel model, final BlockAndTintGetter level, final BlockState state) {
        final List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(level, BlockPos.ZERO, state, RandomSource.create(0L), parts);
        final List<List<BakedQuad>> sides = new ArrayList<>();
        for (int side = 0; side <= 6; side++) {
            final Direction direction = side < 6 ? Direction.from3DDataValue(side) : null;
            final List<BakedQuad> quads = new ArrayList<>();
            for (final BlockStateModelPart part : parts) {
                quads.addAll(part.getQuads(direction));
            }
            sides.add(quads);
        }
        return sides;
    }

    /** Guards the shared {@code KeyMappingAccessor} mixin, which the radial menu casts to. */
    private static void verifyKeyMappingAccessor() {
        final List<KeyMapping> mappings = KeyBindings.created();
        if (mappings.isEmpty()) {
            fail("no key mappings were created");
            return;
        }
        try {
            KeyBindings.boundKeyOf(mappings.get(0));
        } catch (final ClassCastException error) {
            fail("KeyMappingAccessor mixin is not applied: " + error.getMessage());
        }
    }

    private static boolean verifyFrame(final Path screenshot) {
        try {
            if (Files.size(screenshot) < 1024) {
                return false;
            }
            final BufferedImage image = ImageIO.read(screenshot.toFile());
            if (image == null || image.getWidth() < 64 || image.getHeight() < 64) {
                return false;
            }

            final Set<Integer> colors = new HashSet<>();
            for (int y = 0; y < image.getHeight(); y += 8) {
                for (int x = 0; x < image.getWidth(); x += 8) {
                    colors.add(image.getRGB(x, y) & 0xF0F0F0);
                }
            }
            if (colors.size() < 8) {
                fail("rendered frame looks blank (" + colors.size() + " coarse colors)");
            }
            return true;
        } catch (final IOException error) {
            return false;
        }
    }

    private static Set<String> screenshotFiles() {
        final Set<String> names = new HashSet<>();
        try (Stream<Path> stream = Files.list(screenshotDir())) {
            stream.forEach(path -> names.add(path.getFileName().toString()));
        } catch (final IOException ignored) {
            // Directory is created by the first screenshot.
        }
        return names;
    }

    private static Path newestScreenshot() {
        try (Stream<Path> stream = Files.list(screenshotDir())) {
            return stream.filter(path ->
                            !screenshotsBefore.contains(path.getFileName().toString()))
                    .findFirst()
                    .orElse(null);
        } catch (final IOException ignored) {
            return null;
        }
    }

    private static Path screenshotDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR);
    }

    private static void fail(final String message) {
        FAILURES.add(message);
    }

    private static void finish() {
        if (FAILURES.isEmpty()) {
            System.out.println("[chiselsandbits] client render smoke passed");
            System.exit(0);
        }

        FAILURES.forEach(failure -> System.err.println("[chiselsandbits] client render smoke failed: " + failure));
        System.exit(1);
    }
}
