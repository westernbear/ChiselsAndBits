package mod.chiselsandbits.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class LegacyConfigurationMigrationCheck {
    private LegacyConfigurationMigrationCheck() {}

    public static void main(final String[] args) throws Exception {
        final Path configDir = Files.createTempDirectory("chiselsandbits-legacy-config-");
        final Path clientFile = configDir.resolve("chiselsandbits-client.toml");
        final Path commonFile = configDir.resolve("chiselsandbits-common.toml");
        final Path serverFile = configDir.resolve("chiselsandbits-server.toml");

        try {
            Files.writeString(clientFile, """
                    [client.settings]
                    enable-right-click-mode-change = true
                    per-chisel-mode = false
                    persist-creative-clipboard = false

                    [client.settings.enable.toolbar]
                    icons = false

                    [client.settings.undo]
                    max-count = 7

                    [client.settings.radial.menu]
                    volume = 0.75

                    [client.client.performance.bit-storage.contents.cache]
                    size = 321

                    [client.client.performance.models.cache]
                    size = 444
                    """);
            Files.writeString(commonFile, """
                    [common.help.common.help]
                    enabled = false

                    [common.common.performance.common.performance.collisions.cache]
                    size = 42
                    """);
            Files.writeString(serverFile, """
                    [server.troubleshooting.server.troubleshooting.logging]
                    tile-errors = false

                    [server.server.balancing.server.balancing.chisel-tool.harvest-check]
                    tools = "minecraft:diamond_pickaxe"

                    [server.server.balancing.server.balancing.bag]
                    stack-size = 64

                    [server.server.balancing.server.balancing.bits]
                    light-percentage = 12.5

                    [server.server.balancing.server.balancing.revertible]
                    blocks = ["minecraft:stone", "minecraft:dirt"]

                    [server.server.server.performance.server.performance.memory.low-mode]
                    enabled = true
                    """);

            final AtomicInteger saves = new AtomicInteger();
            final ClientConfiguration client = new ClientConfiguration();
            final CommonConfiguration common = new CommonConfiguration();
            final ServerConfiguration server = new ServerConfiguration();

            require(
                    LegacyConfigurationMigrator.migrateClient(configDir, false, client, saves::incrementAndGet),
                    "client config was not migrated");
            require(
                    LegacyConfigurationMigrator.migrateCommon(configDir, false, common, saves::incrementAndGet),
                    "common config was not migrated");
            require(
                    LegacyConfigurationMigrator.migrateServer(configDir, false, server, saves::incrementAndGet),
                    "server config was not migrated");

            require(client.enableRightClickModeChange.get(), "client boolean was not migrated");
            require(!client.perChiselMode.get(), "nested client boolean was not migrated");
            require(!client.enableToolbarIcons.get(), "dotted client key was not migrated");
            require(!client.persistCreativeClipboard.get(), "client clipboard boolean was not migrated");
            require(client.maxUndoLevel.get() == 7, "client integer was not migrated");
            require(client.radialMenuVolume.get() == 0.75, "client double was not migrated");
            require(client.bitStorageContentCacheSize.get() == 321L, "client long was not migrated");
            require(client.modelCacheSize.get() == 444L, "duplicated client section was not migrated");
            require(!common.enableHelp.get(), "common boolean was not migrated");
            require(common.collisionBoxCacheSize.get() == 42L, "common long was not migrated");
            require(!server.logTileErrors.get(), "server troubleshooting boolean was not migrated");
            require(
                    server.enableChiselToolHarvestCheckTools.get().equals("minecraft:diamond_pickaxe"),
                    "server string was not migrated");
            require(server.bagStackSize.get() == 64, "server integer was not migrated");
            require(server.bitLightPercentage.get() == 12.5, "server double was not migrated");
            require(
                    server.revertibleBlocks.get().equals(List.of("minecraft:stone", "minecraft:dirt")),
                    "server string list was not migrated");
            require(server.lowMemoryMode.get(), "duplicated server section was not migrated");
            require(saves.get() == 3, "each migrated config must be saved once");

            client.enableRightClickModeChange.validateAndSet(false);
            require(
                    !LegacyConfigurationMigrator.migrateClient(configDir, true, client, saves::incrementAndGet),
                    "an existing Fzzy config was overwritten");
            require(!client.enableRightClickModeChange.get(), "existing Fzzy value was overwritten");
            require(saves.get() == 3, "skipped migration was saved");
            require(
                    Files.isRegularFile(clientFile)
                            && Files.isRegularFile(commonFile)
                            && Files.isRegularFile(serverFile),
                    "legacy config files were removed");
        } finally {
            Files.deleteIfExists(clientFile);
            Files.deleteIfExists(commonFile);
            Files.deleteIfExists(serverFile);
            Files.deleteIfExists(configDir);
        }
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
