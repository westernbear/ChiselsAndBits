package mod.chiselsandbits.utils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.Palette;

public class PaletteUtils {

    private PaletteUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: PaletteUtils. This is a utility class");
    }

    public static List<BlockState> getOrderedListInPalette(final Palette<BlockState> stateIPalette) {
        final List<BlockState> data = new ArrayList<>(stateIPalette.getSize());
        for (int index = 0; index < stateIPalette.getSize(); index++) {
            data.add(stateIPalette.valueFor(index));
        }
        return data;
    }
}
