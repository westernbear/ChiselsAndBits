package mod.chiselsandbits.gametest;

import java.util.List;
import java.util.function.Consumer;
import mod.chiselsandbits.core.ChiselsAndBits;
import mod.chiselsandbits.items.MirrorPrintDropGameTestsBodies;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers the shared gametest bodies with NeoForge's runtime game-test API.
 *
 * <p>26.2 replaced annotation discovery with two registries: the test function must be registered in
 * {@code minecraft:test_function} and each instance in {@code minecraft:test_instance}.
 */
@EventBusSubscriber(modid = ChiselsAndBits.MODID)
public final class NeoForgeGameTestRegistrar {

    private static final String STRUCTURE = "empty_5";

    private static final List<TestEntry> TESTS = List.of(
            new TestEntry(
                    "api_contract_wrapsoutofrangebitcoordinates",
                    100,
                    ApiContractGameTestsBodies::wrapsOutOfRangeBitCoordinates),
            new TestEntry(
                    "api_contract_rejectsnullworldtargets", 100, ApiContractGameTestsBodies::rejectsNullWorldTargets),
            new TestEntry("api_contract_airblockaccessisempty", 100, ApiContractGameTestsBodies::airBlockAccessIsEmpty),
            new TestEntry(
                    "api_contract_statevariantprovideroverwritesandmayreturnempty",
                    100,
                    ApiContractGameTestsBodies::stateVariantProviderOverwritesAndMayReturnEmpty),
            new TestEntry(
                    "api_contract_nullargumentsarerejected", 100, ApiContractGameTestsBodies::nullArgumentsAreRejected),
            new TestEntry(
                    "api_contract_chiseledblockreportsitselfaschiseled",
                    100,
                    ApiContractGameTestsBodies::chiseledBlockReportsItselfAsChiseled),
            new TestEntry(
                    "api_server_mutatesandrestoresworldthroughapi",
                    100,
                    ApiServerGameTestsBodies::mutatesAndRestoresWorldThroughApi),
            new TestEntry("api_server_givesbitstoserverplayer", 100, ApiServerGameTestsBodies::givesBitsToServerPlayer),
            new TestEntry(
                    "api_server_validatespublicapiandconfigurationcontracts",
                    100,
                    ApiServerGameTestsBodies::validatesPublicApiAndConfigurationContracts),
            new TestEntry(
                    "bit_storage_clampsbitstocapacityandrejectsoverflow",
                    100,
                    BitStorageGameTestsBodies::clampsBitsToCapacityAndRejectsOverflow),
            new TestEntry(
                    "bit_storage_convertsfullblockintostorageentry",
                    100,
                    BitStorageGameTestsBodies::convertsFullBlockIntoStorageEntry),
            new TestEntry(
                    "bit_storage_rejectsmixedbitmaterials", 100, BitStorageGameTestsBodies::rejectsMixedBitMaterials),
            new TestEntry(
                    "bit_storage_clampsfluidandclearsonnonpositiveamount",
                    100,
                    BitStorageGameTestsBodies::clampsFluidAndClearsOnNonPositiveAmount),
            new TestEntry(
                    "bit_storage_itemfluidcomponentclampsandclears",
                    100,
                    BitStorageGameTestsBodies::itemFluidComponentClampsAndClears),
            new TestEntry(
                    "bit_storage_dropletconversionislosslessatboundaries",
                    100,
                    BitStorageGameTestsBodies::dropletConversionIsLosslessAtBoundaries),
            new TestEntry(
                    "legacy_compat_upgradeslegacyworlddata",
                    200,
                    LegacyCompatibilityGameTestsBodies::upgradesLegacyWorldData),
            new TestEntry(
                    "pick_block_picksstoredbitundercursorandrejectsemptybit",
                    100,
                    PickBlockGameTestsBodies::picksStoredBitUnderCursorAndRejectsEmptyBit),
            new TestEntry(
                    "pick_block_returnsemptystackforfullyemptiedblock",
                    100,
                    PickBlockGameTestsBodies::returnsEmptyStackForFullyEmptiedBlock),
            new TestEntry(
                    "pick_block_clonestackconsumesrememberedhit",
                    100,
                    PickBlockGameTestsBodies::cloneStackConsumesRememberedHit),
            new TestEntry(
                    "mirror_print_dropconfigurationtoleratesmissingentityandplayer",
                    40,
                    MirrorPrintDropGameTestsBodies::dropConfigurationToleratesMissingEntityAndPlayer),
            new TestEntry(
                    "mirror_print_unwrittenprinthasnopattern",
                    40,
                    MirrorPrintDropGameTestsBodies::unwrittenPrintHasNoPattern),
            new TestEntry(
                    "bit_storage_rejects_fluid_while_solid_bits",
                    100,
                    NeoForgeBitStorageBodies::rejectsFluidWhileSolidBitsAreStored));

    private NeoForgeGameTestRegistrar() {}

    @SubscribeEvent
    public static void registerFunctions(final RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.TEST_FUNCTION)) {
            return;
        }
        for (final TestEntry test : TESTS) {
            event.register(Registries.TEST_FUNCTION, functionId(test.name()), () -> test.body());
        }
    }

    @SubscribeEvent
    public static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(ChiselsAndBits.MODID, "default"),
                new TestEnvironmentDefinition.AllOf());

        for (final TestEntry test : TESTS) {
            final TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                    environment,
                    Identifier.fromNamespaceAndPath(ChiselsAndBits.MODID, STRUCTURE),
                    test.maxTicks(),
                    1,
                    true);
            event.registerTest(
                    Identifier.fromNamespaceAndPath(ChiselsAndBits.MODID, test.name()),
                    new FunctionGameTestInstance(
                            ResourceKey.create(Registries.TEST_FUNCTION, functionId(test.name())), data));
        }
    }

    private static Identifier functionId(final String name) {
        return Identifier.fromNamespaceAndPath(ChiselsAndBits.MODID, name);
    }

    private record TestEntry(String name, int maxTicks, Consumer<GameTestHelper> body) {}
}
