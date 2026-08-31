package mod.chiselsandbits.core;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.utils.Env;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import mod.chiselsandbits.api.IChiselAndBitsAPI;
import mod.chiselsandbits.chiseledblock.BlockBitInfo;
import mod.chiselsandbits.chiseledblock.data.VoxelBlob;
import mod.chiselsandbits.chiseledblock.data.VoxelShapeCache;
import mod.chiselsandbits.client.CreativeClipboardTab;
import mod.chiselsandbits.client.UndoTracker;
import mod.chiselsandbits.config.Configuration;
import mod.chiselsandbits.core.api.ChiselAndBitsAPI;
import mod.chiselsandbits.events.EventPlayerInteract;
import mod.chiselsandbits.events.TickHandler;
import mod.chiselsandbits.events.VaporizeWater;
import mod.chiselsandbits.interfaces.ICacheClearable;
import mod.chiselsandbits.network.NetworkChannel;
import mod.chiselsandbits.platform.PlatformPickBlock;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModContainerTypes;
import mod.chiselsandbits.registry.ModDataComponents;
import mod.chiselsandbits.registry.ModItemGroups;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.registry.ModRecipeSerializers;
import mod.chiselsandbits.registry.ModTileEntityTypes;
import mod.chiselsandbits.render.chiseledblock.ChiseledBlockSmartModel;
import mod.chiselsandbits.utils.Constants;
import mod.chiselsandbits.utils.EnvExecutor;
import mod.chiselsandbits.utils.LanguageHandler;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public class ChiselsAndBits {
    public static final @NotNull String MODID = Constants.MOD_ID;
    private static final IChiselAndBitsAPI api = new ChiselAndBitsAPI();
    private static ChiselsAndBits instance;
    private final NetworkChannel networkChannel = new NetworkChannel();
    List<ICacheClearable> cacheClearables = new ArrayList<>();
    private Configuration config;

    public static void init() {
        if (instance != null) {
            return;
        }
        instance = new ChiselsAndBits();
    }

    private ChiselsAndBits() {
        instance = this;
        config = new Configuration();
        EnvExecutor.runWhenOn(
                Env.CLIENT,
                () -> () -> ClientLifecycleEvent.CLIENT_STARTED.register(
                        client -> LanguageHandler.loadLangPath("assets/chiselsandbits/lang/%s.json")));

        VaporizeWater.register();
        EventPlayerInteract.register();
        PlatformPickBlock.register();
        EnvExecutor.runWhenOn(Env.CLIENT, () -> () -> TickHandler.register());
        ModDataComponents.onModConstruction();
        ModBlocks.onModConstruction();
        ModContainerTypes.onModConstruction();
        ModItems.onModConstruction();
        ModRecipeSerializers.onModConstruction();
        ModTileEntityTypes.onModConstruction();
        ModItemGroups.onModConstruction();
        networkChannel.registerCommonMessages();
        EnvExecutor.runWhenOn(
                Env.CLIENT,
                () -> () -> setupClipboard(new File(Minecraft.getInstance().gameDirectory, MODID + "_clipboard")));
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

    public static void onServerConfigurationChanged() {
        BlockBitInfo.recalculate();
        VoxelBlob.clearServerCache();
        VoxelShapeCache.onConfigurationReload();
    }

    public static void onClientConfigurationChanged() {
        BlockBitInfo.recalculate();
        instance.clearCache();
        VoxelShapeCache.onConfigurationReload();
        ChiseledBlockSmartModel.onConfigurationReload();
    }

    private void setupClipboard(File file) {
        CreativeClipboardTab.getInstance()
                .load(file.toPath().resolve(MODID + "_clipboard.bin").toFile());
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
