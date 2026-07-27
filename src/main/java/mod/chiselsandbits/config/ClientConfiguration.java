package mod.chiselsandbits.config;

import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedLong;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.resources.Identifier;

/** Mod client configuration. Loaded clientside, not synced. */
public class ClientConfiguration extends Config {
    public ValidatedBoolean enableRightClickModeChange = new ValidatedBoolean(false);
    public ValidatedBoolean invertBitBagFullness = new ValidatedBoolean(false);
    public ValidatedBoolean enableToolbarIcons = new ValidatedBoolean(true);
    public ValidatedBoolean perChiselMode = new ValidatedBoolean(true);
    public ValidatedBoolean chatModeNotification = new ValidatedBoolean(true);
    public ValidatedBoolean itemNameModeDisplay = new ValidatedBoolean(true);
    public ValidatedBoolean addBrokenBlocksToCreativeClipboard = new ValidatedBoolean(false);
    public ValidatedInt maxUndoLevel = new ValidatedInt(32);
    public ValidatedInt maxTapeMeasures = new ValidatedInt(10);
    public ValidatedBoolean displayMeasuringTapeInChat = new ValidatedBoolean(true);
    public ValidatedDouble radialMenuVolume = new ValidatedDouble(0.1, Double.MAX_VALUE, Double.MIN_VALUE);
    public ValidatedLong bitStorageContentCacheSize = new ValidatedLong(100L, Long.MAX_VALUE, 0L);
    public ValidatedDouble maxDrawnRegionSize = new ValidatedDouble(4.0, Double.MAX_VALUE, Double.MIN_VALUE);
    public ValidatedBoolean enableFaceLightmapExtraction = new ValidatedBoolean(true);
    public ValidatedBoolean useGetLightValue = new ValidatedBoolean(true);
    public ValidatedBoolean disableCustomVertexFormats = new ValidatedBoolean(true);
    public ValidatedBoolean persistCreativeClipboard = new ValidatedBoolean(true);
    public ValidatedLong modelCacheSize = new ValidatedLong(1000L, 2000L, 0L);

    public ClientConfiguration() {
        super(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "client"));
    }

    @Override
    public void onUpdateClient() {
        ChiselsAndBits.onClientConfigurationChanged();
    }
}
