package mod.chiselsandbits.api;

import java.util.Collection;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Official Chisels &amp; Bits FCB hook ({@code IStateVariantProvider#getAllDefaultVariants}).
 * Mods with many color/shade variants register one of these per block so the Block Bits
 * tab can list every variant instead of only {@link BlockState#getBlock()}'s default state.
 */
public interface StateVariantProvider {

    Collection<BlockState> getAllDefaultVariants(BlockState state);
}
