package mod.chiselsandbits.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class ApiServerGameTests {

    @GameTest(maxTicks = 100)
    public void mutatesAndRestoresWorldThroughApi(final GameTestHelper helper) {
        ApiServerGameTestsBodies.mutatesAndRestoresWorldThroughApi(helper);
    }

    @GameTest(maxTicks = 100)
    public void givesBitsToServerPlayer(final GameTestHelper helper) {
        ApiServerGameTestsBodies.givesBitsToServerPlayer(helper);
    }

    @GameTest(maxTicks = 100)
    public void validatesPublicApiAndConfigurationContracts(final GameTestHelper helper) {
        ApiServerGameTestsBodies.validatesPublicApiAndConfigurationContracts(helper);
    }
}
