package mod.chiselsandbits.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class LegacyCompatibilityGameTests {

    @GameTest(maxTicks = 200)
    public void upgradesLegacyWorldData(final GameTestHelper helper) {
        LegacyCompatibilityGameTestsBodies.upgradesLegacyWorldData(helper);
    }
}
