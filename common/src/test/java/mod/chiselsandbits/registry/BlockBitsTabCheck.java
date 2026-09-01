package mod.chiselsandbits.registry;

import java.util.Collection;
import mod.chiselsandbits.core.api.ChiselAndBitsAPI;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockBitsTabCheck {
    private BlockBitsTabCheck() {}

    public static void main(final String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        final ChiselAndBitsAPI api = new ChiselAndBitsAPI();
        require(api.getAllDefaultVariants(Blocks.OAK_LOG.defaultBlockState()).isEmpty(), "oak log has no provider");

        api.registerStateVariantProvider(
                Blocks.OAK_LOG, state -> state.getBlock().getStateDefinition().getPossibleStates());

        final Collection<BlockState> variants = api.getAllDefaultVariants(Blocks.OAK_LOG.defaultBlockState());
        require(variants.size() == 3, "oak log axis variants should all be listed");
        require(
                variants.containsAll(Blocks.OAK_LOG.getStateDefinition().getPossibleStates()),
                "provider must return every default variant");

        api.registerBlockProvider(() -> java.util.List.of(Blocks.STONE));
        require(
                api.getAllDefaultVariants(Blocks.STONE.defaultBlockState()).size() == 1,
                "BlockProvider blocks expose their possible states as variants");
        require(
                api.getAllDefaultVariants(Blocks.DIRT.defaultBlockState()).isEmpty(),
                "unregistered blocks have no variants");
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
