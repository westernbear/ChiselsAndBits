package mod.chiselsandbits.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import mod.chiselsandbits.client.model.loader.LegacyItemModelDelegate;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.render.SmartModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.util.RandomSource;

/**
 * Headless client render smoke, enabled by {@code CHISELSANDBITS_CLIENT_SMOKE}.
 *
 * <p>Asserts the baked model layer, the dynamic geometry extraction and the rendered frame, then exits the client with
 * a status code so CI can gate on it.
 */
public final class NeoForgeClientSmoke {

    private static final String ENV = "CHISELSANDBITS_CLIENT_SMOKE";
    private static final int START_TICK = 80;
    private static final int SCREENSHOT_TICKS = 80;

    private static final List<String> FAILURES = new ArrayList<>();

    private static int ticks;
    private static Set<String> screenshotsBefore = Set.of();
    private static boolean grabbed;

    private NeoForgeClientSmoke() {}

    public static void tick() {
        if (System.getenv(ENV) == null || grabbed && ticks > START_TICK + SCREENSHOT_TICKS) {
            return;
        }

        ticks++;
        if (ticks < START_TICK) {
            return;
        }

        if (!grabbed) {
            verifyStructure();
            screenshotsBefore = screenshotFiles();
            Screenshot.grab(Minecraft.getInstance(), false);
            grabbed = true;
            return;
        }

        final Path fresh = newestScreenshot();
        if (fresh != null && verifyFrame(fresh)) {
            finish();
        } else if (ticks > START_TICK + SCREENSHOT_TICKS) {
            fail("no screenshot was written");
            finish();
        }
    }

    private static void verifyStructure() {
        final Minecraft minecraft = Minecraft.getInstance();
        final BlockStateModel blockModel = minecraft
                .getModelManager()
                .getBlockStateModelSet()
                .get(ModBlocks.CHISELED_BLOCK.get().defaultBlockState());
        final List<BlockStateModelPart> parts = new ArrayList<>();
        blockModel.collectParts(RandomSource.create(0L), parts);
        int quads = 0;
        for (final BlockStateModelPart part : parts) {
            for (final net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                quads += part.getQuads(direction).size();
            }
            quads += part.getQuads(null).size();
        }
        if (quads != 0) {
            fail("chiseled block model should contribute no quads, got " + quads + " ("
                    + blockModel.getClass().getName() + ")");
        }

        final ItemModel itemModel =
                minecraft.getModelManager().getItemModel(SmartModelManager.itemId(ModItems.ITEM_BLOCK_BIT.get()));
        if (!(itemModel instanceof LegacyItemModelDelegate)) {
            fail("bit item model is not wrapped: " + itemModel);
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
