package mod.chiselsandbits.items;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

public final class MirrorPrintDropCheck {
    private MirrorPrintDropCheck() {}

    public static void main(final String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ItemMirrorPrint.configureDroppedItem(null, null);
    }
}
