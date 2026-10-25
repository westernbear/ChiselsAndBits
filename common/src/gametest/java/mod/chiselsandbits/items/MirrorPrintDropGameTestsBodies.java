package mod.chiselsandbits.items;

import mod.chiselsandbits.registry.ModItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Regression coverage for the Mirror Print drop path. */
public final class MirrorPrintDropGameTestsBodies {

    public static void dropConfigurationToleratesMissingEntityAndPlayer(final GameTestHelper helper) {
        final ItemEntity entity =
                new ItemEntity(helper.getLevel(), 1.5, 2.0, 1.5, new ItemStack(ModItems.ITEM_MIRROR_PRINT.get()));

        ItemMirrorPrint.configureDroppedItem(entity, null);
        helper.assertFalse(entity.hasPickUpDelay(), "drop is immediately pickup-able");

        // Entities can be dropped without a player (dispensers, command-generated drops).
        ItemMirrorPrint.configureDroppedItem(null, null);
        helper.succeed();
    }

    public static void unwrittenPrintHasNoPattern(final GameTestHelper helper) {
        final ItemStack print = new ItemStack(ModItems.ITEM_MIRROR_PRINT.get());
        final ItemMirrorPrint item = ModItems.ITEM_MIRROR_PRINT.get();

        helper.assertFalse(item.isWritten(print), "fresh print is unwritten");
        helper.assertTrue(item.getPatternedItem(print, false) == null, "unwritten print yields no pattern");

        helper.succeed();
    }
}
