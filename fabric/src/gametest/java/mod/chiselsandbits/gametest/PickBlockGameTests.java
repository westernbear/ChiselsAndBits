package mod.chiselsandbits.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class PickBlockGameTests {

    @GameTest(maxTicks = 100)
    public void picksStoredBitUnderCursorAndRejectsEmptyBit(final GameTestHelper helper) {
        PickBlockGameTestsBodies.picksStoredBitUnderCursorAndRejectsEmptyBit(helper);
    }

    @GameTest(maxTicks = 100)
    public void returnsEmptyStackForFullyEmptiedBlock(final GameTestHelper helper) {
        PickBlockGameTestsBodies.returnsEmptyStackForFullyEmptiedBlock(helper);
    }

    @GameTest(maxTicks = 100)
    public void cloneStackConsumesRememberedHit(final GameTestHelper helper) {
        PickBlockGameTestsBodies.cloneStackConsumesRememberedHit(helper);
    }
}
