package mod.chiselsandbits.api;

import java.util.Collection;
import net.minecraft.world.level.block.Block;

public interface BlockProvider {

    Collection<Block> getBlocks();
}
