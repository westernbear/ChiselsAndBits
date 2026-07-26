package mod.chiselsandbits.core;

import fuzs.forgeconfigapiport.fabric.api.forge.v4.ForgeModConfigEvents;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import mod.chiselsandbits.api.IChiselAndBitsAPI;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.client.CreativeClipboardTab;
import mod.chiselsandbits.client.UndoTracker;
import mod.chiselsandbits.config.Configuration;
import mod.chiselsandbits.core.api.ChiselAndBitsAPI;
import mod.chiselsandbits.events.EventPlayerInteract;
import mod.chiselsandbits.events.VaporizeWater;
import mod.chiselsandbits.interfaces.ICacheClearable;
import mod.chiselsandbits.network.NetworkChannel;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModContainerTypes;
import mod.chiselsandbits.registry.ModItemGroups;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.registry.ModRecipeSerializers;
import mod.chiselsandbits.registry.ModTileEntityTypes;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockSmartModel;
import mod.chiselsandbits.utils.Constants;
import mod.chiselsandbits.utils.EnvExecutor;
import mod.chiselsandbits.utils.LanguageHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public class ChiselsAndBits {
    public static final @NotNull String MODID = Constants.MOD_ID;
    private static final IChiselAndBitsAPI api = new ChiselAndBitsAPI();
    private static ChiselsAndBits instance;
    private final NetworkChannel networkChannel = new NetworkChannel();
    List<ICacheClearable> cacheClearables = new ArrayList<>();
    private Configuration config;

    public ChiselsAndBits() {
        instance = this;
        config = new Configuration();
        EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
            ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
                LanguageHandler.loadLangPath(
                        "assets/chiselsandbits/lang/%s.json"); // hotfix config comments, it's ugly bcs it's gonna be
                // replaced
            });
        });

        EnvExecutor.runWhenOn(
                EnvType.CLIENT, () -> () -> ClientLifecycleEvents.CLIENT_STARTED.register(this::clientSetup));
        VaporizeWater.register();
        EventPlayerInteract.register();
        ModBlocks.onModConstruction();
        ModContainerTypes.onModConstruction();
        ModItems.onModConstruction();
        ModRecipeSerializers.onModConstruction();
        ModTileEntityTypes.onModConstruction();
        ModItemGroups.onModConstruction();
        networkChannel.registerCommonMessages();
        EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> {
            setupClipboard(new File(Minecraft.getInstance().gameDirectory, MODID + "_clipboard"));
            ForgeModConfigEvents.loading(Constants.MOD_ID).register(c -> handleIdMapping(Minecraft.getInstance()));
            ForgeModConfigEvents.reloading(Constants.MOD_ID).register(c -> handleIdMapping(Minecraft.getInstance()));
        });
    }

    public static ChiselsAndBits getInstance() {
        return instance;
    }

    public static Configuration getConfig() {
        return instance.config;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public static IChiselAndBitsAPI getApi() {
        return api;
    }

    public static NetworkChannel getNetworkChannel() {
        return instance.networkChannel;
    }

    private void setupClipboard(File file) {
        CreativeClipboardTab.getInstance()
                .load(file.toPath().resolve(MODID + "_clipboard.bin").toFile());
    }

    public void clientSetup(Minecraft inst) {
        EnvExecutor.runWhenOn(
                EnvType.CLIENT,
                () -> () -> ForgeModConfigEvents.reloading(ChiselsAndBits.MODID)
                        .register(ChiseledBlockSmartModel::onConfigurationReload));
    }

    public void handleIdMapping(Minecraft inst) {
        BlockBitInfo.recalculate();
        clearCache();
    }

    public void clearCache() {
        for (final ICacheClearable clearable : cacheClearables) {
            clearable.clearCache();
        }

        addClearable(UndoTracker.getInstance());
        VoxelBlob.clearCache();
    }

    public void addClearable(final ICacheClearable cache) {
        if (!cacheClearables.contains(cache)) {
            cacheClearables.add(cache);
        }
    }
}
