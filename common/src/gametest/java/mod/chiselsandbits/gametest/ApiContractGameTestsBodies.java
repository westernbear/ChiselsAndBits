package mod.chiselsandbits.gametest;

import java.util.List;
import mod.chiselsandbits.api.APIExceptions.CannotBeChiseled;
import mod.chiselsandbits.api.APIExceptions.SpaceOccupied;
import mod.chiselsandbits.api.BitQueryResults;
import mod.chiselsandbits.api.IBitAccess;
import mod.chiselsandbits.api.IChiselAndBitsAPI;
import mod.chiselsandbits.core.api.ChiselAndBitsAPI;
import mod.chiselsandbits.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Edge cases of the public API contract. */
public final class ApiContractGameTestsBodies {

    private static final BlockPos TARGET = new BlockPos(1, 1, 1);
    private static final BlockPos EMPTY = new BlockPos(3, 1, 1);
    private static final BlockPos BIT_MIN = BlockPos.ZERO;
    private static final BlockPos BIT_MAX = new BlockPos(15, 15, 15);

    public static void wrapsOutOfRangeBitCoordinates(final GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.STONE);
        final BlockPos absolute = helper.absolutePos(TARGET);
        final IChiselAndBitsAPI api = IChiselAndBitsAPI.getInstance();

        final IBitAccess reloaded;
        try {
            final IBitAccess access = api.getBitAccess(helper.getLevel(), absolute);
            access.setBitAt(16, 0, 0, null);
            access.commitChanges(true);
            reloaded = api.getBitAccess(helper.getLevel(), absolute);
        } catch (final CannotBeChiseled | SpaceOccupied error) {
            throw new AssertionError(error);
        }

        helper.assertTrue(reloaded.getBitAt(0, 0, 0).isAir(), "x=16 wrapped to x=0");
        helper.assertValueEqual(reloaded.queryBitRange(BIT_MIN, BIT_MAX).empty, 1, "exactly one bit cleared");

        helper.succeed();
    }

    public static void rejectsNullWorldTargets(final GameTestHelper helper) {
        final IChiselAndBitsAPI api = IChiselAndBitsAPI.getInstance();
        try {
            api.getBitAccess(helper.getLevel(), null);
            throw new AssertionError("null position accepted");
        } catch (final CannotBeChiseled expected) {
            // Documented contract.
        }

        try {
            api.getBitAccess(null, helper.absolutePos(TARGET));
            throw new AssertionError("null level accepted");
        } catch (final CannotBeChiseled expected) {
            // Documented contract.
        }

        helper.succeed();
    }

    public static void airBlockAccessIsEmpty(final GameTestHelper helper) {
        final BitQueryResults results;
        try {
            final IBitAccess access =
                    IChiselAndBitsAPI.getInstance().getBitAccess(helper.getLevel(), helper.absolutePos(EMPTY));
            results = access.queryBitRange(BIT_MIN, BIT_MAX);
        } catch (final CannotBeChiseled error) {
            throw new AssertionError(error);
        }
        helper.assertValueEqual(results.solid, 0, "air has no solid bits");
        helper.assertValueEqual(results.empty, 4096, "air is fully empty");

        helper.succeed();
    }

    public static void stateVariantProviderOverwritesAndMayReturnEmpty(final GameTestHelper helper) {
        final ChiselAndBitsAPI api = new ChiselAndBitsAPI();

        api.registerStateVariantProvider(Blocks.OAK_LOG, state -> List.of());
        helper.assertTrue(
                api.getAllDefaultVariants(Blocks.OAK_LOG.defaultBlockState()).isEmpty(),
                "empty provider result is honoured");

        api.registerStateVariantProvider(Blocks.OAK_LOG, state -> List.of(state));
        helper.assertValueEqual(
                api.getAllDefaultVariants(Blocks.OAK_LOG.defaultBlockState()).size(),
                1,
                "second provider replaces the first");

        helper.assertTrue(
                api.getAllDefaultVariants(Blocks.STONE.defaultBlockState()).isEmpty(),
                "unregistered block has no variants");

        helper.succeed();
    }

    public static void nullArgumentsAreRejected(final GameTestHelper helper) {
        final IChiselAndBitsAPI api = IChiselAndBitsAPI.getInstance();
        final ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);

        expectNullPointer(() -> api.registerBlockProvider(null), "null block provider");
        expectNullPointer(() -> api.registerStateVariantProvider(null, state -> List.of()), "null block");
        expectNullPointer(() -> api.registerStateVariantProvider(Blocks.STONE, null), "null variant provider");
        expectNullPointer(() -> api.beginUndoGroup(null), "null undo group player");

        api.beginUndoGroup(player);
        api.endUndoGroup(player);
        helper.succeed();
    }

    public static void chiseledBlockReportsItselfAsChiseled(final GameTestHelper helper) {
        helper.setBlock(TARGET, ModBlocks.CHISELED_BLOCK.get());
        final BlockPos absolute = helper.absolutePos(TARGET);
        final IChiselAndBitsAPI api = IChiselAndBitsAPI.getInstance();

        helper.assertTrue(api.isBlockChiseled(helper.getLevel(), absolute), "chiseled block detected");
        helper.assertTrue(
                !api.isBlockChiseled(helper.getLevel(), helper.absolutePos(EMPTY)), "empty position is not chiseled");

        helper.succeed();
    }

    private static void expectNullPointer(final Runnable action, final String label) {
        try {
            action.run();
            throw new AssertionError(label + " was accepted");
        } catch (final NullPointerException expected) {
            // Documented contract.
        }
    }
}
