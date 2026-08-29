package mod.chiselsandbits.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField;
import mod.chiselsandbits.utils.Constants;
import net.peanuuutz.tomlkt.Toml;
import net.peanuuutz.tomlkt.TomlElement;
import net.peanuuutz.tomlkt.TomlTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LegacyConfigurationMigrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyConfigurationMigrator.class);

    private LegacyConfigurationMigrator() {}

    static boolean destinationExists(final Path configDir, final String name) {
        return Files.exists(configDir.resolve(Constants.MOD_ID).resolve(name + ".toml"));
    }

    static boolean migrateClient(
            final Path configDir, final boolean destinationExisted, final ClientConfiguration config) {
        return migrateClient(configDir, destinationExisted, config, config::save);
    }

    static boolean migrateClient(
            final Path configDir,
            final boolean destinationExisted,
            final ClientConfiguration config,
            final Runnable save) {
        return migrate(configDir, "client", destinationExisted, save, values -> {
            set(values, "enable-right-click-mode-change", config.enableRightClickModeChange);
            set(values, "invert-bit-bag-fullness", config.invertBitBagFullness);
            set(values, "enable.toolbar.icons", config.enableToolbarIcons);
            set(values, "per-chisel-mode", config.perChiselMode);
            set(values, "chat-mode-notification", config.chatModeNotification);
            set(values, "item-name-mode-display", config.itemNameModeDisplay);
            set(values, "clipboard.add-broken-blocks", config.addBrokenBlocksToCreativeClipboard);
            set(values, "undo.max-count", config.maxUndoLevel);
            set(values, "tape-measure.max-count", config.maxTapeMeasures);
            set(values, "tape-measure.display-in-chat", config.displayMeasuringTapeInChat);
            set(values, "radial.menu.volume", config.radialMenuVolume);
            set(values, "persist-creative-clipboard", config.persistCreativeClipboard);
            set(values, "bit-storage.contents.cache.size", config.bitStorageContentCacheSize);
            set(values, "max-drawn-region.size", config.maxDrawnRegionSize);
            set(values, "lighting.face-lightmap-extraction", config.enableFaceLightmapExtraction);
            set(values, "lighting.use-value", config.useGetLightValue);
            set(values, "vertexformats.custom.disabled", config.disableCustomVertexFormats);
            set(values, "models.cache.size", config.modelCacheSize);
        });
    }

    static boolean migrateCommon(
            final Path configDir, final boolean destinationExisted, final CommonConfiguration config) {
        return migrateCommon(configDir, destinationExisted, config, config::save);
    }

    static boolean migrateCommon(
            final Path configDir,
            final boolean destinationExisted,
            final CommonConfiguration config,
            final Runnable save) {
        return migrate(configDir, "common", destinationExisted, save, values -> {
            set(values, "common.help.enabled", config.enableHelp);
            set(values, "common.performance.collisions.cache.size", config.collisionBoxCacheSize);
        });
    }

    static boolean migrateServer(
            final Path configDir, final boolean destinationExisted, final ServerConfiguration config) {
        return migrateServer(configDir, destinationExisted, config, config::save);
    }

    static boolean migrateServer(
            final Path configDir,
            final boolean destinationExisted,
            final ServerConfiguration config,
            final Runnable save) {
        return migrate(configDir, "server", destinationExisted, save, values -> {
            set(values, "server.troubleshooting.logging.tile-errors", config.logTileErrors);
            set(values, "server.troubleshooting.logging.eligibility-errors", config.logEligibilityErrors);
            set(values, "server.balancing.random-ticking-blocks.blacklisted", config.blackListRandomTickingBlocks);
            set(values, "server.balancing.tools.damage", config.damageTools);
            set(values, "server.balancing.chisel-tool.harvest-check.enabled", config.enableChiselToolHarvestCheck);
            set(values, "server.balancing.chisel-tool.harvest-check.tools", config.enableChiselToolHarvestCheckTools);
            set(values, "server.balancing.tools.harvest-levels.enabled", config.enableToolHarvestLevels);
            set(values, "server.balancing.bits.act-as-light-source", config.enableBitLightSource);
            set(values, "server.balancing.bits.light-percentage", config.bitLightPercentage);
            set(values, "server.balancing.compatibility-mode.enabled", config.compatabilityMode);
            set(values, "server.balancing.bag.stack-size", config.bagStackSize);
            set(values, "server.balancing.chisel-uses.stone", config.stoneChiselUses);
            set(values, "server.balancing.chisel-uses.iron", config.ironChiselUses);
            set(values, "server.balancing.chisel-uses.diamond", config.diamondChiselUses);
            set(values, "server.balancing.chisel-uses.netherite", config.netheriteChiselUses);
            set(values, "server.balancing.chisel-uses.gold", config.goldChiselUses);
            set(values, "server.balancing.wrench-uses", config.wrenchUses);
            set(values, "server.balancing.saw-uses.stone", config.stoneSawUses);
            set(values, "server.balancing.saw-uses.iron", config.ironSawUses);
            set(values, "server.balancing.saw-uses.gold", config.goldSawUses);
            set(values, "server.balancing.saw-uses.diamond", config.diamondSawUses);
            set(values, "server.balancing.saw-uses.netherite", config.netheriteSawUses);
            set(values, "server.balancing.full-block-crafting.enabled", config.fullBlockCrafting);
            set(values, "server.balancing.bag-space.required", config.requireBagSpace);
            set(values, "server.balancing.bag-space.void-excess", config.voidExcessBits);
            set(values, "server.balancing.clipboard.size.creative", config.creativeClipboardSize);
            set(values, "server.balancing.revertible.blocks", config.revertibleBlocks);
            set(values, "server.performance.memory.low-mode.enabled", config.lowMemoryMode);
        });
    }

    private static boolean migrate(
            final Path configDir,
            final String name,
            final boolean destinationExisted,
            final Runnable save,
            final Consumer<Map<String, TomlElement>> importer) {
        final Path legacyFile = configDir.resolve(Constants.MOD_ID + "-" + name + ".toml");
        if (destinationExisted || !Files.isRegularFile(legacyFile)) {
            return false;
        }

        try {
            final Map<String, TomlElement> values = new LinkedHashMap<>();
            flatten("", Toml.Default.parseToTomlTable(Files.readString(legacyFile)), values);
            importer.accept(values);
            save.run();
            LOGGER.info("Imported legacy configuration from {}; the original file was kept", legacyFile);
            return true;
        } catch (final IOException | RuntimeException error) {
            LOGGER.warn("Unable to import legacy configuration from {}; the original file was kept", legacyFile, error);
            return false;
        }
    }

    private static void flatten(final String prefix, final TomlTable table, final Map<String, TomlElement> values) {
        for (final Map.Entry<String, TomlElement> entry : table.entrySet()) {
            final String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof final TomlTable child) {
                flatten(path, child, values);
            } else {
                values.put(path, entry.getValue());
            }
        }
    }

    private static <T> void set(
            final Map<String, TomlElement> values, final String key, final ValidatedField<T> field) {
        final TomlElement element = find(values, key);
        if (element == null) {
            return;
        }

        try {
            final var result = field.deserialize(element, key);
            if (result.isCritical()) {
                LOGGER.warn("Ignoring invalid legacy configuration value for {}", key);
            } else {
                field.validateAndSet(result.get());
            }
        } catch (final RuntimeException error) {
            LOGGER.warn("Ignoring invalid legacy configuration value for {}", key, error);
        }
    }

    private static TomlElement find(final Map<String, TomlElement> values, final String key) {
        final TomlElement exact = values.get(key);
        if (exact != null) {
            return exact;
        }

        TomlElement match = null;
        final String suffix = "." + key;
        for (final Map.Entry<String, TomlElement> entry : values.entrySet()) {
            if (entry.getKey().endsWith(suffix)) {
                if (match != null) {
                    LOGGER.warn("Ignoring ambiguous legacy configuration key {}", key);
                    return null;
                }
                match = entry.getValue();
            }
        }
        return match;
    }
}
