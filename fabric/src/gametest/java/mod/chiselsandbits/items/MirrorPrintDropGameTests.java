package mod.chiselsandbits.items;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class MirrorPrintDropGameTests {

    @GameTest(maxTicks = 40)
    public void dropConfigurationToleratesMissingEntityAndPlayer(final GameTestHelper helper) {
        MirrorPrintDropGameTestsBodies.dropConfigurationToleratesMissingEntityAndPlayer(helper);
    }

    @GameTest(maxTicks = 40)
    public void unwrittenPrintHasNoPattern(final GameTestHelper helper) {
        MirrorPrintDropGameTestsBodies.unwrittenPrintHasNoPattern(helper);
    }
}
