package mod.chiselsandbits.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class ApiContractGameTests {

    @GameTest(maxTicks = 100)
    public void wrapsOutOfRangeBitCoordinates(final GameTestHelper helper) {
        ApiContractGameTestsBodies.wrapsOutOfRangeBitCoordinates(helper);
    }

    @GameTest(maxTicks = 100)
    public void rejectsNullWorldTargets(final GameTestHelper helper) {
        ApiContractGameTestsBodies.rejectsNullWorldTargets(helper);
    }

    @GameTest(maxTicks = 100)
    public void airBlockAccessIsEmpty(final GameTestHelper helper) {
        ApiContractGameTestsBodies.airBlockAccessIsEmpty(helper);
    }

    @GameTest(maxTicks = 100)
    public void stateVariantProviderOverwritesAndMayReturnEmpty(final GameTestHelper helper) {
        ApiContractGameTestsBodies.stateVariantProviderOverwritesAndMayReturnEmpty(helper);
    }

    @GameTest(maxTicks = 100)
    public void nullArgumentsAreRejected(final GameTestHelper helper) {
        ApiContractGameTestsBodies.nullArgumentsAreRejected(helper);
    }

    @GameTest(maxTicks = 100)
    public void chiseledBlockReportsItselfAsChiseled(final GameTestHelper helper) {
        ApiContractGameTestsBodies.chiseledBlockReportsItselfAsChiseled(helper);
    }
}
