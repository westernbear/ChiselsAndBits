package mod.chiselsandbits.gametest;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.api.SaveType;
import mod.chiselsandbits.api.APIExceptions.CannotBeChiseled;
import mod.chiselsandbits.api.APIExceptions.InvalidBitItem;
import mod.chiselsandbits.api.APIExceptions.SpaceOccupied;
import mod.chiselsandbits.api.BitQueryResults;
import mod.chiselsandbits.api.BlockProvider;
import mod.chiselsandbits.api.IBitAccess;
import mod.chiselsandbits.api.IChiselAndBitsAPI;
import mod.chiselsandbits.api.ParameterType.BooleanParam;
import mod.chiselsandbits.api.ParameterType.DoubleParam;
import mod.chiselsandbits.api.ParameterType.FloatParam;
import mod.chiselsandbits.api.ParameterType.IntegerParam;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.core.api.ChiselAndBitsAPI;
import mod.chiselsandbits.registry.ModBlocks;
import mod.chiselsandbits.registry.ModItems;
import mod.chiselsandbits.utils.Constants;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

public class ApiServerGameTests {

    private static final BlockPos TARGET = new BlockPos(1, 1, 1);
    private static final BlockPos BIT_MIN = BlockPos.ZERO;
    private static final BlockPos BIT_MAX = new BlockPos(15, 15, 15);

    @GameTest(maxTicks = 100)
    public void mutatesAndRestoresWorldThroughApi(final GameTestHelper helper) {
        final IChiselAndBitsAPI api = IChiselAndBitsAPI.getInstance();
        helper.setBlock(TARGET, Blocks.STONE);
        final BlockPos absoluteTarget = helper.absolutePos(TARGET);

        try {
            final IBitAccess access = api.getBitAccess(helper.getLevel(), absoluteTarget);
            final BitQueryResults initial = access.queryBitRange(BIT_MIN, BIT_MAX);
            helper.assertValueEqual(initial.solid, 4096, "initial solid bit count");
            helper.assertValueEqual(initial.empty, 0, "initial empty bit count");

            access.setBitAt(0, 0, 0, null);
            access.commitChanges(true);

            helper.assertBlockPresent(ModBlocks.CHISELED_BLOCK.get(), TARGET);
            helper.assertTrue(api.isBlockChiseled(helper.getLevel(), absoluteTarget), "block was not chiseled");
            final IBitAccess chiseled = api.getBitAccess(helper.getLevel(), absoluteTarget);
            final BitQueryResults changed = chiseled.queryBitRange(BIT_MIN, BIT_MAX);
            helper.assertValueEqual(changed.solid, 4095, "changed solid bit count");
            helper.assertValueEqual(changed.empty, 1, "changed empty bit count");
            helper.assertTrue(chiseled.getBitAt(0, 0, 0).isAir(), "removed bit was not air");

            chiseled.setBitAt(0, 0, 0, api.createBrushFromState(Blocks.STONE.defaultBlockState()));
            chiseled.commitChanges(true);
            helper.assertBlockPresent(Blocks.STONE, TARGET);
            helper.assertFalse(api.isBlockChiseled(helper.getLevel(), absoluteTarget), "full block was not restored");
        } catch (final CannotBeChiseled | InvalidBitItem | SpaceOccupied error) {
            throw new AssertionError(error);
        }

        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void givesBitsToServerPlayer(final GameTestHelper helper) {
        final IChiselAndBitsAPI api = IChiselAndBitsAPI.getInstance();
        final ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);

        try {
            final ItemStack bits = api.getBitItem(Blocks.STONE.defaultBlockState());
            bits.setCount(3);
            api.giveBitToPlayer(player, bits, null);
        } catch (final InvalidBitItem error) {
            throw new AssertionError(error);
        }

        helper.assertValueEqual(
                player.getInventory().countItem(ModItems.ITEM_BLOCK_BIT.get()), 3, "server player bit count");
        api.beginUndoGroup(player);
        api.beginUndoGroup(player);
        api.endUndoGroup(player);
        api.endUndoGroup(player);
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void validatesPublicApiAndConfigurationContracts(final GameTestHelper helper) {
        final IChiselAndBitsAPI liveApi = IChiselAndBitsAPI.getInstance();
        helper.assertTrue(liveApi == ChiselsAndBits.getApi(), "API singleton accessor returned another instance");

        final ChiselAndBitsAPI isolatedApi = new ChiselAndBitsAPI();
        final BlockProvider provider = () -> List.of(Blocks.STONE);
        isolatedApi.registerBlockProvider(provider);
        helper.assertTrue(isolatedApi.getStateProviders().contains(provider), "block provider was not registered");
        helper.assertValueEqual(
                isolatedApi.getAllDefaultVariants(Blocks.STONE.defaultBlockState()).size(),
                1,
                "BlockProvider stone variants");

        isolatedApi.registerStateVariantProvider(
                Blocks.OAK_LOG, state -> state.getBlock().getStateDefinition().getPossibleStates());
        helper.assertValueEqual(
                isolatedApi.getAllDefaultVariants(Blocks.OAK_LOG.defaultBlockState()).size(),
                3,
                "oak log state-variant count");

        final AtomicBoolean itemStackHandlerCalled = new AtomicBoolean();
        isolatedApi.registerItemStackHandler(Blocks.STONE, (state, stack) -> itemStackHandlerCalled.set(true));
        final ItemStack representative = isolatedApi.getItemStackForState(Blocks.STONE.defaultBlockState());
        helper.assertFalse(representative.isEmpty(), "stone representative stack was empty");
        helper.assertTrue(itemStackHandlerCalled.get(), "registered item stack handler was not called");
        helper.assertTrue(isolatedApi.getItemStackForState(null).isEmpty(), "null state stack was not empty");
        helper.assertTrue(isolatedApi.getItemType(null) == null, "null item stack had an item type");

        try {
            isolatedApi.getBitItem(null);
            throw new AssertionError("null block state did not throw InvalidBitItem");
        } catch (final InvalidBitItem expected) {
            // Expected public contract.
        }

        helper.assertTrue(
                liveApi.getParameter(BooleanParam.ENABLE_DAMAGE_TOOLS) instanceof Boolean, "boolean parameter type");
        helper.assertTrue(
                liveApi.getParameter(FloatParam.BLOCK_FULL_LIGHT_PERCENTAGE) instanceof Float, "float parameter type");
        helper.assertTrue(
                liveApi.getParameter(DoubleParam.BIT_MAX_DRAWN_REGION_SIZE) instanceof Double, "double parameter type");
        helper.assertTrue(
                liveApi.getParameter(IntegerParam.BIT_BAG_MAX_STACK_SIZE) instanceof Integer, "integer parameter type");
        helper.assertTrue(
                ConfigApiJava.isConfigLoaded(
                        ChiselsAndBits.getConfig().getCommon().getId().toLanguageKey() + ".", RegisterType.SERVER),
                "common config was not registered on the dedicated server");
        helper.assertFalse(
                ConfigApiJava.isConfigLoaded(
                        ChiselsAndBits.getConfig().getClient().getId().toLanguageKey() + ".", RegisterType.BOTH),
                "client config was registered on the dedicated server");
        ChiselsAndBits.onServerConfigurationChanged();
        helper.assertValueEqual(
                ChiselsAndBits.getConfig().getServer().saveType(), SaveType.SEPARATE, "server config save type");
        helper.assertValueEqual(
                ChiselsAndBits.getConfig().getServer().getId(),
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "server"),
                "server config id");
        helper.succeed();
    }
}
