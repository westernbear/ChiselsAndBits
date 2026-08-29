package mod.chiselsandbits.config;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.nio.file.Path;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import mod.chiselsandbits.platform.PlatformHelper;

/** Mod root configuration. */
public class Configuration {
    private final ClientConfiguration clientConfig;
    private final ServerConfiguration serverConfig;
    private final CommonConfiguration commonConfig;

    public Configuration() {
        final Path configDir = PlatformHelper.getConfigDir();

        if (Platform.getEnvironment() == Env.CLIENT) {
            final boolean existed = LegacyConfigurationMigrator.destinationExists(configDir, "client");
            clientConfig = ConfigApiJava.registerAndLoadConfig(ClientConfiguration::new, RegisterType.CLIENT);
            LegacyConfigurationMigrator.migrateClient(configDir, existed, clientConfig);
        } else {
            clientConfig = new ClientConfiguration();
        }

        final boolean commonExisted = LegacyConfigurationMigrator.destinationExists(configDir, "common");
        commonConfig = ConfigApiJava.registerAndLoadConfig(CommonConfiguration::new, RegisterType.BOTH);
        LegacyConfigurationMigrator.migrateCommon(configDir, commonExisted, commonConfig);

        final boolean serverExisted = LegacyConfigurationMigrator.destinationExists(configDir, "server");
        serverConfig = ConfigApiJava.registerAndLoadNoGuiConfig(ServerConfiguration::new, RegisterType.BOTH);
        LegacyConfigurationMigrator.migrateServer(configDir, serverExisted, serverConfig);
    }

    public ClientConfiguration getClient() {
        return clientConfig;
    }

    public ServerConfiguration getServer() {
        return serverConfig;
    }

    public CommonConfiguration getCommon() {
        return commonConfig;
    }
}
