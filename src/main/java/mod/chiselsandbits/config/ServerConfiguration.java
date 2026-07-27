package mod.chiselsandbits.config;

import java.util.List;
import java.util.Objects;
import me.fzzyhmstrs.fzzy_config.api.SaveType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.event.api.ServerUpdateContext;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/** Mod server configuration. Loaded serverside and synced on connection. */
public class ServerConfiguration extends Config {
    public ValidatedBoolean logTileErrors = new ValidatedBoolean(true);
    public ValidatedBoolean logEligibilityErrors = new ValidatedBoolean(true);
    public ValidatedBoolean blackListRandomTickingBlocks = new ValidatedBoolean(false);
    public ValidatedBoolean damageTools = new ValidatedBoolean(true);
    public ValidatedBoolean enableChiselToolHarvestCheck = new ValidatedBoolean(false);
    public ValidatedString enableChiselToolHarvestCheckTools = new ValidatedString("");
    public ValidatedBoolean enableToolHarvestLevels = new ValidatedBoolean(true);
    public ValidatedBoolean enableBitLightSource = new ValidatedBoolean(true);
    public ValidatedDouble bitLightPercentage = new ValidatedDouble(6.25, Double.MAX_VALUE, Double.MIN_VALUE);
    public ValidatedBoolean compatabilityMode = new ValidatedBoolean(false);
    public ValidatedInt bagStackSize = new ValidatedInt(512);
    public ValidatedInt stoneChiselUses = new ValidatedInt(12288);
    public ValidatedInt ironChiselUses = new ValidatedInt(110592);
    public ValidatedInt diamondChiselUses = new ValidatedInt(995328);
    public ValidatedInt netheriteChiselUses = new ValidatedInt(8957952);
    public ValidatedInt goldChiselUses = new ValidatedInt(1024);
    public ValidatedInt wrenchUses = new ValidatedInt(1888);
    public ValidatedInt stoneSawUses = new ValidatedInt(512);
    public ValidatedInt ironSawUses = new ValidatedInt(2048);
    public ValidatedInt goldSawUses = new ValidatedInt(500);
    public ValidatedInt diamondSawUses = new ValidatedInt(8192);
    public ValidatedInt netheriteSawUses = new ValidatedInt(32768);
    public ValidatedBoolean fullBlockCrafting = new ValidatedBoolean(true);
    public ValidatedBoolean requireBagSpace = new ValidatedBoolean(true);
    public ValidatedBoolean voidExcessBits = new ValidatedBoolean(true);
    public ValidatedInt creativeClipboardSize = new ValidatedInt(10);
    public ValidatedList<String> revertibleBlocks = new ValidatedString("*").toList(List.of("*"));
    public ValidatedBoolean lowMemoryMode = new ValidatedBoolean(false);

    public ServerConfiguration() {
        super(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "server"));
    }

    @Override
    public SaveType saveType() {
        return SaveType.SEPARATE;
    }

    @Override
    public void onSyncClient() {
        ChiselsAndBits.onClientConfigurationChanged();
    }

    @Override
    public void onUpdateClient() {
        ChiselsAndBits.onClientConfigurationChanged();
    }

    @Override
    public void onUpdateServer(final ServerUpdateContext context) {
        ChiselsAndBits.onServerConfigurationChanged();
    }

    public boolean canRevertToBlock(final BlockState newState) {
        final List<? extends String> blockNames = revertibleBlocks.get();
        return blockNames.contains("*")
                || blockNames.contains(Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(newState.getBlock()))
                        .toString());
    }
}
