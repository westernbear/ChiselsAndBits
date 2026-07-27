package mod.chiselsandbits.config;

import java.util.List;
import me.fzzyhmstrs.fzzy_config.annotations.NonSync;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.event.api.ServerUpdateContext;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedLong;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.helpers.LocalStrings;
import mod.chiselsandbits.utils.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class CommonConfiguration extends Config {

    @NonSync
    public ValidatedBoolean enableHelp = new ValidatedBoolean(true);

    @NonSync
    public ValidatedLong collisionBoxCacheSize = new ValidatedLong(10000L, Long.MAX_VALUE, 0L);

    public CommonConfiguration() {
        super(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "common"));
    }

    @Override
    public void onUpdateClient() {
        ChiselsAndBits.onClientConfigurationChanged();
    }

    @Override
    public void onUpdateServer(final ServerUpdateContext context) {
        ChiselsAndBits.onServerConfigurationChanged();
    }

    public void helpText(final LocalStrings string, final List<Component> tooltip, final String... variables) {
        if (enableHelp.get()) {
            int varOffset = 0;

            final String[] lines = string.getLocal().split(";");
            for (String a : lines) {
                while (a.contains("{}") && variables.length > varOffset) {
                    final int offset = a.indexOf("{}");
                    if (offset >= 0) {
                        final String pre = a.substring(0, offset);
                        final String post = a.substring(offset + 2);
                        a = String.format("%s%s%s", pre, variables[varOffset++], post);
                    }
                }

                tooltip.add(Component.literal(a));
            }
        }
    }
}
